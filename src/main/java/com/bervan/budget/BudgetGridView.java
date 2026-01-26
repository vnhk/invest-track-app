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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BudgetGridView extends AbstractPageView {
    private static final Map<String, VaadinIcon> CATEGORY_ICONS = Map.of(
            "shop", VaadinIcon.CART,
            "home", VaadinIcon.HOME,
            "house", VaadinIcon.HOME,
            "car", VaadinIcon.CAR,
            "work", VaadinIcon.OFFICE,
            "wedding", VaadinIcon.HEART,
            "entertainment", VaadinIcon.MOVIE,
            "subscription", VaadinIcon.RECORDS,
            "loan", VaadinIcon.INSTITUTION
    );
    private static final VaadinIcon OTHERS_ICON = VaadinIcon.TAGS;
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

        grid.addComponentHierarchyColumn(budgetRow -> {
                    Component component = name(budgetRow);
                    return component;
                }).setHeader("Name")
                .setAutoWidth(true)
                .setResizable(true);

        grid.addColumn(new ComponentRenderer<>(this::money))
                .setHeader("Amount")
                .setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(this::paymentMethod))
                .setHeader("Payment Method")
                .setWidth("25px");

        grid.addColumn(e -> e.getEntryDate())
                .setHeader("Date")
                .setWidth("40px");

        grid.addColumn(e -> e.getNotes())
                .setHeader("Notes")
                .setAutoWidth(true);

        data = service.loadTreeData(LocalDate.of(2020, 1, 1), LocalDate.of(2050, 1, 1));
        provider = new TreeDataProvider<>(data);
        grid.setDataProvider(provider);

        grid.addComponentColumn(row -> {
            Checkbox cb = new Checkbox();
            cb.setEnabled(!row.isGroup());
            return cb;
        }).setWidth("50px");

        BudgetRow firstRoot = data.getRootItems().stream().findFirst().orElse(null);
        if (firstRoot != null) { //expand #1 row
            grid.expandRecursively(List.of(firstRoot), Integer.MAX_VALUE);
        }

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
                return bervanButton;
            } else if (row.getRowType().equals("ITEM_ROW")) {
                BervanButton bervanButton = new BervanButton("+", buttonClickEvent -> {
                    addItemRow(data.getParent(row), data.getParent(data.getParent(row)));
                });
                return bervanButton;
            }
        }

        if (row.getRowType().equals("CATEGORY_ROW")) {
            String name = row.getName().toLowerCase();

            VaadinIcon icon = CATEGORY_ICONS.entrySet().stream()
                    .filter(e -> name.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(OTHERS_ICON);

            Icon vaadinIcon = icon.create();
            vaadinIcon.getStyle()
                    .set("margin-right", "6px")
                    .set("color", "var(--lumo-secondary-text-color)");

            HorizontalLayout layout = new HorizontalLayout(vaadinIcon, new Span(row.getName()));
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(false);

            if (icon == OTHERS_ICON) {
                String tooltipText = "Recognized categories:\n" +
                        CATEGORY_ICONS.keySet().stream()
                                .sorted()
                                .collect(Collectors.joining(", "));

                Tooltip.forComponent(layout)
                        .withText(tooltipText)
                        .withPosition(Tooltip.TooltipPosition.END);
            }

            return layout;
        }

        return new H4(row.getName());
    }

    private void addDateRow() {
        Dialog dialog = new Dialog();
        BervanDatePicker datePicker = new BervanDatePicker("Date", LocalDate.now());
        dialog.add(datePicker);
        dialog.add(new Hr());
        dialog.add(new BervanButton("Add", e -> {
            List<BudgetRow> children = data.getChildren(null);
            String dateStr = datePicker.getValue().getMonthValue() + "-" + datePicker.getValue().getYear();
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

            //set last day of month as default entry date or if current month, set today
            String dateValue = date.getName();
            String[] dateSplit = dateValue.split("-");
            int year = Integer.parseInt(dateSplit[1]);
            int month = Integer.parseInt(dateSplit[0]);
            LocalDate localDate = YearMonth.of(year, month).atEndOfMonth();

            if (month == LocalDate.now().getMonthValue()) {
                localDate = LocalDate.now();
            }

            fieldsHolder
                    .get(BudgetEntry.class.getDeclaredField("entryDate"))
                    .setValue(localDate);

            dialog.add(new BervanButton("Add", e -> {
                for (Map.Entry<Field, AutoConfigurableField> fieldAutoConfigurableFieldEntry : fieldsHolder.entrySet()) {
                    fieldAutoConfigurableFieldEntry.getValue().validate();
                    if (fieldAutoConfigurableFieldEntry.getValue().isInvalid()) {
                        return;
                    }
                }
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
            return new Span(new Icon("vaadin:cash"));
        } else if (row.getPaymentMethod().equals("Card")) {
            return new Span(new Icon("vaadin:credit-card"));
        } else if (row.getPaymentMethod().equals("Transfer")) {
            return new Span(new Icon("vaadin:money-exchange"));
        }

        return new Span("");
    }
}
