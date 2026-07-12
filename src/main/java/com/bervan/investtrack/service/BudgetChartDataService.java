package com.bervan.investtrack.service;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.TreeSet;

@Service
public class BudgetChartDataService {

    private final BudgetEntryService budgetEntryService;

    public BudgetChartDataService(BudgetEntryService budgetEntryService) {
        this.budgetEntryService = budgetEntryService;
    }

    /**
     * Get monthly income and expense totals
     *
     * @param startDate start of period
     * @param endDate   end of period
     * @return MonthlyBudgetData containing income and expense maps
     */
    public MonthlyBudgetData getMonthlyIncomeExpense(LocalDate startDate, LocalDate endDate) {
        List<BudgetEntry> entries = loadEntries(startDate, endDate);

        Map<String, BigDecimal> incomeByMonth = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByMonth = new LinkedHashMap<>();

        // Initialize all months in range
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String key = formatYearMonth(ym);
            incomeByMonth.put(key, BigDecimal.ZERO);
            expenseByMonth.put(key, BigDecimal.ZERO);
        }

        // Aggregate entries
        for (BudgetEntry entry : entries) {
            if (entry.getEntryDate() == null || entry.getValue() == null) continue;

            String key = formatYearMonth(YearMonth.from(entry.getEntryDate()));
            BigDecimal value = entry.getValue().abs();

            if ("Income".equals(entry.getEntryType())) {
                incomeByMonth.merge(key, value, BigDecimal::add);
            } else {
                expenseByMonth.merge(key, value, BigDecimal::add);
            }
        }

        return new MonthlyBudgetData(incomeByMonth, expenseByMonth);
    }

    /**
     * Get average monthly expense per category for a given year.
     * Average = total / total months in range (months with no entries count as 0).
     */

    private List<BudgetEntry> loadEntries(LocalDate startDate, LocalDate endDate) {
        SearchRequest request = new SearchRequest();
        request.addCriterion("START_DATE", BudgetEntry.class, "entryDate",
                SearchOperation.GREATER_EQUAL_OPERATION, startDate);
        request.addCriterion("END_DATE", BudgetEntry.class, "entryDate",
                SearchOperation.LESS_EQUAL_OPERATION, endDate);

        return budgetEntryService.load(request, Pageable.ofSize(100000), "entryDate", SortDirection.ASC);
    }

    private String formatYearMonth(YearMonth ym) {
        return String.format("%d-%02d", ym.getYear(), ym.getMonthValue());
    }

    public record MonthlyBudgetData(Map<String, BigDecimal> income, Map<String, BigDecimal> expense) {}

}
