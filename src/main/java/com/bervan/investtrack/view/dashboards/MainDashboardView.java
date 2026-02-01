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

/**
 * Main unified dashboard with KPI cards, quick filters, and multiple charts
 */
@CssImport("./invest-track-dashboard.css")
public class MainDashboardView extends VerticalLayout {

    private final WalletService walletService;
    private final CurrencyConverter currencyConverter;
    private final InvestmentCalculationService calculationService;
    private final BudgetChartDataService budgetChartDataService;

    private final Div chartsContainer = new Div();
    private final HorizontalLayout quickFiltersLayout = new HorizontalLayout();
    private final NumberFormat currencyFormat;
    private List<Wallet> wallets;
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
            this.wallets = loaded.stream()
                    .sorted(Comparator.comparing(Wallet::getReturnRate).reversed())
                    .toList();

            // Find date range
            Optional<LocalDate> minDate = wallets.stream()
                    .flatMap(w -> w.getSnapshots().stream())
                    .map(WalletSnapshot::getSnapshotDate)
                    .min(LocalDate::compareTo);

            Optional<LocalDate> maxDate = wallets.stream()
                    .flatMap(w -> w.getSnapshots().stream())
                    .map(WalletSnapshot::getSnapshotDate)
                    .max(LocalDate::compareTo);

