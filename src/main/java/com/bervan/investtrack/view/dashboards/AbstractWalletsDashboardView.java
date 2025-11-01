package com.bervan.investtrack.view.dashboards;

import com.bervan.common.component.BervanComboBox;
import com.bervan.common.view.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.WalletService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletsDashboardView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/wallets-dashboard/";
    private final VerticalLayout content = new VerticalLayout();
    private final BervanComboBox<String> aggregationPeriodSelector = createAggregationPeriodSelector();
    private final BervanComboBox<String> aggregationSelector = createAggregationSelector();

    public AbstractWalletsDashboardView(WalletService service) {
        try {
            Set<Wallet> wallets = service.load(Pageable.ofSize(16));
            if (wallets.size() > 16) {
                showWarningNotification("Too many wallets loaded. Only the first 16 will be displayed.");
            }

            List<Wallet> sortedWallets = wallets.stream().sorted(Comparator.comparing(Wallet::getReturnRate).reversed()).toList();

            List<String> aggregatedDatesForOneWallet = new ArrayList<>();
            List<BigDecimal> aggregatedBalancesForOneWallet = new ArrayList<>();
            List<BigDecimal> aggregatedDepositsForOneWallet = new ArrayList<>();
            List<BigDecimal> aggregatedSumOfDepositsForOneWallet = new ArrayList<>();
            //todo it should aggregate by currency
            oneWalletCalculations(sortedWallets, aggregatedDatesForOneWallet, aggregatedBalancesForOneWallet, aggregatedDepositsForOneWallet, aggregatedSumOfDepositsForOneWallet);

            Map<UUID, List<String>> dates = new HashMap<>();
            Map<UUID, List<BigDecimal>> balances = new HashMap<>();
            Map<UUID, List<BigDecimal>> deposits = new HashMap<>();
            Map<UUID, List<BigDecimal>> sumOfDeposits = new HashMap<>();
            allWalletsCalculations(sortedWallets, dates, balances, deposits, sumOfDeposits);

            aggregationSelector.addValueChangeListener(event -> {
                if (event.getValue().equals("All Wallets")) {
                    createAndAddTabs(sortedWallets, dates, balances, deposits, sumOfDeposits);
                } else if (event.getValue().equals("One Wallet")) {
                    BigDecimal lastSumOfDeposits = aggregatedSumOfDepositsForOneWallet.get(aggregatedSumOfDepositsForOneWallet.size() - 1);
                    BigDecimal lastBalance = aggregatedBalancesForOneWallet.get(aggregatedBalancesForOneWallet.size() - 1);
                    BigDecimal returnRate = BigDecimal.ZERO;
                    if (!lastSumOfDeposits.equals(BigDecimal.ZERO) && !lastBalance.equals(BigDecimal.ZERO)) {
                        returnRate = lastBalance.divide(lastSumOfDeposits, 2, BigDecimal.ROUND_HALF_UP);
                    }

                    Wallet oneAggregatedWallet = new Wallet();
                    oneAggregatedWallet.setName("Aggregated Wallet (" + returnRate + "%)");
                    oneAggregatedWallet.setId(UUID.randomUUID());

                    createAndAddTabs(List.of(oneAggregatedWallet), Map.of(oneAggregatedWallet.getId(), aggregatedDatesForOneWallet),
                            Map.of(oneAggregatedWallet.getId(), aggregatedBalancesForOneWallet), Map.of(oneAggregatedWallet.getId(), aggregatedDepositsForOneWallet),
                            Map.of(oneAggregatedWallet.getId(), aggregatedSumOfDepositsForOneWallet));
                }
            });

            createAndAddTabs(sortedWallets, dates, balances, deposits, sumOfDeposits);

        } catch (Exception e) {
            log.error("Failed to load wallets: {}", e.getMessage(), e);
            showErrorNotification("Failed to load wallets!");
        }
    }

    private void oneWalletCalculations(List<Wallet> sortedWallets,
                                       List<String> aggregatedDatesForOneWallet,
                                       List<BigDecimal> aggregatedBalancesForOneWallet,
                                       List<BigDecimal> aggregatedDepositsForOneWallet,
                                       List<BigDecimal> aggregatedSumOfDepositsForOneWallet) {
        Map<String, List<WalletSnapshot>> walletSnapshotsForADate = new HashMap<>();

        List<WalletSnapshot> allSnapshotsSorted = sortedWallets.stream().flatMap(wallet -> wallet.getSnapshots().stream()).sorted(Comparator.comparing(WalletSnapshot::getSnapshotDate)).toList();
        for (WalletSnapshot snapshot : allSnapshotsSorted) {
            String date = snapshot.getSnapshotDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            aggregatedDatesForOneWallet.add(date);
            walletSnapshotsForADate.putIfAbsent(date, new ArrayList<>());
            walletSnapshotsForADate.get(date).add(snapshot);
        }

        for (String date : aggregatedDatesForOneWallet) {
            BigDecimal totalBalanceForDate = BigDecimal.ZERO;
            BigDecimal totalDepositsForDate = BigDecimal.ZERO;
            for (WalletSnapshot walletSnapshot : walletSnapshotsForADate.get(date)) {
                totalBalanceForDate = totalBalanceForDate.add(walletSnapshot.getPortfolioValue());
                totalDepositsForDate = totalDepositsForDate.add(walletSnapshot.getMonthlyDeposit().subtract(walletSnapshot.getMonthlyWithdrawal()));
            }
            aggregatedBalancesForOneWallet.add(totalBalanceForDate);
            aggregatedDepositsForOneWallet.add(totalDepositsForDate);

            BigDecimal previousSum = aggregatedSumOfDepositsForOneWallet.isEmpty()
                    ? BigDecimal.ZERO
                    : aggregatedSumOfDepositsForOneWallet.get(aggregatedSumOfDepositsForOneWallet.size() - 1);
            aggregatedSumOfDepositsForOneWallet.add(previousSum.add(totalDepositsForDate));
        }
    }

    private void allWalletsCalculations(List<Wallet> sortedWallets,
                                        Map<UUID, List<String>> dates,
                                        Map<UUID, List<BigDecimal>> balances,
                                        Map<UUID, List<BigDecimal>> deposits,
                                        Map<UUID, List<BigDecimal>> sumOfDeposits) {
        sortedWallets.forEach(wallet -> {
            dates.put(wallet.getId(), new ArrayList<>());
            balances.put(wallet.getId(), new ArrayList<>());
            deposits.put(wallet.getId(), new ArrayList<>());
            sumOfDeposits.put(wallet.getId(), new ArrayList<>());

            for (WalletSnapshot snapshot : wallet.getSnapshots()) {
                dates.get(wallet.getId()).add(snapshot.getSnapshotDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                balances.get(wallet.getId()).add(snapshot.getPortfolioValue());
                deposits.get(wallet.getId()).add(snapshot.getMonthlyDeposit().subtract(snapshot.getMonthlyWithdrawal()));
            }

            for (BigDecimal deposit : deposits.get(wallet.getId())) {
                sumOfDeposits.get(wallet.getId()).add(deposit.add(sumOfDeposits.get(wallet.getId()).isEmpty() ? BigDecimal.ZERO : sumOfDeposits.get(wallet.getId()).get(sumOfDeposits.get(wallet.getId()).size() - 1)));
            }
        });
    }

    private void createAndAddTabs(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits) {
        removeAll();
        add(new InvestTrackPageLayout(ROUTE_NAME, null));
        Tabs tabs = new Tabs();
        Tab balance = new Tab("Balance");
        Tab earnings = new Tab("Earnings");
        tabs.add(balance, earnings);
        add(new HorizontalLayout(aggregationSelector, aggregationPeriodSelector));
        add(tabs);
        add(content);

        tabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab.getLabel().equals("Balance")) {
                balanceTab(wallets, dates, balances, deposits, sumOfDeposits);
            } else if (selectedTab.getLabel().equals("Earnings")) {
                earningsTab(wallets, dates, balances, deposits, sumOfDeposits);
            }
        });

        tabs.setSelectedTab(balance);
        balanceTab(wallets, dates, balances, deposits, sumOfDeposits);
    }

    private void earningsTab(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits) {
        content.removeAll();
        content.add(new WalletsEarningsView(wallets, dates, balances, deposits, sumOfDeposits, aggregationPeriodSelector));
    }

    private void balanceTab(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits) {
        content.removeAll();
        content.add(new WalletsBalanceView(wallets, dates, balances, deposits, sumOfDeposits, aggregationPeriodSelector));
    }

    private BervanComboBox<String> createAggregationPeriodSelector() {
        BervanComboBox<String> monthsDropdown = new BervanComboBox<>("Select time aggregation period", false);
        monthsDropdown.setItems("Monthly", "Two-Monthly", "Quarterly", "Half-Yearly", "Yearly");
        monthsDropdown.setValue("Monthly");
        monthsDropdown.setWidth("300px");
        return monthsDropdown;
    }

    private BervanComboBox<String> createAggregationSelector() {
        BervanComboBox<String> monthsDropdown = new BervanComboBox<>("Select Aggregation", false);
        monthsDropdown.setItems("All Wallets", "One Wallet");
        monthsDropdown.setValue("All Wallets");
        monthsDropdown.setWidth("300px");
        return monthsDropdown;
    }
}