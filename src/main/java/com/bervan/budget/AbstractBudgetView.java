package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.component.CommonComponentHelper;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractPageView;
import com.bervan.investtrack.service.BudgetChartDataService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;

@Deprecated
public abstract class AbstractBudgetView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/budget";

    public AbstractBudgetView(BudgetGridService service, BervanViewConfig bervanViewConfig,
                               BudgetEntryService budgetEntryService,
                               BudgetChartDataService chartDataService) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new BudgetHeader());

        BudgetGridView gridView = new BudgetGridView(service, budgetEntryService, bervanViewConfig,
                new CommonComponentHelper(BudgetEntry.class));

        Div chartsContent = new Div();
        chartsContent.setWidthFull();

        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();
        tabSheet.add("Budget Tree", gridView);
        Tab chartsTab = tabSheet.add("Charts", chartsContent);

        BudgetAnalyticsPanel[] holder = {null};
        tabSheet.addSelectedChangeListener(e -> {
            if (e.getSelectedTab().equals(chartsTab) && holder[0] == null) {
                BudgetAnalyticsPanel panel = new BudgetAnalyticsPanel(chartDataService);
                panel.setWidthFull();
                holder[0] = panel;
                chartsContent.add(panel);
            }
        });

        add(tabSheet);
    }
}
