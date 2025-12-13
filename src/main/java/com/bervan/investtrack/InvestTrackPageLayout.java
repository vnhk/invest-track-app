package com.bervan.investtrack;

import com.bervan.common.MenuNavigationComponent;
import com.bervan.investments.recommendation.AbstractInvestmentRecommendationView;
import com.bervan.investtrack.view.*;
import com.bervan.investtrack.view.dashboards.AbstractWalletsDashboardView;
import com.vaadin.flow.component.icon.VaadinIcon;

public class InvestTrackPageLayout extends MenuNavigationComponent {

    public InvestTrackPageLayout(String routeName, String walletName, String... notVisibleButtonRoutes) {
        super(routeName, notVisibleButtonRoutes);

        addButtonIfVisible(menuButtonsRow, AbstractWalletsDashboardView.ROUTE_NAME, "Dashboard", VaadinIcon.DASHBOARD.create());
        addButtonIfVisible(menuButtonsRow, AbstractReportsRecommendationsView.ROUTE_NAME, "Recommendations", VaadinIcon.FLAG.create());
        addButtonIfVisible(menuButtonsRow, AbstractStockPriceAlertsView.ROUTE_NAME, "Alerts", VaadinIcon.ALARM.create());
        addButtonIfVisible(menuButtonsRow, AbstractWalletsView.ROUTE_NAME, "Wallets", VaadinIcon.WALLET.create());
        if (walletName != null && !walletName.isEmpty()) {
            addButtonIfVisible(menuButtonsRow, AbstractWalletView.ROUTE_NAME, walletName, VaadinIcon.INFO_CIRCLE.create());
        }
        addButtonIfVisible(menuButtonsRow, AbstractInvestmentRecommendationView.ROUTE_NAME, "Recommendation History", VaadinIcon.LIST_OL.create());
        addButtonIfVisible(menuButtonsRow, AbstractImportExportData.ROUTE_NAME, "Data IE", VaadinIcon.DATABASE.create());
        add(menuButtonsRow);
    }
}
