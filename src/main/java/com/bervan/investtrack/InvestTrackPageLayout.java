package com.bervan.investtrack;

import com.bervan.common.MenuNavigationComponent;
import com.bervan.investtrack.view.AbstractWalletBalanceView;
import com.vaadin.flow.component.icon.VaadinIcon;

public class InvestTrackPageLayout extends MenuNavigationComponent {

    public InvestTrackPageLayout(String routeName, String... notVisibleButtonRoutes) {
        super(routeName, notVisibleButtonRoutes);

        addButtonIfVisible(menuButtonsRow, AbstractWalletBalanceView.ROUTE_NAME, "Incomes Outcomes", VaadinIcon.HOME.create());
        add(menuButtonsRow);
    }
}
