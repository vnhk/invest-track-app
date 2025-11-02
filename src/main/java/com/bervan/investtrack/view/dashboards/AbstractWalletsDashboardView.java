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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

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

            Map<UUID, List<String>> dates = new HashMap<>();
            Map<UUID, List<BigDecimal>> balances = new HashMap<>();
            Map<UUID, List<BigDecimal>> deposits = new HashMap<>();
            Map<UUID, List<BigDecimal>> sumOfDeposits = new HashMap<>();
            allWalletsCalculations(sortedWallets, dates, balances, deposits, sumOfDeposits);

            List<String> aggregatedDatesForOneWallet = new ArrayList<>();
            List<BigDecimal> aggregatedBalancesForOneWallet = new ArrayList<>();
            List<BigDecimal> aggregatedDepositsForOneWallet = new ArrayList<>();
            List<BigDecimal> aggregatedSumOfDepositsForOneWallet = new ArrayList<>();
            //todo it should aggregate by currency
            oneWalletCalculations(sortedWallets,
                    dates, balances, deposits, sumOfDeposits,
                    aggregatedDatesForOneWallet, aggregatedBalancesForOneWallet, aggregatedDepositsForOneWallet, aggregatedSumOfDepositsForOneWallet);


            aggregationSelector.addValueChangeListener(event -> {
                if (event.getValue().equals("All Wallets")) {
                    createAndAddTabs(sortedWallets, dates, balances, deposits, sumOfDeposits);
                } else if (event.getValue().equals("One Wallet")) {
                    BigDecimal lastSumOfDeposits = aggregatedSumOfDepositsForOneWallet.get(aggregatedSumOfDepositsForOneWallet.size() - 1);
                    BigDecimal lastBalance = aggregatedBalancesForOneWallet.get(aggregatedBalancesForOneWallet.size() - 1);
                    BigDecimal totalReturn = lastBalance.subtract(lastSumOfDeposits);

                    BigDecimal returnRate = BigDecimal.ZERO;
                    if (!lastSumOfDeposits.equals(BigDecimal.ZERO) && !lastBalance.equals(BigDecimal.ZERO)) {
                        returnRate = totalReturn.divide(lastSumOfDeposits, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
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
                                       Map<UUID, List<String>> dates,
                                       Map<UUID, List<BigDecimal>> balances,
                                       Map<UUID, List<BigDecimal>> deposits,
                                       Map<UUID, List<BigDecimal>> sumOfDeposits,
                                       List<String> aggregatedDatesForOneWallet,
                                       List<BigDecimal> aggregatedBalancesForOneWallet,
                                       List<BigDecimal> aggregatedDepositsForOneWallet,
                                       List<BigDecimal> aggregatedSumOfDepositsForOneWallet) {

        Set<LocalDate> allDates = dates.values().stream().flatMap(Collection::stream).map(
                d -> LocalDate.parse(d, new DateTimeFormatterBuilder().appendPattern("dd-MM-yyyy").toFormatter())
        ).collect(Collectors.toSet());

        // Map to aggregate values by date
        Map<LocalDate, BigDecimal> totalBalancesByDate = new TreeMap<>();
        Map<LocalDate, BigDecimal> totalDepositsByDate = new TreeMap<>();
        Map<LocalDate, BigDecimal> totalSumOfDepositsByDate = new TreeMap<>();

        // Go through each wallet and add its values
        for (Wallet wallet : sortedWallets) {
            UUID id = wallet.getId();
            List<String> walletDates = dates.getOrDefault(id, List.of());
            List<BigDecimal> walletBalances = balances.getOrDefault(id, List.of());
            List<BigDecimal> walletDeposits = deposits.getOrDefault(id, List.of());
            List<BigDecimal> walletSumOfDeposits = sumOfDeposits.getOrDefault(id, List.of());

            for (int i = 0; i < walletDates.size(); i++) {
                String sDate = walletDates.get(i);
                LocalDate date = LocalDate.parse(sDate, new DateTimeFormatterBuilder().appendPattern("dd-MM-yyyy").toFormatter());
                if (date.getDayOfMonth() != date.lengthOfMonth()) {
                    date = date.with(TemporalAdjusters.lastDayOfMonth());
                }

                // For safety, handle possible size mismatches
                BigDecimal balance = i < walletBalances.size() ? walletBalances.get(i) : BigDecimal.ZERO;
                BigDecimal deposit = i < walletDeposits.size() ? walletDeposits.get(i) : BigDecimal.ZERO;
                BigDecimal sumDeposit = i < walletSumOfDeposits.size() ? walletSumOfDeposits.get(i) : BigDecimal.ZERO;

                // Aggregate by date
                totalBalancesByDate.merge(date, balance, BigDecimal::add);
                totalDepositsByDate.merge(date, deposit, BigDecimal::add);
                totalSumOfDepositsByDate.merge(date, sumDeposit, BigDecimal::add);
            }
        }

        // Fill output lists in chronological order
        for (LocalDate date : totalBalancesByDate.keySet()) {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            aggregatedDatesForOneWallet.add(formattedDate);
            aggregatedBalancesForOneWallet.add(totalBalancesByDate.get(date));
            aggregatedDepositsForOneWallet.add(totalDepositsByDate.get(date));
            aggregatedSumOfDepositsForOneWallet.add(totalSumOfDepositsByDate.get(date));
        }
        sortAggregatedData(aggregatedDatesForOneWallet, aggregatedBalancesForOneWallet, aggregatedDepositsForOneWallet, aggregatedSumOfDepositsForOneWallet);
    }

    // Sort all lists by date (keeping values in sync)
    private void sortAggregatedData(List<String> aggregatedDates,
                                    List<BigDecimal> aggregatedBalances,
                                    List<BigDecimal> aggregatedDeposits,
                                    List<BigDecimal> aggregatedSumOfDeposits) {

        // Create combined list of indices
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < aggregatedDates.size(); i++) {
            indices.add(i);
        }

        // Sort indices by parsed LocalDate or fallback to string compare
        indices.sort(Comparator.comparing((Integer i) -> {
            String dateStr = aggregatedDates.get(i);
            try {
                return LocalDate.parse(dateStr, new DateTimeFormatterBuilder().appendPattern("dd-MM-yyyy").toFormatter());
            } catch (Exception e) {
                return LocalDate.MIN; // fallback for invalid dates
            }
        }));

        // Reorder all lists using sorted indices
        List<String> sortedDates = new ArrayList<>();
        List<BigDecimal> sortedBalances = new ArrayList<>();
        List<BigDecimal> sortedDeposits = new ArrayList<>();
        List<BigDecimal> sortedSumOfDeposits = new ArrayList<>();

        for (int idx : indices) {
            sortedDates.add(aggregatedDates.get(idx));
            sortedBalances.add(aggregatedBalances.get(idx));
            sortedDeposits.add(aggregatedDeposits.get(idx));
            sortedSumOfDeposits.add(aggregatedSumOfDeposits.get(idx));
        }

        // Replace original lists’ contents
        aggregatedDates.clear();
        aggregatedDates.addAll(sortedDates);
        aggregatedBalances.clear();
        aggregatedBalances.addAll(sortedBalances);
        aggregatedDeposits.clear();
        aggregatedDeposits.addAll(sortedDeposits);
        aggregatedSumOfDeposits.clear();
        aggregatedSumOfDeposits.addAll(sortedSumOfDeposits);
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