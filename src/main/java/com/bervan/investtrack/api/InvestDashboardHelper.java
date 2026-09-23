package com.bervan.investtrack.api;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.BudgetChartDataService;
import com.bervan.investtrack.service.CurrencyConverter;
import com.bervan.investtrack.service.ETFDataService;
import com.bervan.investtrack.service.InvestmentCalculationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class InvestDashboardHelper {

    private static final DateTimeFormatter BENCHMARK_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ETFDataService etfDataService;
    private final InvestmentCalculationService calculationService;
    private final CurrencyConverter currencyConverter;
    private final BudgetChartDataService budgetChartDataService;

    public InvestDashboardHelper(
            ETFDataService etfDataService,
            InvestmentCalculationService calculationService,
            CurrencyConverter currencyConverter,
            BudgetChartDataService budgetChartDataService) {
        this.etfDataService = etfDataService;
        this.calculationService = calculationService;
        this.currencyConverter = currencyConverter;
        this.budgetChartDataService = budgetChartDataService;
    }

    public Map<String, Object> getDashboard(List<Wallet> allWallets) {
        List<Wallet> investWallets = allWallets.stream().filter(Wallet::isInvestmentLike).toList();
        List<Wallet> ppkWallets = allWallets.stream().filter(Wallet::isPPK).toList();
        List<Wallet> investmentFundsWallets = allWallets.stream().filter(Wallet::isInvestmentFund).toList();
        List<Wallet> savingsWallets = allWallets.stream().filter(w -> !w.isInvestmentLike()).toList();

        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> investTs =
                calculationService.buildAggregatedTimeSeries(investWallets, this::toPln);
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> ppkTs =
                calculationService.buildAggregatedTimeSeries(ppkWallets, this::toPln);
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> investFundTs =
                calculationService.buildAggregatedTimeSeries(investmentFundsWallets, this::toPln);
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> allTs =
                calculationService.buildAggregatedTimeSeries(allWallets, this::toPln);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpi", buildKpis(investWallets, savingsWallets, investTs));
        result.put("investTimeSeries", buildTimeSeriesWithBenchmarks(investTs));
        result.put("netWorthTimeSeries", buildTimeSeriesWithBenchmarks(allTs));
        result.put("ppkTimeSeries", buildTimeSeriesWithBenchmarks(ppkTs));
        result.put("investFundTimeSeries", buildTimeSeriesWithBenchmarks(investFundTs));
        result.put("allocation", buildAssetAllocation(allWallets));
        result.put("heatmap", buildHeatmap(investTs));
        result.put("budget", buildBudgetSeries());
        result.put("walletSeries", buildWalletSeriesList(allWallets));

        return result;
    }

    // ── KPIs ──────────────────────────────────────────────────────────────────

    private Map<String, Object> buildKpis(
            List<Wallet> investWallets,
            List<Wallet> savingsWallets,
            Map<LocalDate, InvestmentCalculationService.PortfolioPoint> investTs) {

        // Investment KPIs
        BigDecimal investBalance = sumBalance(investWallets);
        BigDecimal investNetDeposits = sumNetDeposits(investWallets);
        BigDecimal investReturn = investBalance.subtract(investNetDeposits);
        BigDecimal investReturnPct = investNetDeposits.compareTo(BigDecimal.ZERO) > 0
                ? pct(investReturn.divide(investNetDeposits, 4, RoundingMode.HALF_UP))
                : BigDecimal.ZERO;

        BigDecimal investTwr = pct(calculationService.calculateAggregatedTWR(investTs));

        double investYears = monthsSpan(investWallets) / 12.0;
        BigDecimal investCagr = BigDecimal.ZERO;
        if (investYears > 0.1 && investNetDeposits.compareTo(BigDecimal.ZERO) > 0) {
            investCagr = pct(calculationService.calculateCAGR(investNetDeposits, investBalance, Math.max(investYears, 0.1)));
        }

        // Savings KPIs
        BigDecimal savingsBalance = sumBalance(savingsWallets);
        BigDecimal savingsNetDeposits = sumNetDeposits(savingsWallets);
        BigDecimal savingsGrowth = savingsBalance.subtract(savingsNetDeposits);
        BigDecimal netWorth = investBalance.add(savingsBalance);

        BigDecimal avgMonthlyDeposit = investYears > 0
                ? investNetDeposits.divide(BigDecimal.valueOf(investYears * 12), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("investBalance", round(investBalance));
        kpi.put("investNetDeposits", round(investNetDeposits));
        kpi.put("investReturn", round(investReturn));
        kpi.put("investReturnPct", round(investReturnPct));
        kpi.put("investTwr", round(investTwr));
        kpi.put("investCagr", round(investCagr));
        kpi.put("savingsBalance", round(savingsBalance));
        kpi.put("savingsGrowth", round(savingsGrowth));
        kpi.put("netWorth", round(netWorth));
        kpi.put("avgMonthlyDeposit", round(avgMonthlyDeposit));
        kpi.put("investMonthsSpan", (int) Math.round(investYears * 12));
        return kpi;
    }

    private BigDecimal sumBalance(List<Wallet> wallets) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Wallet w : wallets) {
            sum = sum.add(toPln(w.getCurrentValue(), w.getCurrency()));
        }
        return sum;
    }

    private BigDecimal sumNetDeposits(List<Wallet> wallets) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Wallet w : wallets) {
            sum = sum.add(toPln(w.getTotalDeposits(), w.getCurrency()));
        }
        return sum;
    }

    // ── Time Series & Benchmarks ──────────────────────────────────────────────

    private List<Map<String, Object>> buildTimeSeriesWithBenchmarks(
            Map<LocalDate, InvestmentCalculationService.PortfolioPoint> ts) {

        List<String> datesDdMmYyyy = new ArrayList<>();
        List<BigDecimal> netDeposits = new ArrayList<>();
        for (Map.Entry<LocalDate, InvestmentCalculationService.PortfolioPoint> e : ts.entrySet()) {
            datesDdMmYyyy.add(e.getKey().format(BENCHMARK_DATE_FORMATTER));
            netDeposits.add(e.getValue().cashFlow());
        }

        BenchmarkValues benchmarks = fetchBenchmarks(datesDdMmYyyy, netDeposits, "PLN");

        List<Map<String, Object>> list = new ArrayList<>();
        BigDecimal cum = BigDecimal.ZERO;
        int i = 0;
        for (Map.Entry<LocalDate, InvestmentCalculationService.PortfolioPoint> e : ts.entrySet()) {
            cum = cum.add(e.getValue().cashFlow());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", e.getKey().toString());
            point.put("balance", round(e.getValue().balance()));
            point.put("cumDeposit", round(cum));
            point.put("sp500", benchmarks.getBenchmarkValue(benchmarks.sp500(), i));
            point.put("wig20", benchmarks.getBenchmarkValue(benchmarks.wig20(), i));
            point.put("nasdaq", benchmarks.getBenchmarkValue(benchmarks.nasdaq(), i));
            point.put("dji", benchmarks.getBenchmarkValue(benchmarks.dji(), i));
            point.put("fixedDeposit3_5", benchmarks.getBenchmarkValue(benchmarks.fixedDeposit3_5(), i));
            list.add(point);
            i++;
        }
        return list;
    }

    private BenchmarkValues fetchBenchmarks(List<String> datesDdMmYyyy, List<BigDecimal> netDeposits, String targetCurrency) {
        List<BigDecimal> sp500 = etfDataService.calculateBenchmarkValuesForTicker(
                ETFDataService.SP500_TICKER, "USD", datesDdMmYyyy, netDeposits, targetCurrency);
        List<BigDecimal> wig20 = etfDataService.calculateBenchmarkValuesForTicker(
                ETFDataService.WIG20_TICKER, "PLN", datesDdMmYyyy, netDeposits, targetCurrency);
        List<BigDecimal> nasdaq = etfDataService.calculateBenchmarkValuesForTicker(
                ETFDataService.NASDAQ_TICKER, "USD", datesDdMmYyyy, netDeposits, targetCurrency);
        List<BigDecimal> dji = etfDataService.calculateBenchmarkValuesForTicker(
                ETFDataService.DJI_TICKER, "USD", datesDdMmYyyy, netDeposits, targetCurrency);
        List<BigDecimal> fixedDeposit3_5 = etfDataService.calculateBenchmarkValuesForTicker(
                ETFDataService.FIXED_DEPOSIT_TICKER_3_5, "PLN", datesDdMmYyyy, netDeposits, targetCurrency);

        return new BenchmarkValues(sp500, wig20, nasdaq, dji, fixedDeposit3_5);
    }

    private List<Map<String, Object>> buildAssetAllocation(List<Wallet> allWallets) {
        List<Map<String, Object>> allocation = new ArrayList<>();
        for (Wallet w : allWallets) {
            BigDecimal valuePln = toPln(w.getCurrentValue(), w.getCurrency());
            if (valuePln.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", w.getName());
                entry.put("type", w.getWalletType());
                entry.put("valuePln", round(valuePln));
                allocation.add(entry);
            }
        }
        return allocation;
    }

    // ── Asset Allocation ──────────────────────────────────────────────────────

    private Map<String, BigDecimal> buildHeatmap(
            Map<LocalDate, InvestmentCalculationService.PortfolioPoint> ts) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        List<LocalDate> dates = new ArrayList<>(ts.keySet());
        for (int i = 1; i < dates.size(); i++) {
            LocalDate prev = dates.get(i - 1);
            LocalDate curr = dates.get(i);
            InvestmentCalculationService.PortfolioPoint prevPt = ts.get(prev);
            InvestmentCalculationService.PortfolioPoint currPt = ts.get(curr);
            BigDecimal beginValue = prevPt.balance().add(currPt.cashFlow());
            if (beginValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ret = currPt.balance().subtract(beginValue)
                        .divide(beginValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                String key = String.format("%d-%02d", curr.getYear(), curr.getMonthValue());
                result.put(key, round(ret));
            }
        }
        return result;
    }

    // ── Monthly Heatmap ───────────────────────────────────────────────────────

    private List<Map<String, Object>> buildBudgetSeries() {
        LocalDate budgetFrom = LocalDate.now().minusMonths(12).withDayOfMonth(1);
        LocalDate budgetTo = LocalDate.now();
        BudgetChartDataService.MonthlyBudgetData monthly =
                budgetChartDataService.getMonthlyIncomeExpense(budgetFrom, budgetTo);

        List<Map<String, Object>> budgetSeries = new ArrayList<>();
        for (String month : monthly.income().keySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", month);
            point.put("income", monthly.income().getOrDefault(month, BigDecimal.ZERO));
            point.put("expense", monthly.expense().getOrDefault(month, BigDecimal.ZERO));
            budgetSeries.add(point);
        }
        return budgetSeries;
    }

    // ── Budget Data ───────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildWalletSeriesList(List<Wallet> allWallets) {
        List<Map<String, Object>> walletSeriesList = new ArrayList<>();
        for (Wallet w : allWallets) {
            walletSeriesList.add(buildSingleWalletEntry(w));
        }
        return walletSeriesList;
    }

    // ── Per-Wallet Series ─────────────────────────────────────────────────────

    private Map<String, Object> buildSingleWalletEntry(Wallet w) {
        List<WalletSnapshot> snaps = w.getSnapshots().stream()
                .filter(s -> s.getSnapshotDate() != null)
                .sorted(Comparator.comparing(WalletSnapshot::getSnapshotDate))
                .toList();

        List<String> datesDdMmYyyy = new ArrayList<>();
        List<BigDecimal> netDeposits = new ArrayList<>();
        for (WalletSnapshot snap : snaps) {
            datesDdMmYyyy.add(snap.getSnapshotDate().format(BENCHMARK_DATE_FORMATTER));
            BigDecimal dep = snap.getMonthlyDeposit() != null ? snap.getMonthlyDeposit() : BigDecimal.ZERO;
            BigDecimal wdr = snap.getMonthlyWithdrawal() != null ? snap.getMonthlyWithdrawal() : BigDecimal.ZERO;
            netDeposits.add(dep.subtract(wdr));
        }

        BenchmarkValues benchmarks = fetchBenchmarks(datesDdMmYyyy, netDeposits, w.getCurrency());

        List<Map<String, Object>> series = new ArrayList<>();
        BigDecimal cum = BigDecimal.ZERO;
        int idx = 0;
        for (WalletSnapshot snap : snaps) {
            BigDecimal dep = snap.getMonthlyDeposit() != null ? snap.getMonthlyDeposit() : BigDecimal.ZERO;
            BigDecimal wdr = snap.getMonthlyWithdrawal() != null ? snap.getMonthlyWithdrawal() : BigDecimal.ZERO;
            cum = cum.add(toPln(dep.subtract(wdr), w.getCurrency()));

            BigDecimal pv = snap.getPortfolioValue() != null ? snap.getPortfolioValue() : BigDecimal.ZERO;
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("date", snap.getSnapshotDate().toString());
            pt.put("balance", round(toPln(pv, w.getCurrency())));
            pt.put("cumDeposit", round(cum));
            pt.put("sp500", benchmarks.getBenchmarkValueInPln(benchmarks.sp500(), idx, w.getCurrency(), currencyConverter));
            pt.put("wig20", benchmarks.getBenchmarkValueInPln(benchmarks.wig20(), idx, w.getCurrency(), currencyConverter));
            pt.put("nasdaq", benchmarks.getBenchmarkValueInPln(benchmarks.nasdaq(), idx, w.getCurrency(), currencyConverter));
            pt.put("dji", benchmarks.getBenchmarkValueInPln(benchmarks.dji(), idx, w.getCurrency(), currencyConverter));
            pt.put("fixedDeposit3_5", benchmarks.getBenchmarkValueInPln(benchmarks.fixedDeposit3_5(), idx, w.getCurrency(), currencyConverter));

            series.add(pt);
            idx++;
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("walletId", w.getId().toString());
        entry.put("walletName", w.getName());
        entry.put("isInvestment", w.isInvestmentLike());
        entry.put("returnRate", w.getReturnRate() != null ? round(w.getReturnRate()) : BigDecimal.ZERO);
        entry.put("series", series);
        return entry;
    }

    private BigDecimal pct(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Helper Math & Conversion ──────────────────────────────────────────────

    private BigDecimal toPln(BigDecimal amount, String currency) {
        if (amount == null) return BigDecimal.ZERO;
        return currencyConverter.convert(amount, CurrencyConverter.Currency.of(currency), CurrencyConverter.Currency.PLN);
    }

    private BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private double monthsSpan(List<Wallet> wallets) {
        Optional<LocalDate> min = wallets.stream()
                .flatMap(w -> w.getSnapshots().stream())
                .map(WalletSnapshot::getSnapshotDate).min(Comparator.naturalOrder());
        Optional<LocalDate> max = wallets.stream()
                .flatMap(w -> w.getSnapshots().stream())
                .map(WalletSnapshot::getSnapshotDate).max(Comparator.naturalOrder());
        if (min.isEmpty() || max.isEmpty()) return 1;
        return ChronoUnit.MONTHS.between(min.get(), max.get()) + 1;
    }

    private record BenchmarkValues(
            List<BigDecimal> sp500,
            List<BigDecimal> wig20,
            List<BigDecimal> nasdaq,
            List<BigDecimal> dji,
            List<BigDecimal> fixedDeposit3_5) {

        BigDecimal getBenchmarkValue(List<BigDecimal> values, int index) {
            return (index < values.size()) ? values.get(index).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }

        BigDecimal getBenchmarkValueInPln(List<BigDecimal> values, int index, String currency, CurrencyConverter converter) {
            if (index < values.size()) {
                BigDecimal val = values.get(index);
                if (val == null) return BigDecimal.ZERO;
                return converter.convert(val, CurrencyConverter.Currency.of(currency), CurrencyConverter.Currency.PLN)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        }
    }
}
