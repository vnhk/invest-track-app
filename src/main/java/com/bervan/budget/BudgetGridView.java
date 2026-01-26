package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.common.component.*;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractPageView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BudgetGridView extends AbstractPageView {
    private final TreeDataProvider<BudgetRow> provider;
    private final TreeData<BudgetRow> data;
    private final BudgetService service;
    private final BervanViewConfig bervanViewConfig;
    private final ComponentHelper<UUID, BudgetEntry> componentHelper;

    public BudgetGridView(BudgetService service, BervanViewConfig bervanViewConfig, ComponentHelper<UUID, BudgetEntry> componentHelper) {
        this.service = service;
        this.bervanViewConfig = bervanViewConfig;
        this.componentHelper = componentHelper;
        TreeGrid<BudgetRow> grid = new TreeGrid<>();
        grid.setWidthFull();
        grid.setHeightFull();

        grid.addHierarchyColumn(BudgetRow::getName)
                .setVisible(true);

        grid.addColumn(new ComponentRenderer<>(this::name))
                .setAutoWidth(true)
                .setHeader("Name");

        grid.addColumn(new ComponentRenderer<>(this::money))
                .setHeader("Amount")
                .setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(this::paymentMethod))
                .setHeader("Payment Method")
                .setWidth("100px");

        data = service.loadTreeData(LocalDate.of(2020, 1, 1), LocalDate.now());
        provider = new TreeDataProvider<>(data);
        grid.setDataProvider(provider);

        grid.addComponentColumn(row -> {
            Checkbox cb = new Checkbox();
            cb.setEnabled(!row.isGroup());
            return cb;
        }).setWidth("50px");

        add(grid);
        setSizeFull();
    }

    private Component name(BudgetRow row) {
        if (row.getName().equals("+")) {
            if (row.getRowType().equals("DATE_ROW")) {
                BervanButton bervanButton = new BervanButton("+", buttonClickEvent -> {
                    addDateRow();
                });
                return bervanButton;
            } else if (row.getRowType().equals("CATEGORY_ROW")) {
                BervanButton bervanButton = new BervanButton("+", buttonClickEvent -> {
                    addCategoryRow(data.getParent(row));
                });
                bervanButton.getStyle().set("margin-left", "10px");
                return bervanButton;
            } else if (row.getRowType().equals("ITEM_ROW")) {
                BervanButton bervanButton = new BervanButton("+", buttonClickEvent -> {
                    addItemRow(data.getParent(row), data.getParent(data.getParent(row)));
                });
                bervanButton.getStyle().set("margin-left", "20px");
                return bervanButton;
            }
        }
        return new Span(row.getName());
    }

    private void addDateRow() {
        Dialog dialog = new Dialog();
        BervanDatePicker datePicker = new BervanDatePicker("Date", LocalDate.now());
        dialog.add(datePicker);
        dialog.add(new Hr());
        dialog.add(new BervanButton("Add", e -> {
            List<BudgetRow> children = data.getChildren(null);
            String dateStr = datePicker.getValue().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            if (children.stream().anyMatch(c -> c.getName().equals(dateStr))) {
                showPrimaryNotification("Date already exists.");
                dialog.close();
            }
            BudgetRow newDate = service.createDateRow(dateStr);
            data.addItem(null, newDate);
            service.addNewGroupBudgetRow(data, newDate, "CATEGORY_ROW");
            provider.refreshAll();
            showPrimaryNotification("Date added successfully.");
            dialog.close();
        }));
        dialog.open();
    }

    private void addCategoryRow(BudgetRow parent) {
        Dialog dialog = new Dialog();
        BervanTextField category = new BervanTextField("Category");
        dialog.add(category);
        dialog.add(new BervanButton("Add", e -> {
            List<BudgetRow> children = data.getChildren(parent);
            if (children.stream().anyMatch(c -> c.getName().equals(category.getValue()))) {
                showPrimaryNotification("Category already exists.");
                dialog.close();
            }
            BudgetRow newCategory = service.createCategoryRow(category.getValue());
            data.addItem(parent, newCategory);
            service.addNewItemBudgetRow(data, newCategory, "ITEM_ROW");
            provider.refreshAll();
            showPrimaryNotification("Category added successfully.");
            dialog.close();
        }));
        dialog.open();
    }

    private void addItemRow(BudgetRow category, BudgetRow date) {
        Dialog dialog = new Dialog();
        dialog.setWidth("60vw");
        Map<Field, AutoConfigurableField> fieldsHolder = new HashMap<>();
        Map<Field, VerticalLayout> fieldsLayoutHolder = new HashMap<>();
        try {
            VerticalLayout verticalLayout = CommonComponentUtils.buildFormLayout(BudgetEntry.class, null, fieldsHolder, fieldsLayoutHolder, bervanViewConfig);
            dialog.add(verticalLayout);
            dialog.add(new BervanButton("Add", e -> {
                BudgetEntry newBudgetEntry = new BudgetEntry();
                for (Map.Entry<Field, AutoConfigurableField> fieldAutoConfigurableFieldEntry : fieldsHolder.entrySet()) {
                    try {
                        fieldAutoConfigurableFieldEntry.getKey().setAccessible(true);
                        fieldAutoConfigurableFieldEntry.getKey().set(newBudgetEntry, componentHelper.getFieldValueForNewItemDialog(fieldAutoConfigurableFieldEntry));
                        fieldAutoConfigurableFieldEntry.getKey().setAccessible(false);
                    } catch (Exception exception) {
                        throw new RuntimeException("Could not set field value for Budget Entry");
                    }
                }
                BudgetRow newItem = service.createItemRow(newBudgetEntry, date, category);
                data.addItem(category, newItem);
                provider.refreshAll();
                showPrimaryNotification("Item added successfully.");
                dialog.close();
            }));
            dialog.open();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Component money(BudgetRow row) {
        if (row.isGroup()) {
            return new Span(""); // groups don't have it
        }

        if (row.getAmount() == null || row.getCurrency() == null) {
            return new Span("");
        }

        String currency = "";
        if (row.getCurrency().equals("PLN")) {
            currency = "zł";
        } else if (row.getCurrency().equals("USD")) {
            currency = "$";
        } else if (row.getCurrency().equals("EUR")) {
            currency = "e";
        }
        boolean income = row.getEntryType().equals("Income");

        H4 moneyText = new H4(String.format("%s%,.2f %s", income ? "" : "-", row.getAmount(), currency));
        moneyText.getStyle().setColor(income ? "green" : "red");
        return moneyText;
    }


    private Component paymentMethod(BudgetRow row) {
        if (row.isGroup()) {
            return new Span(""); // groups don't have it
        }

        if (row.getPaymentMethod() == null) {
            return new Span("");
        }

        if (row.getPaymentMethod().equals("Cash")) {
            return new Span(new Icon("vaadin:money-bill-wave"));
        } else if (row.getPaymentMethod().equals("Card")) {
            return new Span(new Icon("vaadin:credit-card"));
        } else if (row.getPaymentMethod().equals("Transfer")) {
            return new Span(new Icon("vaadin:exchange"));
        }

        return new Span("");
    }
}
