package com.bervan.budget;

import com.bervan.common.view.AbstractPageView;

public abstract class AbstractBudgetView extends AbstractPageView {

    public AbstractBudgetView(BudgetService service) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new BudgetHeader());
//        service.loadBudget()
//                .forEach(group -> add(new BudgetGroupComponent(group)));
        add(new BudgetGridView(service));
    }
}
