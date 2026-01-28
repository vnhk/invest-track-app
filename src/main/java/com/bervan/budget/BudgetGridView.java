package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.component.*;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractPageView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H5;
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
import java.util.*;
import java.util.stream.Collectors;

public class BudgetGridView extends AbstractPageView {
    private static final Map<String, VaadinIcon> CATEGORY_ICONS = Map.of(
            "shop", VaadinIcon.CART,
            "shopping", VaadinIcon.CART,
            "food", VaadinIcon.CUTLERY,
            "house", VaadinIcon.HOME,
            "car", VaadinIcon.CAR,
            "work", VaadinIcon.OFFICE,
            "wedding", VaadinIcon.HEART,
            "entertainment", VaadinIcon.MOVIE,
            "subscription", VaadinIcon.RECORDS,
            "loan", VaadinIcon.INSTITUTION
    );
    private static final VaadinIcon OTHERS_ICON = VaadinIcon.TAGS;
    private final BudgetGridService service;
    private final BudgetEntryService budgetEntryService;
    private final BervanViewConfig bervanViewConfig;
    private final ComponentHelper<UUID, BudgetEntry> componentHelper;
    private final TreeGrid<BudgetRow> grid;
    private TreeDataProvider<BudgetRow> provider;
    private TreeData<BudgetRow> data;
    private Set<BudgetRow> selectedRows;
    private BervanButton copy;
    private BervanButton delete;
    private BervanButton edit;
    private BervanButton move;

    public BudgetGridView(BudgetGridService service, BudgetEntryService budgetEntryService, BervanViewConfig bervanViewConfig, ComponentHelper<UUID, BudgetEntry> componentHelper) {
        this.service = service;
        this.budgetEntryService = budgetEntryService;
        this.bervanViewConfig = bervanViewConfig;
        this.componentHelper = componentHelper;
        grid = new TreeGrid<>();

        HorizontalLayout toolbar = buildToolbar();

        grid.setWidthFull();
        grid.setHeightFull();

        buildColumns();

        add(toolbar, grid);
        setSizeFull();

        refreshAll();
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setSpacing(true);

        BervanButton expandAll = new BervanButton("Expand all", e ->
                grid.expandRecursively(data.getRootItems(), Integer.MAX_VALUE), BervanButtonStyle.WARNING);
        BervanButton collapseAll = new BervanButton("Collapse all", e ->
                grid.collapseRecursively(data.getRootItems(), Integer.MAX_VALUE), BervanButtonStyle.WARNING);
        delete = new BervanButton("Delete", e -> delete(), BervanButtonStyle.WARNING);
        copy = new BervanButton("Copy", e -> copy(), BervanButtonStyle.WARNING);
        edit = new BervanButton("Edit", e -> edit(), BervanButtonStyle.WARNING);
        move = new BervanButton("Move", e -> move(), BervanButtonStyle.WARNING);
        toolbar.add(expandAll, collapseAll, delete, copy, edit);
        return toolbar;
    }

    private void move() {
        Dialog dialog = getDialog();
        BervanDatePicker datePicker = new BervanDatePicker("New date (leave empty if you don't want to change)", false);
        dialog.add(datePicker);

        BervanTextField categoryField = new BervanTextField("New Category (leave empty if you don't want to change)");
        dialog.add(categoryField);

        String commonCategory = null;
        LocalDate commonLocalDate = null;

        Set<UUID> uuids = selectedRows.stream().map(e -> e.getId()).collect(Collectors.toSet());

        List<BudgetEntry> originalSelected = service.load(uuids);
        int categoryChanged = 0;
        int dateChanged = 0;
        String categoryToSet = null;
        LocalDate dateToSet = null;

        for (BudgetEntry selectedEntry : originalSelected) {
            if (commonCategory == null) {
                commonCategory = selectedEntry.getCategory();
            }

            if (commonLocalDate == null) {
                commonLocalDate = selectedEntry.getEntryDate();
            }

            if (commonLocalDate != selectedEntry.getEntryDate()) {
                dateChanged++;
            }

            if (commonCategory != selectedEntry.getCategory()) {
                categoryChanged++;
            }
        }

        if (categoryChanged <= 1) {
            categoryField.setValue(commonCategory);
        }
        if (dateChanged <= 1) {
            datePicker.setValue(commonLocalDate);
        }

        dialog.add(new BervanButton("Move", e1 -> {
            LocalDate newDate = datePicker.getValue();
            String newCategory = categoryField.getValue();

            service.update(originalSelected, newDate, newCategory);

            refreshAll();

            showPrimaryNotification("Items moved successfully.");

            dialog.close();
        }));

        dialog.open();
    }

