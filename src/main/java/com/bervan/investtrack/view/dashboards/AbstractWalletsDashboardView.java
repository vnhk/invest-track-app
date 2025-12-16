package com.bervan.investtrack.view.dashboards;

import com.bervan.common.component.BervanComboBox;
import com.bervan.common.component.BervanDatePicker;
import com.bervan.common.view.AbstractPageView;
import com.bervan.investments.recommendation.InvestmentRecommendationService;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.CurrencyConverter;
import com.bervan.investtrack.service.WalletService;
import com.bervan.investtrack.service.recommendations.ShortTermRecommendationStrategy;
import com.bervan.logging.JsonLogger;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@CssImport("./invest-track.css")
public abstract class AbstractWalletsDashboardView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/wallets-dashboard/";
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final VerticalLayout content = new VerticalLayout();
    private final BervanComboBox<String> aggregationPeriodSelector = createAggregationPeriodSelector();
    private final BervanComboBox<String> aggregationSelector = createAggregationSelector();
    private final BervanComboBox<String> currencySelector = createCurrencySelector();
    private final BervanDatePicker fromDateFilter = createDateFilter("From");
    private final BervanDatePicker toDateFilter = createDateFilter("To");
    private final Map<UUID, List<String>> dates = new HashMap<>();
    private final Map<UUID, List<BigDecimal>> balances = new HashMap<>();
    private final Map<UUID, List<BigDecimal>> deposits = new HashMap<>();
    private final Map<UUID, List<BigDecimal>> sumOfDeposits = new HashMap<>();
    private final List<String> aggregatedDatesForOneWallet = new ArrayList<>();
    private final List<BigDecimal> aggregatedBalancesForOneWallet = new ArrayList<>();
    private final List<BigDecimal> aggregatedDepositsForOneWallet = new ArrayList<>();
    private final List<BigDecimal> aggregatedSumOfDepositsForOneWallet = new ArrayList<>();
    private final CurrencyConverter currencyConverter;
    private final Map<String, ShortTermRecommendationStrategy> strategies;
    private final InvestmentRecommendationService recommendationService;

    public AbstractWalletsDashboardView(CurrencyConverter currencyConverter,
                                        WalletService service, Map<String, ShortTermRecommendationStrategy> strategies,
                                        InvestmentRecommendationService recommendationService) {
        this.currencyConverter = currencyConverter;
        this.strategies = strategies;
        this.recommendationService = recommendationService;
        try {
            Set<Wallet> wallets = service.load(Pageable.ofSize(16));
            if (wallets.size() > 16) {
                showWarningNotification("Too many wallets loaded. Only the first 16 will be displayed.");
            }

            List<Wallet> sortedWallets = wallets.stream().sorted(Comparator.comparing(Wallet::getReturnRate).reversed()).toList();

            Optional<WalletSnapshot> minDateOpt = wallets.stream().flatMap(wallet -> wallet.getSnapshots().stream())
                    .min(Comparator.comparing(WalletSnapshot::getSnapshotDate));

            Optional<WalletSnapshot> maxDateOpt = wallets.stream().flatMap(wallet -> wallet.getSnapshots().stream())
                    .max(Comparator.comparing(WalletSnapshot::getSnapshotDate));

            minDateOpt.ifPresent(walletSnapshot -> fromDateFilter.setValue(walletSnapshot.getSnapshotDate()));
            maxDateOpt.ifPresent(walletSnapshot -> toDateFilter.setValue(walletSnapshot.getSnapshotDate()));

            aggregationSelector.addValueChangeListener(event -> {
                refreshDashboards(sortedWallets);
            });

            currencySelector.addValueChangeListener(event -> {
                refreshDashboards(sortedWallets);
            });

            refreshDashboards(sortedWallets);

        } catch (Exception e) {
            log.error("Failed to load wallets: {}", e.getMessage(), e);
            showErrorNotification("Failed to load wallets!");
        }
    }

    private void refreshDashboards(List<Wallet> sortedWallets) {
        sortedWallets = filterAndPrepareCopy(sortedWallets);
        allWalletsCalculations(sortedWallets);
        oneWalletCalculations(sortedWallets);

        if (aggregationSelector.getValue().equals("All Wallets")) {
            createAndAddTabs(sortedWallets, dates, balances, deposits, sumOfDeposits);
        } else if (aggregationSelector.getValue().equals("One Wallet")) {
            BigDecimal returnRate = getReturnRateForAggregatedWallets();

            Wallet oneAggregatedWallet = new Wallet();
            oneAggregatedWallet.setName("Aggregated Wallet (" + returnRate + "%)");
            oneAggregatedWallet.setId(UUID.randomUUID());

            createAndAddTabs(List.of(oneAggregatedWallet), Map.of(oneAggregatedWallet.getId(), aggregatedDatesForOneWallet),
                    Map.of(oneAggregatedWallet.getId(), aggregatedBalancesForOneWallet), Map.of(oneAggregatedWallet.getId(), aggregatedDepositsForOneWallet),
                    Map.of(oneAggregatedWallet.getId(), aggregatedSumOfDepositsForOneWallet));
        }
    }

    private List<Wallet> filterAndPrepareCopy(List<Wallet> sortedWallets) {
        return sortedWallets.stream()
                .map(original -> {
                    Wallet copy = new Wallet();
                    copy.setId(original.getId());
                    copy.setCurrency(currencySelector.getValue());
                    copy.setName(original.getName());
                    copy.setSnapshots(original.getSnapshots().stream()
                            .map(snapshot -> {
                                WalletSnapshot sCopy = new WalletSnapshot();
                                sCopy.setSnapshotDate(snapshot.getSnapshotDate());
                                sCopy.setMonthlyDeposit(currencyConverter.convert(snapshot.getMonthlyDeposit(),
                                        CurrencyConverter.Currency.of(original.getCurrency()),
                                        CurrencyConverter.Currency.of(currencySelector.getValue())));
                                sCopy.setMonthlyEarnings(currencyConverter.convert(snapshot.getMonthlyEarnings(),
                                        CurrencyConverter.Currency.of(original.getCurrency()),
                                        CurrencyConverter.Currency.of(currencySelector.getValue())));
                                sCopy.setMonthlyWithdrawal(currencyConverter.convert(snapshot.getMonthlyWithdrawal(),
                                        CurrencyConverter.Currency.of(original.getCurrency()),
                                        CurrencyConverter.Currency.of(currencySelector.getValue())));
                                sCopy.setPortfolioValue(currencyConverter.convert(snapshot.getPortfolioValue(),
                                        CurrencyConverter.Currency.of(original.getCurrency()),
                                        CurrencyConverter.Currency.of(currencySelector.getValue())));
                                return sCopy;
                            }).toList());
                    return copy;
                }).toList();
    }

    private BigDecimal getReturnRateForAggregatedWallets() {
        BigDecimal lastSumOfDeposits = aggregatedSumOfDepositsForOneWallet.get(aggregatedSumOfDepositsForOneWallet.size() - 1);
        BigDecimal lastBalance = aggregatedBalancesForOneWallet.get(aggregatedBalancesForOneWallet.size() - 1);
        BigDecimal totalReturn = lastBalance.subtract(lastSumOfDeposits);

        BigDecimal returnRate = BigDecimal.ZERO;
        if (!lastSumOfDeposits.equals(BigDecimal.ZERO) && !lastBalance.equals(BigDecimal.ZERO)) {
            returnRate = totalReturn.divide(lastSumOfDeposits, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return returnRate;
    }

    private void oneWalletCalculations(List<Wallet> sortedWallets) {
        aggregatedDatesForOneWallet.clear();
        aggregatedBalancesForOneWallet.clear();
        aggregatedDepositsForOneWallet.clear();
        aggregatedSumOfDepositsForOneWallet.clear();

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

    private void allWalletsCalculations(List<Wallet> sortedWallets) {
        dates.clear();
        deposits.clear();
        balances.clear();
        sumOfDeposits.clear();

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
        Tab fire = new Tab("FIRE");
        Tab shortTermStrategies = new Tab("Short Term Strategies");
        tabs.add(balance, earnings, fire, shortTermStrategies);
        add(tabs);
        VerticalLayout filtersLayout = new VerticalLayout(new HorizontalLayout(aggregationSelector, aggregationPeriodSelector, currencySelector, fromDateFilter, toDateFilter));
        filtersLayout.setSpacing(true);
        filtersLayout.setPadding(true);
        add(filtersLayout);
        add(content);

        tabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();

            if (selectedTab.getLabel().equals("Balance")) {
                filtersLayout.setVisible(true);
                balanceTab(wallets, dates, balances, deposits, sumOfDeposits);
            } else if (selectedTab.getLabel().equals("Earnings")) {
                filtersLayout.setVisible(true);
                earningsTab(wallets, dates, balances, deposits, sumOfDeposits);
            } else if (selectedTab.getLabel().equals("FIRE")) {
                filtersLayout.setVisible(false);
                fireTab(wallets, dates, balances, deposits, sumOfDeposits);
            } else if (selectedTab.getLabel().equals("Short Term Strategies")) {
                filtersLayout.setVisible(false);
                strategiesTab();
            }
        });

        tabs.setSelectedTab(balance);
        balanceTab(wallets, dates, balances, deposits, sumOfDeposits);
    }

    private void fireTab(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits) {
        content.removeAll();
        content.add(new FirePathView(currencyConverter, wallets, balances, sumOfDeposits, dates));
    }

    private void strategiesTab() {
        content.removeAll();
        content.add(new StrategyDashboardView(strategies, recommendationService));
    }

    private void earningsTab(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits) {
        content.removeAll();
        content.add(new WalletsEarningsView(wallets, dates, balances, deposits, sumOfDeposits, aggregationPeriodSelector, fromDateFilter, toDateFilter));
    }

    private void balanceTab(List<Wallet> wallets, Map<UUID, List<String>> dates, Map<UUID, List<BigDecimal>> balances, Map<UUID, List<BigDecimal>> deposits, Map<UUID, List<BigDecimal>> sumOfDeposits) {
        content.removeAll();
        content.add(new WalletsBalanceView(wallets, dates, balances, deposits, sumOfDeposits, aggregationPeriodSelector, fromDateFilter, toDateFilter));
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

    private BervanComboBox<String> createCurrencySelector() {
        BervanComboBox<String> monthsDropdown = new BervanComboBox<>("Select Currency", false);
        monthsDropdown.setItems("PLN", "EUR", "USD");
        monthsDropdown.setValue("PLN");
        monthsDropdown.setWidth("300px");
        return monthsDropdown;
    }

    private BervanDatePicker createDateFilter(String label) {
        BervanDatePicker bervanDatePicker = new BervanDatePicker(label, null);
        bervanDatePicker.setWidth("300px");
        return bervanDatePicker;
    }
}