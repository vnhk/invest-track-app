package com.bervan.budget;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class BudgetRowComponent extends HorizontalLayout {

    public BudgetRowComponent(BudgetCategory category) {

        add(
                new Span(category.getName()),
                new BudgetProgressBar(category),
                new Span(category.getAssigned().toString()),
                new Span(category.getActivity().toString()),
                new Span(category.getAvailable().toString())
        );

        setWidthFull();
        setAlignItems(Alignment.CENTER);
    }
}