    private Dialog getDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("60vw");
        dialog.add(new Hr());
        return dialog;
    }

    private void delete() {
        for (BudgetRow row : selectedRows) {
            BudgetRow parent = data.getParent(row);
            data.removeItem(row);

            if (parent != null && data.getChildren(parent).isEmpty()) {
                data.removeItem(parent);
            }

            budgetEntryService.deleteById(row.getId());
        }

        refreshAll();
    }

    private Checkbox checkboxColumn(BudgetRow row, BervanButton delete, BervanButton copy, BervanButton edit) {
        Checkbox cb = new Checkbox();
        cb.setEnabled(!row.isGroup() && !row.getName().equals("+"));

        cb.addValueChangeListener(e -> {
            if (e.getValue()) {
                selectedRows.add(row);
            } else {
                selectedRows.remove(row);
            }

            delete.setEnabled(!selectedRows.isEmpty());
            copy.setEnabled(!selectedRows.isEmpty());
            edit.setEnabled(selectedRows.size() == 1);
        });
        return cb;
    }

    private void copy() {
        Dialog dialog = getDialog();
        BervanDatePicker field = new BervanDatePicker("New date", true);
        dialog.add(field);

        dialog.add(new BervanButton("Copy", e1 -> {
            LocalDate newDate = field.getValue();

            for (BudgetRow row : selectedRows) {
                service.copyToMonth(row, newDate);
            }

            refreshAll();

            showPrimaryNotification("Items copied successfully.");

            dialog.close();
        }));

        dialog.open();
    }

    private void buildColumns() {
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

        grid.addComponentColumn(row -> {
            Checkbox cb = checkboxColumn(row, delete, copy, edit);
            return cb;
        }).setWidth("50px");
    }

    private void expandFirstRow() {
        BudgetRow firstRoot = data.getRootItems().stream().findFirst().orElse(null);
        if (firstRoot != null) { //expand #1 row
            grid.expandRecursively(List.of(firstRoot), Integer.MAX_VALUE);
        }
    }

    private void refreshAll() {
        delete.setEnabled(false);
        copy.setEnabled(false);
        edit.setEnabled(false);

        selectedRows = new HashSet<>();
        data = service.loadTreeData(LocalDate.of(2020, 1, 1), LocalDate.of(2050, 1, 1));
        provider = new TreeDataProvider<>(data);
        grid.setDataProvider(provider);

        expandFirstRow();
    }

    private Component name(BudgetRow row) {
        if (row.getName().equals("+")) {
            Icon plusIcon = new Icon(VaadinIcon.PLUS);
            plusIcon.getStyle().set("cursor", "pointer");
            plusIcon.getStyle().set("color", "var(--lumo-primary-color) !important");
            if (row.getRowType().equals("DATE_ROW")) {
                plusIcon.addClickListener(buttonClickEvent -> addDateRow());
                return plusIcon;
            } else if (row.getRowType().equals("CATEGORY_ROW")) {
                plusIcon.addClickListener(buttonClickEvent -> addCategoryRow(data.getParent(row)));
                return plusIcon;
            } else if (row.getRowType().equals("ITEM_ROW")) {
                plusIcon.addClickListener(buttonClickEvent -> addItemRow(data.getParent(row), data.getParent(data.getParent(row))));
                return plusIcon;
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

        return new H5(row.getName());
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

            service.copyRecurringToAnotherDate(datePicker.getValue());
            refreshAll();
            showPrimaryNotification("Date added successfully. Recurring entries will be added automatically.");
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
            refreshAll();
            showPrimaryNotification("Category added successfully.");
            dialog.close();
        }));
        dialog.open();
    }

    private void addItemRow(BudgetRow category, BudgetRow date) {
        SaveItemDialog<UUID, BudgetEntry> saveItemDialog = new SaveItemDialog<>(componentHelper, budgetEntryService, bervanViewConfig, BudgetEntry.class) {
            @Override
            protected void customFieldInFormItemLayout(Field field, VerticalLayout layoutForField, AutoConfigurableField componentWithValue) {
                if (field.getName().equals("entryDate")) {
                    //set date to the end of month or today if it's the current month'
                    String dateValue = date.getName();
                    String[] dateSplit = dateValue.split("-");
                    int year = Integer.parseInt(dateSplit[1]);
                    int month = Integer.parseInt(dateSplit[0]);
                    LocalDate localDate = YearMonth.of(year, month).atEndOfMonth();

                    if (month == LocalDate.now().getMonthValue()) {
                        localDate = LocalDate.now();
                    }
                    componentWithValue.setValue(localDate);
                }
            }
        };

        saveItemDialog.setCustomizeSavingInSaveFormFunction((BudgetEntry entry) -> {
            entry.setCategory(category.getName());

            return entry;
        });
        saveItemDialog.setCustomizePostSaveFunction((BudgetEntry newEntry) -> {
            BudgetRow newItem = service.item(newEntry);
            data.addItem(category, newItem);
            refreshAll();
            return newEntry;
        });
        saveItemDialog.openSaveDialog();
    }

    private void edit() {
        BudgetRow item = selectedRows.iterator().next();
        EditItemDialog<UUID, BudgetEntry> editItemDialog = new EditItemDialog<>(componentHelper, budgetEntryService, bervanViewConfig);
        BudgetEntry budgetEntry = service.getItem(item.getId());

        editItemDialog.openEditDialog(budgetEntry);
        editItemDialog.setCustomizeSavingInEditFormFunction((BudgetEntry entry) -> {
            refreshAll();
            return entry;
        });

    }

    private Component money(BudgetRow row) {
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

        if (row.isGroup()) {
            H5 moneyText = new H5(String.format("%s%,.2f %s", income ? "" : "-", row.getAmount(), currency));
            moneyText.getStyle().setColor(income ? "green" : "red");
            moneyText.getStyle().set("margin-left", "50px");
            return moneyText;
        } else {
            H5 moneyText = new H5(String.format("%s%,.2f %s", income ? "" : "-", row.getAmount(), currency));
            moneyText.getStyle().setColor(income ? "green" : "red");
            return moneyText;
        }
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
