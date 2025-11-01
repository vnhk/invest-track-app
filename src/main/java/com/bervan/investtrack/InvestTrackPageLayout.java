package com.bervan.investtrack;

import com.bervan.common.MenuNavigationComponent;
import com.bervan.investtrack.view.AbstractStockPriceAlertsView;
import com.bervan.investtrack.view.dashboards.AbstractWalletsDashboardView;
import com.bervan.investtrack.view.AbstractWalletView;
import com.bervan.investtrack.view.AbstractWalletsView;
import com.vaadin.flow.component.icon.VaadinIcon;

public class InvestTrackPageLayout extends MenuNavigationComponent {

    public InvestTrackPageLayout(String routeName, String walletName, String... notVisibleButtonRoutes) {
        super(routeName, notVisibleButtonRoutes);

        addButtonIfVisible(menuButtonsRow, AbstractWalletsDashboardView.ROUTE_NAME, "Wallet Balance", VaadinIcon.DASHBOARD.create());
        addButtonIfVisible(menuButtonsRow, AbstractStockPriceAlertsView.ROUTE_NAME, "Alerts", VaadinIcon.ALARM.create());
        addButtonIfVisible(menuButtonsRow, AbstractWalletsView.ROUTE_NAME, "Wallets", VaadinIcon.WALLET.create());
        if (walletName != null && !walletName.isEmpty()) {
            addButtonIfVisible(menuButtonsRow, AbstractWalletView.ROUTE_NAME, walletName, VaadinIcon.INFO_CIRCLE.create());
        }
        add(menuButtonsRow);
    }
}
