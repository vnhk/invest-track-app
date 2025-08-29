package com.bervan.investtrack.view;

import com.bervan.common.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Constants;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.WalletService;
import com.bervan.investtrack.service.WalletSnapshotService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletView extends AbstractPageView implements HasUrlParameter<String> {
    public static final String ROUTE_NAME = "/invest-track-app/wallets/";

    private final WalletService service;

    private final WalletSnapshotService snapshotService;

    private Wallet wallet;

    // UI Components
    private Tabs tabSheet;
    private VerticalLayout contentArea;

    // Wallet form components
    private TextField nameField;
    private TextArea descriptionField;
    private TextField currencyField;
    private BigDecimalField initialValueField;
    private BigDecimalField currentValueField;
    private ComboBox<String> riskLevelCombo;
    private Button saveWalletBtn;
    private Button editWalletBtn;

    // Snapshot form components
    private DatePicker snapshotDatePicker;
    private BigDecimalField portfolioValueField;
    private BigDecimalField monthlyDepositField;
    private BigDecimalField monthlyWithdrawalField;
    private BigDecimalField monthlyEarningsField;
    private TextArea snapshotNotesField;
    private Button saveSnapshotBtn;
    private Grid<WalletSnapshot> snapshotGrid;

    private Binder<Wallet> walletBinder = new Binder<>(Wallet.class);
    private Binder<WalletSnapshot> snapshotBinder = new Binder<>(WalletSnapshot.class);

    private boolean editMode = false;

    public AbstractWalletView(WalletService service, WalletSnapshotService snapshotService) {
        super();
        this.service = service;
        this.snapshotService = snapshotService;
    }

    @Override
    public void setParameter(BeforeEvent event, String s) {
        String walletName = event.getRouteParameters().get("___url_parameter").orElse(UUID.randomUUID().toString());
        init(walletName);
    }

    private void init(String walletName) {
        try {
            add(new InvestTrackPageLayout(ROUTE_NAME, walletName));
            wallet = service.getWalletByName(walletName).iterator().next();

            initializeComponents();
            setupLayout();
            setupBindings();
            loadWalletData();

        } catch (Exception e) {
            log.error("Unable to load wallet: {}", walletName, e);
            showErrorNotification("Unable to load wallet: " + walletName + ".");
        }
    }

    private void initializeComponents() {
        // Tabs
        tabSheet = new Tabs();
        Tab walletTab = new Tab("Wallet Details");
        Tab snapshotsTab = new Tab("Monthly Snapshots");
        tabSheet.add(walletTab, snapshotsTab);

        contentArea = new VerticalLayout();

        // Create forms
        createWalletForm();
        createSnapshotForm();

        // Show wallet form by default
        showWalletForm();

        tabSheet.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab.getLabel().equals("Wallet Details")) {
                showWalletForm();
            } else if (selectedTab.getLabel().equals("Monthly Snapshots")) {
                showSnapshotForm();
            }
        });
    }

    private void createSnapshotForm() {
        snapshotDatePicker = new DatePicker("Snapshot Date");
        snapshotDatePicker.setValue(LocalDate.now().withDayOfMonth(1).minusDays(1)); // Last day of previous month

        portfolioValueField = new BigDecimalField("Portfolio Value");
        monthlyDepositField = new BigDecimalField("Monthly Deposit");
        monthlyWithdrawalField = new BigDecimalField("Monthly Withdrawal");
        monthlyEarningsField = new BigDecimalField("Monthly Earnings");
        snapshotNotesField = new TextArea("Notes");

        saveSnapshotBtn = new Button("Save Snapshot", e -> saveSnapshot());
        saveSnapshotBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        snapshotGrid = new Grid<>(WalletSnapshot.class, false);
        setupSnapshotGrid();
    }

    private void setupSnapshotGrid() {
        snapshotGrid.addColumn(WalletSnapshot::getSnapshotDate).setHeader("Date");
        snapshotGrid.addColumn(snapshot -> formatCurrency(snapshot.getPortfolioValue(), wallet.getCurrency()))
                .setHeader("Portfolio Value");
        snapshotGrid.addColumn(snapshot -> formatCurrency(snapshot.getMonthlyDeposit(), wallet.getCurrency()))
                .setHeader("Deposit");
        snapshotGrid.addColumn(snapshot -> formatCurrency(snapshot.getMonthlyWithdrawal(), wallet.getCurrency()))
                .setHeader("Withdrawal");
        snapshotGrid.addColumn(snapshot -> formatCurrency(snapshot.getMonthlyEarnings(), wallet.getCurrency()))
                .setHeader("Earnings");
        snapshotGrid.addColumn(WalletSnapshot::getNotes).setHeader("Notes");

        snapshotGrid.addComponentColumn(snapshot -> {
            Button deleteBtn = new Button("Delete");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteBtn.addClickListener(e -> confirmDeleteSnapshot(snapshot));
            return deleteBtn;
        }).setHeader("Actions");
    }

    private void setupLayout() {
        add(new H3("Wallet: " + wallet.getName()));
        add(createWalletSummaryCard());
        add(tabSheet);
        add(contentArea);
    }

    private VerticalLayout createWalletSummaryCard() {
        VerticalLayout summary = new VerticalLayout();
        summary.setSpacing(false);
        summary.setPadding(true);
        summary.addClassName("wallet-summary-card");

        HorizontalLayout summaryRow = new HorizontalLayout();
        summaryRow.add(
                createSummaryItem("Current Value", formatCurrency(wallet.getCurrentValue(), wallet.getCurrency())),
                createSummaryItem("Total Return", formatCurrency(wallet.calculateTotalReturn(), wallet.getCurrency())),
                createSummaryItem("Return Rate", formatPercentage(wallet.getReturnRate())),
                createSummaryItem("Risk Level", wallet.getRiskLevel())
        );

        summary.add(summaryRow);
        return summary;
    }

    private VerticalLayout createSummaryItem(String label, String value) {
        VerticalLayout item = new VerticalLayout();
        item.setSpacing(false);
        item.setPadding(false);
        item.add(new Span(label));
        Span valueSpan = new Span(value);
        valueSpan.addClassName("summary-value");
        item.add(valueSpan);
        return item;
    }

    private void setupBindings() {
        // Wallet binder
        walletBinder.forField(nameField).asRequired("Name is required")
                .bind(Wallet::getName, Wallet::setName);
        walletBinder.bind(descriptionField, Wallet::getDescription, Wallet::setDescription);
        walletBinder.bind(currencyField, Wallet::getCurrency, Wallet::setCurrency);
        walletBinder.bind(initialValueField, Wallet::getInitialValue, Wallet::setInitialValue);
        walletBinder.bind(currentValueField, Wallet::getCurrentValue, Wallet::setCurrentValue);
        walletBinder.bind(riskLevelCombo, Wallet::getRiskLevel, Wallet::setRiskLevel);

        // Snapshot binder
        snapshotBinder.forField(snapshotDatePicker).asRequired("Date is required")
                .bind(WalletSnapshot::getSnapshotDate, WalletSnapshot::setSnapshotDate);
        snapshotBinder.forField(portfolioValueField).asRequired("Portfolio value is required")
                .bind(WalletSnapshot::getPortfolioValue, WalletSnapshot::setPortfolioValue);
        snapshotBinder.bind(monthlyDepositField, WalletSnapshot::getMonthlyDeposit, WalletSnapshot::setMonthlyDeposit);
        snapshotBinder.bind(monthlyWithdrawalField, WalletSnapshot::getMonthlyWithdrawal, WalletSnapshot::setMonthlyWithdrawal);
        snapshotBinder.bind(monthlyEarningsField, WalletSnapshot::getMonthlyEarnings, WalletSnapshot::setMonthlyEarnings);
        snapshotBinder.bind(snapshotNotesField, WalletSnapshot::getNotes, WalletSnapshot::setNotes);
    }

    private void loadWalletData() {
        walletBinder.readBean(wallet);
    }

    private void showSnapshotForm() {
        contentArea.removeAll();

        FormLayout snapshotForm = new FormLayout();
        snapshotForm.add(snapshotDatePicker, portfolioValueField, monthlyDepositField,
                monthlyWithdrawalField, monthlyEarningsField, snapshotNotesField);
        snapshotForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Pre-fill portfolio value with current wallet value
        portfolioValueField.setValue(wallet.getCurrentValue());

        contentArea.add(new H3("Monthly Snapshot"));
        contentArea.add(snapshotForm);
        contentArea.add(saveSnapshotBtn);
        contentArea.add(new H3("Snapshot History"));
        contentArea.add(snapshotGrid);

        refreshSnapshotGrid();
    }

    private void toggleEditMode() {
        editMode = !editMode;
        setFieldsReadOnly(!editMode);
        showWalletForm(); // Refresh the form
    }

    private void setFieldsReadOnly(boolean readOnly) {
        nameField.setReadOnly(readOnly);
        descriptionField.setReadOnly(readOnly);
        currencyField.setReadOnly(readOnly);
        initialValueField.setReadOnly(readOnly);
        currentValueField.setReadOnly(readOnly);
        riskLevelCombo.setReadOnly(readOnly);
    }

    private void cancelEdit() {
        editMode = false;
        setFieldsReadOnly(true);
        walletBinder.readBean(wallet); // Restore original values
        showWalletForm();
    }

    private void saveWallet() {
        try {
            walletBinder.writeBean(wallet);
            wallet = service.save(wallet);

            editMode = false;
            setFieldsReadOnly(true);

            showSuccessNotification("Wallet updated successfully!");
            showWalletForm();

        } catch (ValidationException e) {
            showErrorNotification("Please check the data validity");
        }
    }


    private void saveSnapshot() {
        try {
            WalletSnapshot snapshot = new WalletSnapshot();
            snapshot.setId(UUID.randomUUID());
            snapshotBinder.writeBean(snapshot);

            wallet = service.addSnapshot(wallet.getId(), snapshot);

            showSuccessNotification("Monthly snapshot saved successfully!");
            refreshSnapshotGrid();
            snapshotBinder.readBean(new WalletSnapshot()); // Clear form

        } catch (ValidationException e) {
            showErrorNotification("Please check the snapshot data validity");
        }
    }

    private void refreshSnapshotGrid() {
        snapshotGrid.setItems(snapshotService.findByWalletId(wallet.getId()));
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) return "0.00";
        return String.format("%.2f %s", amount, currency);
    }

    private String formatPercentage(BigDecimal percentage) {
        if (percentage == null) return "0.00%";
        return String.format("%.2f%%", percentage);
    }

    private void createWalletForm() {
        nameField = new TextField("Wallet Name");
        nameField.setReadOnly(true);

        descriptionField = new TextArea("Description");
        descriptionField.setReadOnly(true);

        currencyField = new TextField("Currency");
        currencyField.setReadOnly(true);

        initialValueField = new BigDecimalField("Initial Value");
        initialValueField.setReadOnly(true);

        currentValueField = new BigDecimalField("Current Value");
        currentValueField.setReadOnly(true);

        riskLevelCombo = new ComboBox<>("Risk Level");
        riskLevelCombo.setItems(Constants.RISK_LEVEL);
        riskLevelCombo.setReadOnly(true);

        editWalletBtn = new Button("Edit Wallet", e -> toggleEditMode());

        saveWalletBtn = new Button("Save Changes", e -> saveWallet());
        saveWalletBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveWalletBtn.setVisible(false);

        // Add delete button
        Button deleteWalletBtn = new Button("Delete Wallet", e -> confirmDeleteWallet());
        deleteWalletBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteWalletBtn.getStyle().set("margin-left", "auto");
    }

    private void showWalletForm() {
        contentArea.removeAll();

        FormLayout walletForm = new FormLayout();
        walletForm.add(nameField, descriptionField, currencyField, initialValueField,
                currentValueField, riskLevelCombo);
        walletForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        HorizontalLayout buttons = new HorizontalLayout();
        if (editMode) {
            saveWalletBtn.setVisible(true);
            buttons.add(saveWalletBtn, new Button("Cancel", e -> cancelEdit()));
        } else {
            buttons.add(editWalletBtn);
        }

        // Add delete button (always visible)
        Button deleteWalletBtn = new Button("Delete Wallet", e -> confirmDeleteWallet());
        deleteWalletBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        buttons.add(deleteWalletBtn);

        contentArea.add(walletForm, buttons);
    }

    private void confirmDeleteWallet() {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Wallet");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(new Span(
                "Are you sure you want to delete the portfolio '" + wallet.getName() + "'? " +
                        "This will also delete all snapshots and cannot be undone."
        ));

        HorizontalLayout buttonsLayout = new HorizontalLayout();

        Button confirmBtn = new Button("Delete", e -> {
            deleteWallet();
            confirmDialog.close();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Cancel", e -> confirmDialog.close());

        buttonsLayout.add(cancelBtn, confirmBtn);

        dialogLayout.add(buttonsLayout);
        confirmDialog.add(dialogLayout);
        confirmDialog.open();
    }

    private void confirmDeleteSnapshot(WalletSnapshot snapshot) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Snapshot");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(new Span(
                "Are you sure you want to delete the snapshot from " + snapshot.getSnapshotDate() + "? " +
                        "This cannot be undone."
        ));

        HorizontalLayout buttonsLayout = new HorizontalLayout();

        Button confirmBtn = new Button("Delete", e -> {
            deleteSnapshot(snapshot);
            confirmDialog.close();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Cancel", e -> confirmDialog.close());

        buttonsLayout.add(cancelBtn, confirmBtn);

        dialogLayout.add(buttonsLayout);
        confirmDialog.add(dialogLayout);
        confirmDialog.open();
    }

    private void deleteWallet() {
        try {
            service.deleteById(wallet.getId());
            showSuccessNotification("Portfolio deleted successfully!");

            getUI().ifPresent(ui -> ui.navigate("/"));

        } catch (Exception e) {
            log.error("Failed to delete wallet: {}", wallet.getId(), e);
            showErrorNotification("Failed to Delete Wallet: " + e.getMessage());
        }
    }

    private void deleteSnapshot(WalletSnapshot snapshot) {
        try {
            snapshotService.deleteById(snapshot.getId());

            wallet = service.findById(wallet.getId());
            recalculateWalletValues(wallet);

            showSuccessNotification("Snapshot deleted successfully! Portfolio values recalculated.");

            // Refresh all displays
            refreshSnapshotGrid();
            loadWalletData(); // Refresh wallet form with new values

            // If we're showing wallet details, refresh the summary card
            if (tabSheet.getSelectedIndex() == 0) {
                setupLayout(); // This will recreate the summary card with updated values
            }

        } catch (Exception e) {
            log.error("Failed to delete snapshot: {}", snapshot.getId(), e);
            showErrorNotification("Failed to delete snapshot: " + e.getMessage());
        }
    }

    private void recalculateWalletValues(Wallet wallet) {
        List<WalletSnapshot> remainingSnapshots = snapshotService.findByWalletId(wallet.getId());

        if (remainingSnapshots.isEmpty()) {
            // No snapshots left - reset to initial values
            wallet.setCurrentValue(wallet.getInitialValue());
            wallet.setReturnRate(BigDecimal.ZERO);
        } else {
            // Find the latest snapshot by date
            WalletSnapshot latestSnapshot = remainingSnapshots.stream()
                    .max((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                    .orElse(null);

            if (latestSnapshot != null) {
                // Update current value to the latest snapshot's portfolio value
                wallet.setCurrentValue(latestSnapshot.getPortfolioValue());

                // Recalculate totals from all remaining snapshots
                BigDecimal totalDeposits = remainingSnapshots.stream()
                        .map(WalletSnapshot::getMonthlyDeposit)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalWithdrawals = remainingSnapshots.stream()
                        .map(WalletSnapshot::getMonthlyWithdrawal)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalEarnings = remainingSnapshots.stream()
                        .map(WalletSnapshot::getMonthlyEarnings)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Update wallet totals
                wallet.setTotalDeposits(totalDeposits);
                wallet.setTotalWithdrawals(totalWithdrawals);
                wallet.setTotalEarnings(totalEarnings);

                // Recalculate return rate
                wallet.updateReturnRate();
            }
        }
    }

}