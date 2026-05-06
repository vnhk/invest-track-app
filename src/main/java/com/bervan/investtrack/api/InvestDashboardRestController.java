package com.bervan.investtrack.api;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.*;
import com.bervan.investtrack.service.CurrencyConverter.Currency;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/invest-track/dashboard")
public class InvestDashboardRestController {

    private final WalletService walletService;
    private final WalletSnapshotService snapshotService;
    private final InvestmentCalculationService calculationService;
    private final BudgetChartDataService budgetChartDataService;
    private final CurrencyConverter currencyConverter;

    public InvestDashboardRestController(WalletService walletService, WalletSnapshotService snapshotService,
                                          InvestmentCalculationService calculationService,
                                          BudgetChartDataService budgetChartDataService,
                                          CurrencyConverter currencyConverter) {
        this.walletService = walletService;
        this.snapshotService = snapshotService;
        this.calculationService = calculationService;
        this.budgetChartDataService = budgetChartDataService;
        this.currencyConverter = currencyConverter;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard() {

        // Load all wallets with snapshots
        List<Wallet> allWallets = new ArrayList<>(walletService.load(PageRequest.of(0, Integer.MAX_VALUE)));
        for (Wallet w : allWallets) {
            List<WalletSnapshot> snapshots = snapshotService.findByWalletId(w.getId());
            w.getSnapshots().clear();
            w.getSnapshots().addAll(snapshots);
        }

        List<Wallet> investWallets = allWallets.stream().filter(Wallet::isInvestmentLike).toList();
        List<Wallet> savingsWallets = allWallets.stream().filter(w -> !w.isInvestmentLike()).toList();

        // ── Investment KPIs ──────────────────────────────────────────────────────
        BigDecimal investBalance = BigDecimal.ZERO;
        BigDecimal investNetDeposits = BigDecimal.ZERO;
        for (Wallet w : investWallets) {
            investBalance = investBalance.add(toPln(w.getCurrentValue(), w.getCurrency()));
            investNetDeposits = investNetDeposits.add(
                    toPln(w.getTotalDeposits().subtract(w.getTotalWithdrawals()), w.getCurrency()));
        }
        BigDecimal investReturn = investBalance.subtract(investNetDeposits);
        BigDecimal investReturnPct = investNetDeposits.compareTo(BigDecimal.ZERO) > 0
                ? pct(investReturn.divide(investNetDeposits, 4, RoundingMode.HALF_UP))
                : BigDecimal.ZERO;

        // TWR for investments
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> investTs =
                calculationService.buildAggregatedTimeSeries(investWallets, this::toPln);
        BigDecimal investTwr = pct(calculationService.calculateAggregatedTWR(investTs));

        // CAGR for investments (first snapshot to last)
        double investYears = monthsSpan(investWallets) / 12.0;
        BigDecimal investCagr = BigDecimal.ZERO;
        if (investYears > 0.1 && investNetDeposits.compareTo(BigDecimal.ZERO) > 0) {
            investCagr = pct(calculationService.calculateCAGR(investNetDeposits, investBalance, Math.max(investYears, 0.1)));
        }

        // ── Savings KPIs ────────────────────────────────────────────────────────
        BigDecimal savingsBalance = BigDecimal.ZERO;
        BigDecimal savingsNetDeposits = BigDecimal.ZERO;
        for (Wallet w : savingsWallets) {
            savingsBalance = savingsBalance.add(toPln(w.getCurrentValue(), w.getCurrency()));
            savingsNetDeposits = savingsNetDeposits.add(
                    toPln(w.getTotalDeposits().subtract(w.getTotalWithdrawals()), w.getCurrency()));
        }
        BigDecimal savingsGrowth = savingsBalance.subtract(savingsNetDeposits);
        BigDecimal netWorth = investBalance.add(savingsBalance);

        // ── Time series ─────────────────────────────────────────────────────────
        // investment wallets only
        List<Map<String, Object>> investTimeSeries = buildTimeSeries(investTs);

        // all wallets (net worth)
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> allTs =
                calculationService.buildAggregatedTimeSeries(allWallets, this::toPln);
        List<Map<String, Object>> netWorthTimeSeries = buildTimeSeries(allTs);

        // ── Asset allocation ────────────────────────────────────────────────────
        List<Map<String, Object>> allocation = new ArrayList<>();
        for (Wallet w : allWallets) {
            BigDecimal valuePln = toPln(w.getCurrentValue(), w.getCurrency());
            if (valuePln.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", w.getName());
                entry.put("type", w.getWalletType());
                entry.put("valuePln", valuePln.setScale(2, RoundingMode.HALF_UP));
                allocation.add(entry);
            }
        }

        // ── Monthly returns heatmap (investment wallets) ─────────────────────────
        Map<String, BigDecimal> heatmap = buildHeatmap(investTs);

        // ── Budget data (last 12 months) ────────────────────────────────────────
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

        // ── Per-wallet time series (Balance / Earnings tabs) ───────────────────
        List<Map<String, Object>> walletSeriesList = new ArrayList<>();
        for (Wallet w : allWallets) {
            List<WalletSnapshot> snaps = w.getSnapshots().stream()
                    .filter(s -> s.getSnapshotDate() != null)
                    .sorted(Comparator.comparing(WalletSnapshot::getSnapshotDate))
                    .toList();
            BigDecimal cum = BigDecimal.ZERO;
            List<Map<String, Object>> series = new ArrayList<>();
            for (WalletSnapshot snap : snaps) {
                BigDecimal dep = snap.getMonthlyDeposit() != null ? snap.getMonthlyDeposit() : BigDecimal.ZERO;
                BigDecimal wdr = snap.getMonthlyWithdrawal() != null ? snap.getMonthlyWithdrawal() : BigDecimal.ZERO;
                cum = cum.add(toPln(dep.subtract(wdr), w.getCurrency()));
                BigDecimal pv = snap.getPortfolioValue() != null ? snap.getPortfolioValue() : BigDecimal.ZERO;
                Map<String, Object> pt = new LinkedHashMap<>();
                pt.put("date", snap.getSnapshotDate().toString());
                pt.put("balance", toPln(pv, w.getCurrency()).setScale(2, RoundingMode.HALF_UP));
                pt.put("cumDeposit", cum.setScale(2, RoundingMode.HALF_UP));
                series.add(pt);
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("walletId", w.getId().toString());
            entry.put("walletName", w.getName());
            entry.put("isInvestment", w.isInvestmentLike());
            entry.put("returnRate", w.getReturnRate() != null ? round(pct(w.getReturnRate())) : BigDecimal.ZERO);
            entry.put("series", series);
            walletSeriesList.add(entry);
        }

        // ── Assemble response ───────────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();

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
        result.put("kpi", kpi);

        result.put("investTimeSeries", investTimeSeries);
        result.put("netWorthTimeSeries", netWorthTimeSeries);
        result.put("allocation", allocation);
        result.put("heatmap", heatmap);
        result.put("budget", budgetSeries);
        result.put("walletSeries", walletSeriesList);

        return ResponseEntity.ok(result);
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private BigDecimal toPln(BigDecimal amount, String currency) {
        if (amount == null) return BigDecimal.ZERO;
        return currencyConverter.convert(amount, Currency.of(currency), Currency.PLN);
    }

    private BigDecimal pct(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
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

    private List<Map<String, Object>> buildTimeSeries(
            Map<LocalDate, InvestmentCalculationService.PortfolioPoint> ts) {
        List<Map<String, Object>> list = new ArrayList<>();
        BigDecimal cum = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, InvestmentCalculationService.PortfolioPoint> e : ts.entrySet()) {
            cum = cum.add(e.getValue().cashFlow());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", e.getKey().toString());
            point.put("balance", e.getValue().balance().setScale(2, RoundingMode.HALF_UP));
            point.put("cumDeposit", cum.setScale(2, RoundingMode.HALF_UP));
            list.add(point);
        }
        return list;
    }

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
                result.put(key, ret.setScale(2, RoundingMode.HALF_UP));
            }
        }
        return result;
    }
}
