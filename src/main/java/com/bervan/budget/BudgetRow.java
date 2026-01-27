package com.bervan.budget;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class BudgetRow {

    private UUID id;
    private String name;
    private String entryType;
    private String paymentMethod;
    private String entryDate;
    private String notes;
    private BigDecimal amount;
    private String currency;
    private String rowType;
    private boolean group;
}
