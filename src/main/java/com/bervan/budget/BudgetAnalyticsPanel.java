package com.bervan.budget;

import com.bervan.investtrack.service.BudgetChartDataService;
import com.bervan.investtrack.view.dashboards.BudgetIncomeExpenseChart;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

public class BudgetAnalyticsPanel extends VerticalLayout {

    private static final List<String> PALETTE = Arrays.asList(
            "rgba(239, 68,  68,  0.85)",
            "rgba(245, 158, 11,  0.85)",
            "rgba(99,  102, 241, 0.85)",
            "rgba(34,  211, 238, 0.85)",
            "rgba(16,  185, 129, 0.85)",
            "rgba(139, 92,  246, 0.85)",
            "rgba(236, 72,  153, 0.85)",
            "rgba(59,  130, 246, 0.85)",
            "rgba(168, 162, 158, 0.85)",
            "rgba(251, 191, 36,  0.85)"
    );
    private static final String OTHER_COLOR = "rgba(156, 163, 175, 0.85)";

    private final BudgetChartDataService chartDataService;
    private final DatePicker fromDate = new DatePicker("From");
    private final DatePicker toDate = new DatePicker("To");
    private final CheckboxGroup<String> categoryFilter = new CheckboxGroup<>();
    private final Div chartContainer = new Div();
    private final Div rankingContainer = new Div();
    private final Div avgPieContainer = new Div();
    private int avgPieYear = LocalDate.now().getYear();

    public BudgetAnalyticsPanel(BudgetChartDataService chartDataService) {
        this.chartDataService = chartDataService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        getStyle()
                .set("border", "1px solid var(--bervan-border-color, rgba(255,255,255,0.1))")
                .set("border-radius", "12px")
                .set("margin-bottom", "16px");

        LocalDate now = LocalDate.now();
        fromDate.setValue(now.minusMonths(11).withDayOfMonth(1));
        toDate.setValue(now);

        categoryFilter.setLabel("Categories");

        Button selectAll = new Button("Select All", e -> categoryFilter.setValue(new HashSet<>(categoryFilter.getGenericDataView().getItems().toList())));
        selectAll.addClassName("glass-btn");

        Button deselectAll = new Button("Deselect All", e -> categoryFilter.clear());
        deselectAll.addClassName("glass-btn");

        Button applyButton = new Button("Apply", e -> refresh());
        applyButton.addClassName("glass-btn");
        applyButton.addClassName("glass-btn-primary");

        fromDate.addValueChangeListener(e -> reloadCategories());
        toDate.addValueChangeListener(e -> reloadCategories());

        HorizontalLayout controlsRow = new HorizontalLayout(fromDate, toDate, applyButton);
        controlsRow.setAlignItems(Alignment.END);

        HorizontalLayout categoryButtons = new HorizontalLayout(selectAll, deselectAll);
        categoryButtons.setSpacing(true);

        chartContainer.setWidthFull();
        rankingContainer.setWidthFull();
        avgPieContainer.setWidthFull();

        add(new H4("Budget Analytics"), controlsRow, categoryButtons, categoryFilter,
                chartContainer, new Hr(), rankingContainer,
                new Hr(), buildAvgPieSection());

        reloadCategories();
        refresh();
        refreshAvgPie();
    }

