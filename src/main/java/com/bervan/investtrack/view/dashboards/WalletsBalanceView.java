package com.bervan.investtrack.view.dashboards;

import com.bervan.common.component.BervanComboBox;
import com.bervan.common.component.BervanDatePicker;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.view.charts.WalletBalanceSumOfDepositsCharts;
import com.bervan.logging.JsonLogger;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WalletsBalanceView extends AbstractWalletsBaseDashboardView {
    private final JsonLogger log = JsonLogger.getLogger(getClass());

    public WalletsBalanceView(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits, BervanComboBox<String> periodSelectorAggregation, BervanDatePicker fromDateFilter, BervanDatePicker toDateFilter) {
        try {
            Div gridContainer = getGridContainer();
            periodSelectorAggregation.addValueChangeListener(e -> {
                String selected = e.getValue();
                if (selected == null) return;
                gridContainer.removeAll();
                buildCharts(wallets, dates, selected, balances, sumOfDeposits, gridContainer, fromDateFilter.getValue(), toDateFilter.getValue());
            });

            fromDateFilter.addValueChangeListener(event -> {
                gridContainer.removeAll();
                buildCharts(wallets, dates, periodSelectorAggregation.getValue(), balances, sumOfDeposits, gridContainer, event.getValue(), toDateFilter.getValue());
            });

            toDateFilter.addValueChangeListener(event -> {
                gridContainer.removeAll();
                buildCharts(wallets, dates, periodSelectorAggregation.getValue(), balances, sumOfDeposits, gridContainer, fromDateFilter.getValue(), event.getValue());
            });

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
            horizontalLayout.getStyle().setMarginLeft("20px");
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
        if (b.isEmpty()) return BigDecimal.ZERO;
        return b.get(b.size() - 1).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal getTotalSumOfDeposit(Map<UUID, List<BigDecimal>> sumOfDeposits, UUID walletId) {
        List<BigDecimal> b = sumOfDeposits.get(walletId);
        if (b.isEmpty()) return BigDecimal.ZERO;
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

            WalletBalanceSumOfDepositsCharts chart = new WalletBalanceSumOfDepositsCharts(
                    filteredDates.get(wallet.getId().toString()),
                    filteredBalances.get(wallet.getId().toString()),
                    filteredSumOfDeposits.get(wallet.getId().toString()));
            chart.setWidthFull();
            chart.getStyle().set("flex-grow", "1");
            walletTile.add(chart);
            gridContainer.add(walletTile);
        }
    }
}