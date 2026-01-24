package com.bervan.budget;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BudgetGroup {

    private String name;
    private List<BudgetCategory> categories;
}
