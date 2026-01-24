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
    private boolean group;
    private BigDecimal assigned;
    private BigDecimal activity;
    private BigDecimal available;
    private CategoryStatus status;

}
