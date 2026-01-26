package com.bervan.budget;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class BudgetCategory {

    private String name;
    private BigDecimal assigned;
    private BigDecimal activity;
    private BigDecimal available;

}