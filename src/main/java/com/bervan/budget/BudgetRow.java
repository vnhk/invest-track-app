package com.bervan.budget;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class BudgetRow {

    private String name;
    private String entryType;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String rowType;
    private boolean group;
}
