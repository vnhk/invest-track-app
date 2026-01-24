package com.bervan.budget;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class BudgetHeader extends HorizontalLayout {

    public BudgetHeader() {
        add(
                new H2("This Month"),
                new Span("$300.00 Ready to Assign"),
                new Button("Assign")
        );
        setWidthFull();
        setJustifyContentMode(JustifyContentMode.BETWEEN);
    }
}
