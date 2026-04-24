package com.bervan.investtrack.view.dashboards;

import com.bervan.common.component.BervanButton;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.BudgetChartDataService;
import com.bervan.investtrack.service.CurrencyConverter;
import com.bervan.investtrack.service.InvestmentCalculationService;
import com.bervan.investtrack.service.WalletService;
import com.bervan.investtrack.view.charts.AssetAllocationChart;
import com.bervan.investtrack.view.charts.WalletBalanceSumOfDepositsCharts;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@CssImport("./invest-track-dashboard.css")
public class MainDashboardView extends VerticalLayout {

    private final WalletService walletService;
    private final CurrencyConverter currencyConverter;
    private final InvestmentCalculationService calculationService;
    private final BudgetChartDataService budgetChartDataService;

    private final Div chartsContainer = new Div();
    private final HorizontalLayout quickFiltersLayout = new HorizontalLayout();
    private final NumberFormat currencyFormat;

    private List<Wallet> allWallets;
    private List<Wallet> investmentWallets;
    private List<Wallet> savingsWallets;

    private LocalDate filterStartDate;
    private LocalDate filterEndDate;
    private String activeFilter = "ALL";

    public MainDashboardView(WalletService walletService,
                             CurrencyConverter currencyConverter,
                             InvestmentCalculationService calculationService,
                             BudgetChartDataService budgetChartDataService) {
        this.walletService = walletService;
        this.currencyConverter = currencyConverter;
        this.calculationService = calculationService;
        this.budgetChartDataService = budgetChartDataService;

        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
        currencyFormat.setMaximumFractionDigits(0);

        addClassName("invest-dashboard");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        loadData();
        initializeFilters();
        buildDashboard();
    }

    private void loadData() {
        try {
            Set<Wallet> loaded = walletService.load(Pageable.ofSize(100));
            this.allWallets = loaded.stream()
                    .sorted(Comparator.comparing(Wallet::getReturnRate).reversed())
                    .toList();
            this.investmentWallets = allWallets.stream().filter(Wallet::isInvestmentLike).toList();
            this.savingsWallets = allWallets.stream().filter(w -> !w.isInvestmentLike()).toList();

            Optional<LocalDate> minDate = allWallets.stream()
                    .flatMap(w -> w.getSnapshots().stream())
                    .map(WalletSnapshot::getSnapshotDate)
                    .min(LocalDate::compareTo);
            Optional<LocalDate> maxDate = allWallets.stream()
                    .flatMap(w -> w.getSnapshots().stream())
                    .map(WalletSnapshot::getSnapshotDate)
                    .max(LocalDate::compareTo);

            filterStartDate = minDate.orElse(LocalDate.now().minusYears(1));
            filterEndDate = maxDate.orElse(LocalDate.now());

        } catch (Exception e) {
            this.allWallets = Collections.emptyList();
            this.investmentWallets = Collections.emptyList();
            this.savingsWallets = Collections.emptyList();
            filterStartDate = LocalDate.now().minusYears(1);
            filterEndDate = LocalDate.now();
        }
    }

    private void initializeFilters() {
        quickFiltersLayout.addClassName("invest-quick-filters");

        String[] filters = {"MTD", "YTD", "1Y", "3Y", "5Y", "ALL"};
        for (String filter : filters) {
            BervanButton btn = new BervanButton(filter, e -> applyQuickFilter(filter));
            btn.addClassName("invest-quick-filter-btn");
            if (filter.equals(activeFilter)) {
                btn.addClassName("active");
            }
            quickFiltersLayout.add(btn);
        }
    }

    private void applyQuickFilter(String filter) {
        this.activeFilter = filter;

        LocalDate now = LocalDate.now();
        LocalDate start = switch (filter) {
            case "MTD" -> now.withDayOfMonth(1);
            case "YTD" -> now.withDayOfYear(1);
            case "1Y" -> now.minusYears(1);
            case "3Y" -> now.minusYears(3);
            case "5Y" -> now.minusYears(5);
            default -> allWallets.stream()
                    .flatMap(w -> w.getSnapshots().stream())
                    .map(WalletSnapshot::getSnapshotDate)
                    .min(LocalDate::compareTo)
                    .orElse(now.minusYears(10));
        };

        filterStartDate = start;
        filterEndDate = now;

        quickFiltersLayout.getChildren().forEach(c -> {
            if (c instanceof BervanButton btn) {
                btn.removeClassName("active");
                if (btn.getText().equals(filter)) {
                    btn.addClassName("active");
                }
            }
        });

        refreshCharts();
    }

