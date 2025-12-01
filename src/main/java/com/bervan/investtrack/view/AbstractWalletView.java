package com.bervan.investtrack.view;

import com.bervan.common.component.BervanButton;
import com.bervan.common.component.BervanButtonStyle;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Constants;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.service.WalletService;
import com.bervan.investtrack.service.WalletSnapshotService;
import com.bervan.logging.JsonLogger;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;

import java.math.BigDecimal;
import java.util.UUID;

@CssImport("./invest-track.css")
public abstract class AbstractWalletView extends AbstractPageView implements HasUrlParameter<String> {
    public static final String ROUTE_NAME = "/invest-track-app/wallets/";
    private final JsonLogger log = JsonLogger.getLogger(getClass());

    private final WalletService service;

    private final WalletSnapshotService snapshotService;
    private final BervanViewConfig bervanViewConfig;
    private Wallet wallet;
    // UI Components
    private Tabs tabSheet;
    private VerticalLayout contentArea;
    // Wallet form components
    private TextField nameField;
    private TextArea descriptionField;
    private TextField currencyField;
    private ComboBox<String> riskLevelCombo;
    private BervanButton saveWalletBtn;
    private BervanButton editWalletBtn;
    private Binder<Wallet> walletBinder = new Binder<>(Wallet.class);
    private boolean editMode = false;
    private WalletSnapshotListView snapshotsView;

    public AbstractWalletView(WalletService service, WalletSnapshotService snapshotService, BervanViewConfig bervanViewConfig) {
        super();
        this.service = service;
        this.snapshotService = snapshotService;
        this.bervanViewConfig = bervanViewConfig;
    }

    @Override
    public void setParameter(BeforeEvent event, String s) {
        String walletName = event.getRouteParameters().get("___url_parameter").orElse(UUID.randomUUID().toString());
        init(walletName);
    }

    private void init(String walletName) {
        try {
            add(new InvestTrackPageLayout(ROUTE_NAME, walletName));
            wallet = service.getWalletWithSnapshots(walletName);

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
        snapshotsView = new WalletSnapshotListView(snapshotService, wallet, bervanViewConfig);
        snapshotsView.renderCommonComponents();
    }

    private void showSnapshotForm() {
        contentArea.removeAll();
        contentArea.add(new Hr(), snapshotsView);
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

    // Update binding to remove fields that shouldn't be edited
    private void setupBindings() {
        // Wallet binder - remove calculated fields from binding
        walletBinder.forField(nameField).asRequired("Name is required")
                .bind(Wallet::getName, Wallet::setName);
        walletBinder.bind(descriptionField, Wallet::getDescription, Wallet::setDescription);
        walletBinder.bind(currencyField, Wallet::getCurrency, Wallet::setCurrency);
        walletBinder.bind(riskLevelCombo, Wallet::getRiskLevel, Wallet::setRiskLevel);
    }

    private void loadWalletData() {
        walletBinder.readBean(wallet);
    }

    private void exportSnapshotsToCSV() {
        try {
            String csvContent = snapshotService.exportSnapshotsToCsv(wallet.getId());

            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Export Snapshots to CSV");
            dialog.setWidth("80vw");
            TextArea textArea = new TextArea("Exported Snapshots");
            textArea.setWidthFull();
            textArea.setValue(csvContent);
            dialog.add(textArea);

            dialog.open();

            showSuccessNotification("CSV file exported successfully!");
        } catch (Exception e) {
            log.error("Failed to export snapshots to CSV", e);
            showErrorNotification("Failed to export snapshots!");
        }
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
        // Remove these since they're calculated:
        // initialValueField.setReadOnly(readOnly);
        // currentValueField.setReadOnly(readOnly);
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

        riskLevelCombo = new ComboBox<>("Risk Level");
        riskLevelCombo.setItems(Constants.RISK_LEVEL);
        riskLevelCombo.setReadOnly(true);

        editWalletBtn = new BervanButton("Edit Wallet", e -> toggleEditMode());

        saveWalletBtn = new BervanButton("Save Changes", e -> saveWallet());
        saveWalletBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveWalletBtn.setVisible(false);

        BervanButton deleteWalletBtn = new BervanButton("Delete Wallet", e -> confirmDeleteWallet(), BervanButtonStyle.WARNING);
        deleteWalletBtn.getStyle().set("margin-left", "auto");
    }

    private void showWalletForm() {
        contentArea.removeAll();

        FormLayout walletForm = new FormLayout();
        walletForm.add(nameField, descriptionField, currencyField, riskLevelCombo);
        walletForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        HorizontalLayout buttons = new HorizontalLayout();
        if (editMode) {
            saveWalletBtn.setVisible(true);
            buttons.add(saveWalletBtn, new BervanButton("Cancel", e -> cancelEdit()));
        } else {
            buttons.add(editWalletBtn);
        }

        // Add delete BervanButton (always visible)
        BervanButton deleteWalletBtn = new BervanButton("Delete Wallet", e -> confirmDeleteWallet());
        deleteWalletBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        buttons.add(deleteWalletBtn);

        contentArea.add(walletForm, buttons);
    }

    private void confirmDeleteWallet() {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Wallet");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(new Span(
                "Are you sure you want to delete the wallet '" + wallet.getName() + "'? " +
                        "This will also delete all snapshots and cannot be undone."
        ));

        HorizontalLayout bervanButtonsLayout = new HorizontalLayout(JustifyContentMode.BETWEEN);

        BervanButton confirmBtn = new BervanButton("DELETE", e -> {
            deleteWallet();
            confirmDialog.close();
        }, BervanButtonStyle.SECONDARY);

        BervanButton cancelBtn = new BervanButton("Cancel", e -> confirmDialog.close(), BervanButtonStyle.PRIMARY);

        bervanButtonsLayout.add(cancelBtn, confirmBtn);

        dialogLayout.add(bervanButtonsLayout);
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
}