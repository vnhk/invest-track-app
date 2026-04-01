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

    public WalletsBalanceView(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits, BervanComboBox<String> periodSelectorAggregation, BervanDatePicker fromDateFilter, BervanDatePicker toDateFilter, SP500DataService sp500DataService) {
        this.sp500DataService = sp500DataService;
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

            HorizontalLayout horizontalLayout = new HorizontalLayout(createCard("Total Balance", totalBalance, VaadinIcon.MONEY),
                    createCard("Total Deposit", totalDeposit, VaadinIcon.PIGGY_BANK),
                    createCard("Total Profit", totalBalance.subtract(totalDeposit), VaadinIcon.TRENDING_UP));
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
            List<BigDecimal> sp500Benchmark = computeSP500Benchmark(chartDates, chartSumOfDeposits);

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

    /** Derives monthly net deposits from cumulative sumOfDeposits, then delegates to SP500DataService. */
    private List<BigDecimal> computeSP500Benchmark(List<String> dates, List<BigDecimal> sumOfDeposits) {
        if (sp500DataService == null || dates == null || sumOfDeposits == null || sumOfDeposits.isEmpty()) {
            return List.of();
        }
        List<BigDecimal> monthlyDeposits = new ArrayList<>(sumOfDeposits.size());
        for (int i = 0; i < sumOfDeposits.size(); i++) {
            if (i == 0) {
                monthlyDeposits.add(sumOfDeposits.get(0));
            } else {
                BigDecimal delta = sumOfDeposits.get(i).subtract(sumOfDeposits.get(i - 1));
                monthlyDeposits.add(delta.max(BigDecimal.ZERO));
            }
        }
        return sp500DataService.calculateBenchmarkValues(dates, monthlyDeposits);
    }
}