package com.bervan.investtrack;

import com.bervan.common.MenuNavigationComponent;
import com.bervan.investtrack.view.AbstractWalletBalanceView;
import com.bervan.investtrack.view.AbstractWalletsView;
import com.vaadin.flow.component.icon.VaadinIcon;

public class InvestTrackPageLayout extends MenuNavigationComponent {

    public InvestTrackPageLayout(String routeName, String walletName, String... notVisibleButtonRoutes) {
        super(routeName, notVisibleButtonRoutes);

        addButtonIfVisible(menuButtonsRow, AbstractWalletBalanceView.ROUTE_NAME, "Wallet Balance", VaadinIcon.DASHBOARD.create());
        addButtonIfVisible(menuButtonsRow, AbstractWalletsView.ROUTE_NAME, "Wallets", VaadinIcon.WALLET.create());
        if (walletName != null && !walletName.isEmpty()) {
            addButtonIfVisible(menuButtonsRow, AbstractWalletsView.ROUTE_NAME, walletName, VaadinIcon.WALLET.create());
        }
        add(menuButtonsRow);
    }
}
