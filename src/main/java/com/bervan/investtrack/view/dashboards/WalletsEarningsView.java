package com.bervan.investtrack.view.dashboards;

import com.bervan.common.component.BervanComboBox;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.view.charts.WalletEarningsCharts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class WalletsEarningsView extends AbstractWalletsBaseDashboardView {

    public WalletsEarningsView(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits, BervanComboBox<String> periodSelectorAggregation) {
        try {
            Div gridContainer = getGridContainer();
            periodSelectorAggregation.addValueChangeListener(e -> {
                String selected = e.getValue();
                if (selected == null) return;
                gridContainer.removeAll();
                buildCharts(wallets, dates, selected, balances, sumOfDeposits, gridContainer);
            });

            BigDecimal totalEarnings = BigDecimal.ZERO;
            if (balances.size() == 1) {
                UUID walletId = sumOfDeposits.keySet().iterator().next();
                totalEarnings = getTotalEarnings(balances, sumOfDeposits, walletId);
            } else if (balances.size() > 1) {
                for (Wallet wallet : wallets) {
                    totalEarnings = totalEarnings.add(getTotalEarnings(balances, sumOfDeposits, wallet.getId()));
                }
            }

            add(new HorizontalLayout(new H3("Total earnings: " + totalEarnings)));
            add(gridContainer);

            buildCharts(wallets, dates, periodSelectorAggregation.getValue(), balances, sumOfDeposits, gridContainer);
        } catch (Exception e) {
            log.error("Failed to load wallets: {}", e.getMessage(), e);
            showErrorNotification("Failed to load wallets!");
        }
    }

    private BigDecimal getTotalEarnings(Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> sumOfDeposits, UUID walletId) {
        BigDecimal totalEarnings;
        List<BigDecimal> sod = sumOfDeposits.get(walletId);
        List<BigDecimal> b = balances.get(walletId);
        totalEarnings = b.get(b.size() - 1).subtract(sod.get(sod.size() - 1)).setScale(2, BigDecimal.ROUND_HALF_UP);
        return totalEarnings;
    }

    protected void buildCharts(List<Wallet> wallets, Map<UUID, List<String>> dates, String selected, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> sumOfDeposits, Div gridContainer) {
        for (Wallet wallet : wallets) {
            // Get aggregated data based on selection
            Map<String, List<String>> aggregatedDates = aggregateData(dates, selected);
            Map<String, List<BigDecimal>> aggregatedBalances = aggregateData(balances, selected);
            Map<String, List<BigDecimal>> aggregatedSumOfDeposits = aggregateData(sumOfDeposits, selected);

            String returnRate = "";
            if (wallet.getReturnRate() != null && !Objects.equals(wallet.getReturnRate(), BigDecimal.ZERO)) {
                returnRate = " (" + formatPercentage(wallet.getReturnRate()) + ")";
            }

            VerticalLayout walletTile = createWalletTile(
                    wallet.getName() + returnRate
            );

            WalletEarningsCharts chart = new WalletEarningsCharts(aggregatedDates.get(wallet.getId().toString()),
                    aggregatedBalances.get(wallet.getId().toString()),
                    aggregatedSumOfDeposits.get(wallet.getId().toString()));
            chart.setWidthFull();
            chart.getStyle().set("flex-grow", "1");
            walletTile.add(chart);
            gridContainer.add(walletTile);
        }
    }
}