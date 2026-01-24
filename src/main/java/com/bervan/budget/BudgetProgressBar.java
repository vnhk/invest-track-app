package com.bervan.budget;

import com.vaadin.flow.component.progressbar.ProgressBar;

public class BudgetProgressBar extends ProgressBar {

    public BudgetProgressBar(BudgetCategory category) {
        setMin(0);
        setMax(category.getAssigned().doubleValue());
        setValue(
                category.getAssigned()
                        .subtract(category.getAvailable())
                        .doubleValue()
        );
        setWidth("150px");
    }
}
