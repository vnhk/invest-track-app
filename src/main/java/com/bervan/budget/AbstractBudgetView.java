package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.component.CommonComponentHelper;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractPageView;
import com.bervan.investtrack.service.BudgetChartDataService;

public abstract class AbstractBudgetView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/budget";

    public AbstractBudgetView(BudgetGridService service, BervanViewConfig bervanViewConfig,
                               BudgetEntryService budgetEntryService,
                               BudgetChartDataService chartDataService) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new BudgetHeader());
        add(new BudgetAnalyticsPanel(chartDataService));
        add(new BudgetGridView(service, budgetEntryService, bervanViewConfig, new CommonComponentHelper(BudgetEntry.class)));
    }
}
