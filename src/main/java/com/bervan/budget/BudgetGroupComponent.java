package com.bervan.budget;

import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class BudgetGroupComponent extends VerticalLayout {

    public BudgetGroupComponent(BudgetGroup group) {
        add(new H4(group.getName()));
        group.getCategories()
                .forEach(cat -> add(new BudgetRowComponent(cat)));
    }
}
