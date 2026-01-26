package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.common.component.CommonComponentHelper;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractPageView;

public abstract class AbstractBudgetView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/budget";

    public AbstractBudgetView(BudgetService service, BervanViewConfig bervanViewConfig) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new BudgetHeader());
        add(new BudgetGridView(service, bervanViewConfig, new CommonComponentHelper(BudgetEntry.class)));
    }
}