    private void reloadCategories() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null || from.isAfter(to)) return;

        Set<String> allCategories = chartDataService.getAllCategories(from, to);
        Set<String> previousSelection = new HashSet<>(categoryFilter.getValue());
        categoryFilter.setItems(allCategories);

        if (previousSelection.isEmpty()) {
            categoryFilter.setValue(allCategories);
        } else {
            Set<String> newSelection = new HashSet<>(previousSelection);
            newSelection.retainAll(allCategories);
            categoryFilter.setValue(newSelection.isEmpty() ? allCategories : newSelection);
        }
    }

    private void refresh() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null || from.isAfter(to)) return;

        Set<String> selected = new HashSet<>(categoryFilter.getValue());
        renderChart(from, to, selected);
        renderRanking(from, to, selected);
    }

    private void renderChart(LocalDate from, LocalDate to, Set<String> categories) {
        chartContainer.removeAll();
        BudgetChartDataService.MonthlyBudgetData data =
                chartDataService.getMonthlyIncomeExpense(from, to, categories);
        BudgetIncomeExpenseChart chart = new BudgetIncomeExpenseChart(data.income(), data.expense());
        chart.setWidth("100%");
        chart.setHeight("400px");
        chartContainer.add(chart);
    }

    private void renderRanking(LocalDate from, LocalDate to, Set<String> categories) {
        rankingContainer.removeAll();
        BudgetChartDataService.CategoryRankingData ranking =
                chartDataService.getCategoryRanking(from, to, categories);

        HorizontalLayout columns = new HorizontalLayout(
                buildRankingColumn("Top Expenses", ranking.topExpenses(), false),
                buildRankingColumn("Top Income", ranking.topIncome(), true)
        );
        columns.setWidthFull();
        columns.setFlexGrow(1, columns.getComponentAt(0), columns.getComponentAt(1));
        rankingContainer.add(columns);
    }

    private VerticalLayout buildAvgPieSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);
        section.setWidthFull();

        H4 title = new H4("Average Monthly Expenses by Category");
        title.getStyle().set("margin-bottom", "4px");

        int currentYear = LocalDate.now().getYear();

        Button btnCurrent = new Button(String.valueOf(currentYear));
        btnCurrent.addClassName("glass-btn");
        btnCurrent.addClassName("glass-btn-primary");

        Button btnPrev = new Button(String.valueOf(currentYear - 1));
        btnPrev.addClassName("glass-btn");

        btnCurrent.addClickListener(e -> {
            avgPieYear = currentYear;
            btnCurrent.addClassName("glass-btn-primary");
            btnPrev.removeClassName("glass-btn-primary");
            refreshAvgPie();
        });

        btnPrev.addClickListener(e -> {
            avgPieYear = currentYear - 1;
            btnPrev.addClassName("glass-btn-primary");
            btnCurrent.removeClassName("glass-btn-primary");
            refreshAvgPie();
        });

        HorizontalLayout yearButtons = new HorizontalLayout(btnCurrent, btnPrev);
        yearButtons.setSpacing(true);

        section.add(title, yearButtons, avgPieContainer);
        return section;
    }

    private void refreshAvgPie() {
        avgPieContainer.removeAll();
        Map<String, BigDecimal> rawData = chartDataService.getAverageCategoryExpenses(avgPieYear);
        if (rawData.isEmpty()) {
            avgPieContainer.add(new Span("No expense data for " + avgPieYear));
            return;
        }

        BigDecimal total = rawData.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group items under 1% of total into "Other"
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        BigDecimal otherSum = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : rawData.entrySet()) {
            int pct = total.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            if (pct < 1) {
                otherSum = otherSum.add(entry.getValue());
            } else {
                grouped.put(entry.getKey(), entry.getValue());
            }
        }
        if (otherSum.compareTo(BigDecimal.ZERO) > 0) {
            grouped.put("Other", otherSum);
        }

        List<String> keys = new ArrayList<>(grouped.keySet());
        List<String> colors = generateColors(keys.size());
        if (grouped.containsKey("Other")) {
            colors.set(keys.size() - 1, OTHER_COLOR);
        }

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(Alignment.START);
        row.getStyle().set("flex-wrap", "wrap");

        BudgetCategoryAvgPieChart pie = new BudgetCategoryAvgPieChart(grouped, colors);
        pie.setWidth("380px");
        pie.setHeight("380px");
        pie.getStyle().set("flex-shrink", "0");

        VerticalLayout legend = buildAvgLegend(grouped, colors, total);
        legend.getStyle()
                .set("max-height", "380px")
                .set("overflow-y", "auto")
                .set("flex", "1")
                .set("min-width", "260px");

        row.add(pie, legend);
        avgPieContainer.add(row);
    }

    private List<String> generateColors(int count) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(PALETTE.get(i % PALETTE.size()));
        return result;
    }

    private VerticalLayout buildAvgLegend(Map<String, BigDecimal> data, List<String> colors, BigDecimal total) {
        VerticalLayout col = new VerticalLayout();
        col.setPadding(false);
        col.setSpacing(false);

        List<String> keys = new ArrayList<>(data.keySet());

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            BigDecimal value = data.get(key);
            String color = colors.get(i);

            int pct = total.compareTo(BigDecimal.ZERO) > 0
                    ? value.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue()
                    : 0;

            Div colorDot = new Div();
            colorDot.getStyle()
                    .set("width", "12px").set("height", "12px")
                    .set("border-radius", "3px")
                    .set("background", color)
                    .set("flex-shrink", "0");

            Span name = new Span(key);
            name.getStyle().set("flex", "1").set("font-size", "13px");

            Span amount = new Span(String.format("%,.2f  (%d%%)", value, pct));
            amount.getStyle().set("font-weight", "600").set("font-size", "13px")
                    .set("color", "rgba(239,68,68,0.9)").set("min-width", "130px").set("text-align", "right");

            HorizontalLayout legendRow = new HorizontalLayout(colorDot, name, amount);
            legendRow.setWidthFull();
            legendRow.setAlignItems(Alignment.CENTER);
            legendRow.getStyle()
                    .set("padding", "3px 0")
                    .set("border-bottom", "1px solid var(--bervan-border-color, rgba(255,255,255,0.05))");
            col.add(legendRow);
        }

        return col;
    }

    private VerticalLayout buildRankingColumn(String title, List<BudgetChartDataService.CategoryTotal> items, boolean income) {
        VerticalLayout col = new VerticalLayout();
        col.setPadding(true);
        col.setSpacing(false);
        col.setWidthFull();

        String color = income ? "rgba(16,185,129,1)" : "rgba(239,68,68,1)";
        String barColor = income ? "rgba(16,185,129,0.6)" : "rgba(239,68,68,0.6)";

        H4 heading = new H4(title);
        heading.getStyle().set("color", color).set("margin-bottom", "8px");
        col.add(heading);

        if (items.isEmpty()) {
            col.add(new Span("No data"));
            return col;
        }

        BigDecimal max = items.get(0).total();

        for (int i = 0; i < items.size(); i++) {
            BudgetChartDataService.CategoryTotal item = items.get(i);

            Span rank = new Span((i + 1) + ".");
            rank.getStyle().set("min-width", "28px").set("color", "var(--bervan-text-secondary, rgba(255,255,255,0.5))").set("font-size", "13px");

            Span name = new Span(item.category());
            name.getStyle().set("flex", "1").set("font-weight", "500").set("font-size", "14px");

            int pct = max.compareTo(BigDecimal.ZERO) > 0
                    ? item.total().multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            Div bar = new Div();
            bar.getStyle().set("height", "6px").set("width", pct + "%").set("min-width", "4px")
                    .set("background", barColor).set("border-radius", "3px");
            Div track = new Div(bar);
            track.getStyle().set("flex", "2").set("background", "rgba(255,255,255,0.05)")
                    .set("border-radius", "3px").set("display", "flex").set("align-items", "center");

            Span amount = new Span(String.format("%,.2f", item.total()));
            amount.getStyle().set("min-width", "110px").set("text-align", "right")
                    .set("font-weight", "600").set("font-size", "14px").set("color", color);

            HorizontalLayout row = new HorizontalLayout(rank, name, track, amount);
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);
            row.getStyle().set("padding", "4px 0").set("border-bottom", "1px solid var(--bervan-border-color, rgba(255,255,255,0.05))");
            col.add(row);
        }

        return col;
    }
}
