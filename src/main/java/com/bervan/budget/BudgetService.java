package com.bervan.budget;

import com.vaadin.flow.data.provider.hierarchy.TreeData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BudgetService {

    public List<BudgetGroup> loadBudget() {
        return List.of(
                billsGroup(),
                loanGroup(),
                funGroup()
        );
    }

    private BudgetGroup billsGroup() {
        return new BudgetGroup(
                "Bills",
                List.of(
                        new BudgetCategory("Rent",
                                new BigDecimal("1600"),
                                new BigDecimal("-1600"),
                                BigDecimal.ZERO,
                                CategoryStatus.FULLY_SPENT
                        ),
                        new BudgetCategory("Groceries",
                                new BigDecimal("400"),
                                new BigDecimal("-225"),
                                new BigDecimal("175"),
                                CategoryStatus.FUNDED
                        ),
                        new BudgetCategory("Electric",
                                new BigDecimal("85"),
                                BigDecimal.ZERO,
                                new BigDecimal("85"),
                                CategoryStatus.FUNDED
                        ),
                        new BudgetCategory("Water Bill",
                                new BigDecimal("30"),
                                BigDecimal.ZERO,
                                new BigDecimal("30"),
                                CategoryStatus.FUNDED
                        ),
                        new BudgetCategory("Internet Bill",
                                new BigDecimal("50"),
                                BigDecimal.ZERO,
                                new BigDecimal("50"),
                                CategoryStatus.FUNDED
                        ),
                        new BudgetCategory("Phone",
                                new BigDecimal("70"),
                                BigDecimal.ZERO,
                                new BigDecimal("70"),
                                CategoryStatus.FUNDED
                        )
                )
        );
    }

    private BudgetGroup loanGroup() {
        return new BudgetGroup(
                "Loan Payments",
                List.of(
                        new BudgetCategory("Student Loan",
                                new BigDecimal("250.34"),
                                BigDecimal.ZERO,
                                new BigDecimal("250.34"),
                                CategoryStatus.FUNDED
                        ),
                        new BudgetCategory("Car Payment",
                                new BigDecimal("200"),
                                BigDecimal.ZERO,
                                new BigDecimal("200"),
                                CategoryStatus.FUNDED
                        )
                )
        );
    }

    private BudgetGroup funGroup() {
        return new BudgetGroup(
                "Just for Fun",
                List.of(
                        new BudgetCategory("Dining Out",
                                new BigDecimal("200"),
                                new BigDecimal("-120"),
                                new BigDecimal("80"),
                                CategoryStatus.FUNDED
                        ),
                        new BudgetCategory("TV",
                                new BigDecimal("40"),
                                new BigDecimal("-40"),
                                BigDecimal.ZERO,
                                CategoryStatus.FULLY_SPENT
                        ),
                        new BudgetCategory("Gaming",
                                new BigDecimal("20"),
                                BigDecimal.ZERO,
                                new BigDecimal("20"),
                                CategoryStatus.FUNDED
                        )
                )
        );
    }

    public TreeData<BudgetRow> loadTreeData() {

        TreeData<BudgetRow> data = new TreeData<>();

        BudgetRow bills = group("Bills");
        data.addItem(null, bills);

        data.addItem(bills, item("Rent", 1600, -1600, 0));
        data.addItem(bills, item("Groceries", 400, -225, 175));
        data.addItem(bills, item("Electric", 85, 0, 85));

        BudgetRow loans = group("Loan Payments");
        data.addItem(null, loans);

        data.addItem(loans, item("Student Loan", 250.34, 0, 250.34));
        data.addItem(loans, item("Car Payment", 200, 0, 200));

        return data;
    }
    private BudgetRow group(String name) {
        return new BudgetRow(
                name,
                true,           // group
                null,           // assigned
                null,           // activity
                null,           // available
                null            // status
        );
    }

    private BudgetRow item(String name,
                           double assigned,
                           double activity,
                           double available) {

        BigDecimal assignedBd = BigDecimal.valueOf(assigned);
        BigDecimal activityBd = BigDecimal.valueOf(activity);
        BigDecimal availableBd = BigDecimal.valueOf(available);

        return new BudgetRow(
                name,
                false,          // not a group
                assignedBd,
                activityBd,
                availableBd,
                resolveStatus(assignedBd, availableBd)
        );
    }
    private CategoryStatus resolveStatus(BigDecimal assigned,
                                         BigDecimal available) {

        if (available.signum() == 0) {
            return CategoryStatus.FULLY_SPENT;
        }
        if (available.compareTo(assigned) < 0) {
            return CategoryStatus.FUNDED;
        }
        return CategoryStatus.UNDERFUNDED;
    }
}