    private void buildDashboard() {
        add(buildKpiSection());
        add(quickFiltersLayout);

        chartsContainer.addClassName("invest-chart-grid");
        refreshCharts();
        add(chartsContainer);
    }

    private VerticalLayout buildKpiSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();

        // --- Investments row ---
        H4 investLabel = new H4("Investments");
        investLabel.addClassName("kpi-section-label");
        section.add(investLabel);
        section.add(buildInvestmentKpiRow());

        // --- Savings row ---
        H4 savingsLabel = new H4("Savings");
        savingsLabel.addClassName("kpi-section-label");
        section.add(savingsLabel);
        section.add(buildSavingsKpiRow());

        return section;
    }

    private HorizontalLayout buildInvestmentKpiRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("invest-kpi-row");
        row.setWidthFull();

        BigDecimal investBalance = BigDecimal.ZERO;
        BigDecimal investDeposits = BigDecimal.ZERO;

        for (Wallet w : investmentWallets) {
            investBalance = investBalance.add(toPLN(w.getCurrentValue(), w.getCurrency()));
            investDeposits = investDeposits.add(
                    toPLN(w.getTotalDeposits().subtract(w.getTotalWithdrawals()), w.getCurrency()));
        }

        BigDecimal investReturn = investBalance.subtract(investDeposits);
        BigDecimal investReturnRate = investDeposits.compareTo(BigDecimal.ZERO) > 0
                ? investReturn.divide(investDeposits, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        long months = calculateMonthsSpan(investmentWallets);
        double years = Math.max(months / 12.0, 0.1);
        BigDecimal cagr = calculationService.calculateCAGR(investDeposits, investBalance, years)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> timeSeries =
                calculationService.buildAggregatedTimeSeries(investmentWallets, this::convertToPLN);
        BigDecimal twr = calculationService.calculateAggregatedTWR(timeSeries)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        row.add(createKpiCard("Investment Balance", currencyFormat.format(investBalance), VaadinIcon.WALLET, null));
        row.add(createKpiCard("Net Deposits", currencyFormat.format(investDeposits), VaadinIcon.PIGGY_BANK, null));
        row.add(createKpiCard("Total Return", currencyFormat.format(investReturn),
                investReturn.signum() >= 0 ? VaadinIcon.TRENDING_UP : VaadinIcon.TRENDING_DOWN,
                investReturn.signum() >= 0 ? "positive" : "negative"));
        row.add(createKpiCard("Return Rate", investReturnRate.setScale(2, RoundingMode.HALF_UP) + "%",
                VaadinIcon.CHART, investReturnRate.signum() >= 0 ? "positive" : "negative"));
        row.add(createKpiCard("CAGR", cagr + "%", VaadinIcon.LINE_CHART,
                cagr.signum() >= 0 ? "positive" : "negative",
                "CAGR (Compound Annual Growth Rate)\nAnnual return accounting for compound interest.\nFormula: (Ending Value / Starting Value)^(1/years) - 1\nCalculated for investment wallets only."));
        row.add(createKpiCard("TWR", twr + "%", VaadinIcon.CHART_LINE,
                twr.signum() >= 0 ? "positive" : "negative",
                "TWR (Time-Weighted Return)\nEliminates the effect of contributions/withdrawals.\nFormula: Π(1 + Ri) - 1\nCalculated for investment wallets only."));

        return row;
    }

    private HorizontalLayout buildSavingsKpiRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("invest-kpi-row");
        row.setWidthFull();

        BigDecimal savingsBalance = BigDecimal.ZERO;
        BigDecimal savingsDeposits = BigDecimal.ZERO;
        BigDecimal investBalance = BigDecimal.ZERO;
        BigDecimal investDeposits = BigDecimal.ZERO;

        for (Wallet w : savingsWallets) {
            savingsBalance = savingsBalance.add(toPLN(w.getCurrentValue(), w.getCurrency()));
            savingsDeposits = savingsDeposits.add(
                    toPLN(w.getTotalDeposits().subtract(w.getTotalWithdrawals()), w.getCurrency()));
        }
        for (Wallet w : investmentWallets) {
            investBalance = investBalance.add(toPLN(w.getCurrentValue(), w.getCurrency()));
            investDeposits = investDeposits.add(
                    toPLN(w.getTotalDeposits().subtract(w.getTotalWithdrawals()), w.getCurrency()));
        }

        BigDecimal netWorth = savingsBalance.add(investBalance);
        BigDecimal savingsGrowth = savingsBalance.subtract(savingsDeposits);

        row.add(createKpiCard("Savings Balance", currencyFormat.format(savingsBalance), VaadinIcon.PIGGY_BANK, null));
        row.add(createKpiCard("Savings Growth", currencyFormat.format(savingsGrowth),
                savingsGrowth.signum() >= 0 ? VaadinIcon.TRENDING_UP : VaadinIcon.TRENDING_DOWN,
                savingsGrowth.signum() >= 0 ? "positive" : "negative"));
        row.add(createKpiCard("Net Worth", currencyFormat.format(netWorth), VaadinIcon.MONEY, null,
                "Total portfolio value: all investment + savings wallets combined."));

        return row;
    }

    private long calculateMonthsSpan(List<Wallet> wallets) {
        Optional<LocalDate> minDate = wallets.stream()
                .flatMap(w -> w.getSnapshots().stream())
                .map(WalletSnapshot::getSnapshotDate)
                .min(LocalDate::compareTo);
        Optional<LocalDate> maxDate = wallets.stream()
                .flatMap(w -> w.getSnapshots().stream())
                .map(WalletSnapshot::getSnapshotDate)
                .max(LocalDate::compareTo);

        if (minDate.isPresent() && maxDate.isPresent()) {
            return ChronoUnit.MONTHS.between(minDate.get(), maxDate.get()) + 1;
        }
        return 1;
    }

    private void refreshCharts() {
        chartsContainer.removeAll();

        // Chart 1: Investment Balance vs Deposits
        chartsContainer.add(createChartCard("Investment Portfolio Balance vs Deposits", createBalanceChart(investmentWallets)));

        // Chart 2: Net Worth (all wallets)
        chartsContainer.add(createChartCard("Net Worth (Investments + Savings)", createBalanceChart(allWallets)));

        // Chart 3: Budget Income/Expense
        chartsContainer.add(createChartCard("Monthly Income vs Expense", createBudgetChart()));

        // Chart 4: Asset Allocation by type
        chartsContainer.add(createChartCard("Asset Allocation by Wallet", createAllocationChart()));

        // Chart 5: Monthly Returns Heatmap (investments only)
        chartsContainer.add(createChartCard("Monthly Returns Heatmap (Investments)", createHeatmap(investmentWallets)));
    }

    private Div createChartCard(String title, com.vaadin.flow.component.Component chart) {
        Div card = new Div();
        card.addClassName("invest-chart-card");
        card.addClassName("invest-fade-in");

        H3 titleEl = new H3(title);
        titleEl.addClassName("chart-title");

        Div chartWrapper = new Div(chart);
        chartWrapper.setWidthFull();
        chartWrapper.setHeight("300px");

        card.add(titleEl, chartWrapper);
        return card;
    }

    private Div createKpiCard(String title, String value, VaadinIcon iconType, String trendClass) {
        return createKpiCard(title, value, iconType, trendClass, null);
    }

    private Div createKpiCard(String title, String value, VaadinIcon iconType, String trendClass, String tooltip) {
        Div card = new Div();
        card.addClassName("invest-kpi-card");
        card.addClassName("invest-fade-in");

        Icon icon = iconType.create();
        icon.addClassName("kpi-icon");

        H3 titleEl = new H3(title);
        titleEl.addClassName("kpi-title");

        Span valueEl = new Span(value);
        valueEl.addClassName("kpi-value");
        if (trendClass != null) {
            valueEl.addClassName("invest-text-" + (trendClass.equals("positive") ? "success" : "danger"));
        }

        card.add(icon, titleEl, valueEl);

        if (tooltip != null && !tooltip.isEmpty()) {
            card.getElement().setAttribute("title", tooltip);
            card.addClassName("kpi-has-tooltip");
        }

        return card;
    }

    private BigDecimal toPLN(BigDecimal amount, String currency) {
        if (amount == null) return BigDecimal.ZERO;
        return currencyConverter.convert(amount, CurrencyConverter.Currency.of(currency), CurrencyConverter.Currency.PLN);
    }

    private BigDecimal convertToPLN(BigDecimal amount, String currency) {
        return toPLN(amount, currency);
    }

    private com.vaadin.flow.component.Component createBalanceChart(List<Wallet> wallets) {
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> timeSeries =
                calculationService.buildAggregatedTimeSeries(wallets, this::convertToPLN);

        if (timeSeries.isEmpty()) {
            return new Span("No data for selected period");
        }

        List<String> dates = new ArrayList<>();
        List<BigDecimal> balances = new ArrayList<>();
        List<BigDecimal> cumDeposits = new ArrayList<>();
        BigDecimal runningDeposits = BigDecimal.ZERO;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (Map.Entry<LocalDate, InvestmentCalculationService.PortfolioPoint> entry : timeSeries.entrySet()) {
            LocalDate date = entry.getKey();
            if (date.isBefore(filterStartDate) || date.isAfter(filterEndDate)) {
                runningDeposits = runningDeposits.add(entry.getValue().cashFlow());
                continue;
            }
            dates.add(date.format(formatter));
            balances.add(entry.getValue().balance());
            runningDeposits = runningDeposits.add(entry.getValue().cashFlow());
            cumDeposits.add(runningDeposits);
        }

        if (dates.isEmpty()) {
            return new Span("No data for selected period");
        }

        WalletBalanceSumOfDepositsCharts chart = new WalletBalanceSumOfDepositsCharts(dates, balances, cumDeposits);
        chart.setWidth("100%");
        chart.setHeight("280px");
        return chart;
    }

    private com.vaadin.flow.component.Component createBudgetChart() {
        try {
            BudgetChartDataService.MonthlyBudgetData data =
                    budgetChartDataService.getMonthlyIncomeExpense(filterStartDate, filterEndDate);

            if (data.income().isEmpty()) {
                return new Span("No budget data for selected period");
            }

            BudgetIncomeExpenseChart chart = new BudgetIncomeExpenseChart(data.income(), data.expense());
            chart.setWidth("100%");
            chart.setHeight("280px");
            return chart;
        } catch (Exception e) {
            return new Span("Error loading budget data");
        }
    }

    private com.vaadin.flow.component.Component createAllocationChart() {
        if (allWallets.isEmpty()) {
            return new Span("No wallets found");
        }

        AssetAllocationChart chart = new AssetAllocationChart(
                allWallets, currencyConverter, AssetAllocationChart.GroupBy.WALLET);
        chart.setWidth("100%");
        chart.setHeight("280px");
        return chart;
    }

    private com.vaadin.flow.component.Component createHeatmap(List<Wallet> wallets) {
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> timeSeries =
                calculationService.buildAggregatedTimeSeries(wallets, this::convertToPLN);

        if (timeSeries.size() < 2) {
            return new Span("Not enough data for heatmap");
        }

        Map<String, BigDecimal> monthlyReturns = new LinkedHashMap<>();
        List<LocalDate> dates = new ArrayList<>(timeSeries.keySet());

        for (int i = 1; i < dates.size(); i++) {
            LocalDate prevDate = dates.get(i - 1);
            LocalDate currDate = dates.get(i);

            InvestmentCalculationService.PortfolioPoint prev = timeSeries.get(prevDate);
            InvestmentCalculationService.PortfolioPoint curr = timeSeries.get(currDate);

            BigDecimal beginValue = prev.balance().add(curr.cashFlow());
            if (beginValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal returnPct = curr.balance().subtract(beginValue)
                        .divide(beginValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                String key = String.format("%d-%02d", currDate.getYear(), currDate.getMonthValue());
                monthlyReturns.put(key, returnPct);
            }
        }

        if (monthlyReturns.isEmpty()) {
            return new Span("No returns data available");
        }

        return new MonthlyReturnsHeatmap(monthlyReturns);
    }
}
