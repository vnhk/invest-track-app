package com.bervan.budget;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.math.BigDecimal;

public class BudgetGridView extends VerticalLayout {

    public BudgetGridView(BudgetService service) {

        TreeGrid<BudgetRow> grid = new TreeGrid<>();
        grid.setWidthFull();
        grid.setHeightFull();

        // Checkbox + category name
        grid.addHierarchyColumn(BudgetRow::getName)
                .setHeader("Category")
                .setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(this::progress))
                .setHeader("Status")
                .setWidth("180px");

        grid.addColumn(row -> money(row.getAssigned()))
                .setHeader("Assigned")
                .setTextAlign(ColumnTextAlign.END);

        grid.addColumn(row -> money(row.getActivity()))
                .setHeader("Activity")
                .setTextAlign(ColumnTextAlign.END);

        grid.addColumn(row -> money(row.getAvailable()))
                .setHeader("Available")
                .setTextAlign(ColumnTextAlign.END);

        TreeData<BudgetRow> data = service.loadTreeData();
        TreeDataProvider<BudgetRow> provider = new TreeDataProvider<>(data);
        grid.setDataProvider(provider);

        grid.addComponentColumn(row -> {
            Checkbox cb = new Checkbox();
            cb.setEnabled(!row.isGroup());
            return cb;
        }).setWidth("50px");

        add(grid);
        setSizeFull();
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return String.format("$%,.2f", value);
    }

    private Component progress(BudgetRow row) {
        if (row.isGroup()) {
            return new Span(""); // groups don't have progress
        }

        ProgressBar bar = new ProgressBar();
        bar.setMin(0);
        bar.setMax(row.getAssigned().doubleValue());
        bar.setValue(
                row.getAssigned().subtract(row.getAvailable()).doubleValue()
        );
        bar.addClassName("budget-progress");

        return bar;
    }
}
