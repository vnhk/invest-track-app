package com.bervan.investtrack.view.dashboards;

import com.bervan.common.component.BervanComboBox;
import com.bervan.common.component.BervanDatePicker;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.service.SP500DataService;
import com.bervan.investtrack.view.charts.WalletBalanceSumOfDepositsCharts;
import com.bervan.logging.JsonLogger;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WalletsBalanceView extends AbstractWalletsBaseDashboardView {
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final SP500DataService sp500DataService;
    // null = use per-wallet compareWithSP500 flag; non-null = override deposits for benchmark (aggregated mode)
    private final Map<UUID, List<BigDecimal>> sp500SumOfDepositsOverride;

    public WalletsBalanceView(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits, BervanComboBox<String> periodSelectorAggregation, BervanDatePicker fromDateFilter, BervanDatePicker toDateFilter, SP500DataService sp500DataService,
                              Map<UUID, List<BigDecimal>> sp500SumOfDepositsOverride) {
        this.sp500DataService = sp500DataService;
        this.sp500SumOfDepositsOverride = sp500SumOfDepositsOverride;
        try {
            Div gridContainer = getGridContainer();

            BigDecimal totalBalance = BigDecimal.ZERO;
            BigDecimal totalDeposit = BigDecimal.ZERO;

            if (balances.size() == 1) {
                UUID walletId = balances.keySet().iterator().next();
                totalBalance = getTotalBalance(balances, walletId);
                totalDeposit = getTotalSumOfDeposit(sumOfDeposits, walletId);
            } else if (balances.size() > 1) {
                for (Wallet wallet : wallets) {
                    totalBalance = totalBalance.add(getTotalBalance(balances, wallet.getId()));
                    totalDeposit = totalDeposit.add(getTotalSumOfDeposit(sumOfDeposits, wallet.getId()));
                }
            }

            BigDecimal totalSP500Final = computeTotalSP500Final(wallets, dates, sumOfDeposits, sp500SumOfDepositsOverride);
            BigDecimal sp500Profit = totalSP500Final.subtract(totalDeposit);

            HorizontalLayout horizontalLayout = new HorizontalLayout(createCard("Total Balance", totalBalance, VaadinIcon.MONEY),
                    createCard("Total Deposit", totalDeposit, VaadinIcon.PIGGY_BANK),
                    createCard("Total Profit", totalBalance.subtract(totalDeposit), VaadinIcon.TRENDING_UP),
                    createCard("Total Profit if SP500", sp500Profit, VaadinIcon.CHART_LINE));
            horizontalLayout.addClassName("invest-kpi-row");
            add(horizontalLayout);

            add(gridContainer);

            buildCharts(wallets, dates, periodSelectorAggregation.getValue(), balances, sumOfDeposits, gridContainer, fromDateFilter.getValue(), toDateFilter.getValue());
        } catch (Exception e) {
            log.error("Failed to load wallets: {}", e.getMessage(), e);
            showErrorNotification("Failed to load wallets!");
        }
    }

    private BigDecimal getTotalBalance(Map<UUID, List<BigDecimal>> balances, UUID walletId) {
        List<BigDecimal> b = balances.get(walletId);
        if (b == null || b.isEmpty()) return BigDecimal.ZERO;
        return b.get(b.size() - 1).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal getTotalSumOfDeposit(Map<UUID, List<BigDecimal>> sumOfDeposits, UUID walletId) {
        List<BigDecimal> b = sumOfDeposits.get(walletId);
        if (b == null || b.isEmpty()) return BigDecimal.ZERO;
        return b.get(b.size() - 1).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    protected void buildCharts(List<Wallet> wallets, Map<UUID, List<String>> dates, String selected, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> sumOfDeposits, Div gridContainer,
                               LocalDate fromDateFilterValue, LocalDate toDateFilterValue) {
        String fromDate = fromDateFilterValue.toString();
        String toDate = toDateFilterValue.toString();
        for (Wallet wallet : wallets) {
            // Get aggregated data based on selection
            Map<String, List<String>> aggregatedDates = aggregateData(dates, selected);
            Map<String, List<BigDecimal>> aggregatedBalances = aggregateData(balances, selected);
            Map<String, List<BigDecimal>> aggregatedSumOfDeposits = aggregateData(sumOfDeposits, selected);

            Map<String, List<String>> filteredDates = filterDatesByRange(aggregatedDates, fromDate, toDate);
            Map<String, List<BigDecimal>> filteredBalances = filterBigDecimalsByIndexes(aggregatedBalances, filteredDates, aggregatedDates);
            Map<String, List<BigDecimal>> filteredSumOfDeposits = filterBigDecimalsByIndexes(aggregatedSumOfDeposits, filteredDates, aggregatedDates);

            String returnRate = "";
            if (wallet.getReturnRate() != null && !Objects.equals(wallet.getReturnRate(), BigDecimal.ZERO)) {
                returnRate = " (" + formatPercentage(wallet.getReturnRate()) + ")";
            }

            VerticalLayout walletTile = createWalletTile(
                    wallet.getName() + returnRate
            );

            List<String> chartDates = filteredDates.get(wallet.getId().toString());
            List<BigDecimal> chartSumOfDeposits = filteredSumOfDeposits.get(wallet.getId().toString());

            List<BigDecimal> sp500Benchmark;
            if (sp500SumOfDepositsOverride != null) {
                // Aggregated mode: use pre-filtered SP500 deposits (only compareWithSP500=true wallets)
                List<BigDecimal> sp500Deposits = sp500SumOfDepositsOverride.get(wallet.getId());
                // Need to apply same date filtering as chartSumOfDeposits
                Map<String, List<BigDecimal>> filteredSP500 = filterBigDecimalsByIndexes(
                        Map.of(wallet.getId().toString(), sp500Deposits != null ? sp500Deposits : List.of()),
                        filteredDates,
                        aggregatedDates);
                List<BigDecimal> filteredSP500Deposits = filteredSP500.get(wallet.getId().toString());
                List<BigDecimal> sp500Part = computeSP500Benchmark(chartDates, filteredSP500Deposits, wallet.getCurrency());
                // Add non-SP500 deposits as cash (no S&P 500 growth on them)
                List<BigDecimal> result = new ArrayList<>();
                for (int i = 0; i < sp500Part.size(); i++) {
                    BigDecimal totalDep = (chartSumOfDeposits != null && i < chartSumOfDeposits.size()) ? chartSumOfDeposits.get(i) : BigDecimal.ZERO;
                    BigDecimal sp500Dep = (filteredSP500Deposits != null && i < filteredSP500Deposits.size()) ? filteredSP500Deposits.get(i) : BigDecimal.ZERO;
                    BigDecimal nonSP500Dep = totalDep.subtract(sp500Dep).max(BigDecimal.ZERO);
                    result.add(sp500Part.get(i).add(nonSP500Dep));
                }
                sp500Benchmark = result;
            } else if (!Boolean.FALSE.equals(wallet.getCompareWithSP500())) {
                sp500Benchmark = computeSP500Benchmark(chartDates, chartSumOfDeposits, wallet.getCurrency());
            } else {
                sp500Benchmark = List.of();
            }

            WalletBalanceSumOfDepositsCharts chart = new WalletBalanceSumOfDepositsCharts(
                    chartDates,
                    filteredBalances.get(wallet.getId().toString()),
                    chartSumOfDeposits,
                    sp500Benchmark);
            chart.setWidthFull();
            chart.getStyle().set("flex-grow", "1");
            walletTile.add(chart);
            gridContainer.add(walletTile);
        }
    }

    private BigDecimal computeTotalSP500Final(List<Wallet> wallets, Map<UUID, List<String>> dates,
                                              Map<UUID, List<BigDecimal>> sumOfDeposits,
                                              Map<UUID, List<BigDecimal>> sp500SumOfDepositsOverride) {
        BigDecimal total = BigDecimal.ZERO;
        if (sp500SumOfDepositsOverride != null) {
            // Aggregated mode: one synthetic wallet
            for (Wallet wallet : wallets) {
                UUID id = wallet.getId();
                List<String> wDates = dates.get(id);
                List<BigDecimal> wSumOfDeposits = sumOfDeposits.get(id);
                List<BigDecimal> sp500Deps = sp500SumOfDepositsOverride.get(id);
                List<BigDecimal> sp500Part = computeSP500Benchmark(wDates, sp500Deps, wallet.getCurrency());
                if (!sp500Part.isEmpty()) {
                    BigDecimal sp500Final = sp500Part.get(sp500Part.size() - 1);
                    BigDecimal totalDep = (wSumOfDeposits != null && !wSumOfDeposits.isEmpty()) ? wSumOfDeposits.get(wSumOfDeposits.size() - 1) : BigDecimal.ZERO;
                    BigDecimal sp500Dep = (sp500Deps != null && !sp500Deps.isEmpty()) ? sp500Deps.get(sp500Deps.size() - 1) : BigDecimal.ZERO;
                    BigDecimal nonSP500Dep = totalDep.subtract(sp500Dep).max(BigDecimal.ZERO);
                    total = total.add(sp500Final).add(nonSP500Dep);
                }
            }
        } else {
            // All-wallets mode: per-wallet flag
            for (Wallet wallet : wallets) {
                UUID id = wallet.getId();
                List<String> wDates = dates.get(id);
                List<BigDecimal> wSumOfDeposits = sumOfDeposits.get(id);
                if (wDates == null || wSumOfDeposits == null || wSumOfDeposits.isEmpty()) continue;
                if (!Boolean.FALSE.equals(wallet.getCompareWithSP500())) {
                    List<BigDecimal> benchmark = computeSP500Benchmark(wDates, wSumOfDeposits, wallet.getCurrency());
                    if (!benchmark.isEmpty()) {
                        total = total.add(benchmark.get(benchmark.size() - 1));
                    }
                } else {
                    // Non-SP500 wallet: count deposits as-is (no S&P 500 growth)
                    total = total.add(wSumOfDeposits.get(wSumOfDeposits.size() - 1));
                }
            }
        }
        return total;
    }

    /** Derives monthly net deposits from cumulative sumOfDeposits, then delegates to SP500DataService. */
    private List<BigDecimal> computeSP500Benchmark(List<String> dates, List<BigDecimal> sumOfDeposits, String currency) {
        if (sp500DataService == null || dates == null || sumOfDeposits == null || sumOfDeposits.isEmpty()) {
            return List.of();
        }
        List<BigDecimal> monthlyDeposits = new ArrayList<>(sumOfDeposits.size());
        for (int i = 0; i < sumOfDeposits.size(); i++) {
            if (i == 0) {
                monthlyDeposits.add(sumOfDeposits.get(0));
            } else {
                BigDecimal delta = sumOfDeposits.get(i).subtract(sumOfDeposits.get(i - 1));
                monthlyDeposits.add(delta); // negative = withdrawal, handled in SP500DataService
            }
        }
        return sp500DataService.calculateBenchmarkValues(dates, monthlyDeposits, currency != null ? currency : "PLN");
    }
}