            filterStartDate = minDate.orElse(LocalDate.now().minusYears(1));
            filterEndDate = maxDate.orElse(LocalDate.now());

        } catch (Exception e) {
            this.wallets = Collections.emptyList();
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
            default -> wallets.stream()
                    .flatMap(w -> w.getSnapshots().stream())
                    .map(WalletSnapshot::getSnapshotDate)
                    .min(LocalDate::compareTo)
                    .orElse(now.minusYears(10));
        };

        filterStartDate = start;
        filterEndDate = now;

        // Update button styles
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
        // KPI Cards
        HorizontalLayout kpiRow = buildKpiCards();
        add(kpiRow);

        // Quick Filters
        add(quickFiltersLayout);

        // Charts Grid
        chartsContainer.addClassName("invest-chart-grid");
        refreshCharts();
        add(chartsContainer);
    }

    private void refreshCharts() {
        chartsContainer.removeAll();

        // Chart 1: Balance vs Deposits
        Div balanceCard = createChartCard("Portfolio Balance vs Deposits", createBalanceChart());
        chartsContainer.add(balanceCard);

        // Chart 2: Budget Income/Expense
        Div budgetCard = createChartCard("Monthly Income vs Expense",
                createBudgetChart());
        chartsContainer.add(budgetCard);

        // Chart 3: Asset Allocation
        Div allocationCard = createChartCard("Asset Allocation by Wallet",
                createAllocationChart());
        chartsContainer.add(allocationCard);

        // Chart 4: Monthly Returns Heatmap
        Div heatmapCard = createChartCard("Monthly Returns Heatmap",
                createHeatmap());
        chartsContainer.add(heatmapCard);
    }

    private HorizontalLayout buildKpiCards() {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("invest-kpi-row");
        row.setWidthFull();

        // Calculate metrics
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalDeposits = BigDecimal.ZERO;

        for (Wallet wallet : wallets) {
            BigDecimal valuePLN = currencyConverter.convert(
                    wallet.getCurrentValue(),
                    CurrencyConverter.Currency.of(wallet.getCurrency()),
                    CurrencyConverter.Currency.PLN
            );
            totalBalance = totalBalance.add(valuePLN);

            BigDecimal depositsPLN = currencyConverter.convert(
                    wallet.getTotalDeposits().subtract(wallet.getTotalWithdrawals()),
                    CurrencyConverter.Currency.of(wallet.getCurrency()),
                    CurrencyConverter.Currency.PLN
            );
            totalDeposits = totalDeposits.add(depositsPLN);
        }

        BigDecimal totalReturn = totalBalance.subtract(totalDeposits);
        BigDecimal returnRate = totalDeposits.compareTo(BigDecimal.ZERO) > 0 ?
                totalReturn.divide(totalDeposits, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

        // Calculate CAGR
        long months = calculateMonthsSpan();
        double years = Math.max(months / 12.0, 0.1); // Avoid division by zero
        BigDecimal cagr = calculationService.calculateCAGR(totalDeposits, totalBalance, years);
        BigDecimal cagrPct = cagr.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        // Calculate TWR using aggregated time series with proper carry-forward
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> timeSeries =
                calculationService.buildAggregatedTimeSeries(wallets, this::convertToPLN);
        BigDecimal twr = calculationService.calculateAggregatedTWR(timeSeries);
        BigDecimal twrPct = twr.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        // Create cards
        row.add(createKpiCard("Total Balance", currencyFormat.format(totalBalance),
                VaadinIcon.WALLET, null));
        row.add(createKpiCard("Net Deposits", currencyFormat.format(totalDeposits),
                VaadinIcon.PIGGY_BANK, null));
        row.add(createKpiCard("Total Return", currencyFormat.format(totalReturn),
                totalReturn.compareTo(BigDecimal.ZERO) >= 0 ? VaadinIcon.TRENDING_UP : VaadinIcon.TRENDING_DOWN,
                totalReturn.compareTo(BigDecimal.ZERO) >= 0 ? "positive" : "negative"));
        row.add(createKpiCard("Return Rate", returnRate.setScale(2, RoundingMode.HALF_UP) + "%",
                VaadinIcon.CHART, returnRate.compareTo(BigDecimal.ZERO) >= 0 ? "positive" : "negative"));
        row.add(createKpiCard("CAGR", cagrPct + "%",
                VaadinIcon.LINE_CHART, cagrPct.compareTo(BigDecimal.ZERO) >= 0 ? "positive" : "negative",
                "CAGR (Compound Annual Growth Rate)\n" +
                        "Annual return accounting for compound interest.\n" +
                        "Formula: (Ending Value / Starting Value)^(1/years) - 1\n" +
                        "Shows the average annual growth of the portfolio."));
        row.add(createKpiCard("TWR", twrPct + "%",
                VaadinIcon.CHART_LINE, twrPct.compareTo(BigDecimal.ZERO) >= 0 ? "positive" : "negative",
                "TWR (Time-Weighted Return)\n" +
                        "Time-weighted return – eliminates the effect of contributions/withdrawals.\n" +
                        "Formula: Π(1 + Ri) - 1, where Ri is the return in period i\n" +
                        "Shows the actual efficiency of the investment."));

        return row;
    }

    private long calculateMonthsSpan() {
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

        // Add tooltip if provided
        if (tooltip != null && !tooltip.isEmpty()) {
            card.getElement().setAttribute("title", tooltip);
            card.addClassName("kpi-has-tooltip");
        }

        return card;
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

    private BigDecimal convertToPLN(BigDecimal amount, String currency) {
        if (amount == null) return BigDecimal.ZERO;
        return currencyConverter.convert(amount,
                CurrencyConverter.Currency.of(currency),
                CurrencyConverter.Currency.PLN);
    }

    private com.vaadin.flow.component.Component createBalanceChart() {
        // Use aggregated time series with proper carry-forward
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> timeSeries =
                calculationService.buildAggregatedTimeSeries(wallets, this::convertToPLN);

        if (timeSeries.isEmpty()) {
            return new Span("No data for selected period");
        }

        // Filter by date range and build output lists
        List<String> dates = new ArrayList<>();
        List<BigDecimal> balances = new ArrayList<>();
        List<BigDecimal> cumDeposits = new ArrayList<>();
        BigDecimal runningDeposits = BigDecimal.ZERO;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (Map.Entry<LocalDate, InvestmentCalculationService.PortfolioPoint> entry : timeSeries.entrySet()) {
            LocalDate date = entry.getKey();

            // Apply date filter
            if (date.isBefore(filterStartDate) || date.isAfter(filterEndDate)) {
                // Still accumulate deposits for proper cumulative calculation
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
        if (wallets.isEmpty()) {
            return new Span("No wallets found");
        }

        AssetAllocationChart chart = new AssetAllocationChart(
                wallets, currencyConverter, AssetAllocationChart.GroupBy.WALLET);
        chart.setWidth("100%");
        chart.setHeight("280px");
        return chart;
    }

    private com.vaadin.flow.component.Component createHeatmap() {
        // Use aggregated time series for proper monthly returns
        Map<LocalDate, InvestmentCalculationService.PortfolioPoint> timeSeries =
                calculationService.buildAggregatedTimeSeries(wallets, this::convertToPLN);

        if (timeSeries.size() < 2) {
            return new Span("Not enough data for heatmap");
        }

        // Calculate monthly returns from aggregated data
        Map<String, BigDecimal> monthlyReturns = new LinkedHashMap<>();
        List<LocalDate> dates = new ArrayList<>(timeSeries.keySet());

        for (int i = 1; i < dates.size(); i++) {
            LocalDate prevDate = dates.get(i - 1);
            LocalDate currDate = dates.get(i);

            InvestmentCalculationService.PortfolioPoint prev = timeSeries.get(prevDate);
            InvestmentCalculationService.PortfolioPoint curr = timeSeries.get(currDate);

            BigDecimal prevBalance = prev.balance();
            BigDecimal currBalance = curr.balance();
            BigDecimal cashFlow = curr.cashFlow();

            // Calculate return adjusted for cash flow
            BigDecimal beginValue = prevBalance.add(cashFlow);
            if (beginValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal returnPct = currBalance.subtract(beginValue)
                        .divide(beginValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                String key = String.format("%d-%02d",
                        currDate.getYear(), currDate.getMonthValue());
                monthlyReturns.put(key, returnPct);
            }
        }

        if (monthlyReturns.isEmpty()) {
            return new Span("No returns data available");
        }

        return new MonthlyReturnsHeatmap(monthlyReturns);
    }
}
