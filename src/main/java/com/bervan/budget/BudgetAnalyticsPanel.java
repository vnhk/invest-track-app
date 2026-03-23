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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BudgetAnalyticsPanel extends VerticalLayout {

    private final BudgetChartDataService chartDataService;
    private final DatePicker fromDate = new DatePicker("From");
    private final DatePicker toDate = new DatePicker("To");
    private final CheckboxGroup<String> categoryFilter = new CheckboxGroup<>();
    private final Div chartContainer = new Div();
    private final Div rankingContainer = new Div();

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

        add(new H4("Budget Analytics"), controlsRow, categoryButtons, categoryFilter,
                chartContainer, new Hr(), rankingContainer);

        reloadCategories();
        refresh();
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
