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
     * Get expense trends by category over time
     *
     * @param startDate start of period
     * @param endDate   end of period
     * @return map of category -> (month -> amount)
     */
    public Map<String, Map<String, BigDecimal>> getCategoryTrends(LocalDate startDate, LocalDate endDate) {
        List<BudgetEntry> entries = loadEntries(startDate, endDate);

        // Get all categories
        Set<String> categories = entries.stream()
                .map(BudgetEntry::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Get all months in range
        List<String> months = new ArrayList<>();
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            months.add(formatYearMonth(ym));
        }

        // Initialize result
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        for (String category : categories) {
            Map<String, BigDecimal> categoryData = new LinkedHashMap<>();
            for (String month : months) {
                categoryData.put(month, BigDecimal.ZERO);
            }
            result.put(category, categoryData);
        }

        // Aggregate
        for (BudgetEntry entry : entries) {
            if (entry.getEntryDate() == null || entry.getValue() == null || entry.getCategory() == null) {
                continue;
            }
            // Only count expenses for category trends
            if (!"Expense".equals(entry.getEntryType())) continue;

            String category = entry.getCategory();
            String month = formatYearMonth(YearMonth.from(entry.getEntryDate()));

            if (result.containsKey(category) && result.get(category).containsKey(month)) {
                result.get(category).merge(month, entry.getValue().abs(), BigDecimal::add);
            }
        }

        return result;
    }

    /**
     * Get total balance (income - expense) per month
     *
     * @param startDate start of period
     * @param endDate   end of period
     * @return map of month -> net balance
     */
    public Map<String, BigDecimal> getMonthlyNetBalance(LocalDate startDate, LocalDate endDate) {
        MonthlyBudgetData data = getMonthlyIncomeExpense(startDate, endDate);
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (String month : data.income().keySet()) {
            BigDecimal income = data.income().getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = data.expense().getOrDefault(month, BigDecimal.ZERO);
            result.put(month, income.subtract(expense));
        }

        return result;
    }

    /**
     * Get top expense categories
     *
     * @param startDate start of period
     * @param endDate   end of period
     * @param limit     max number of categories to return
     * @return sorted list of category totals
     */
    public List<CategoryTotal> getTopExpenseCategories(LocalDate startDate, LocalDate endDate, int limit) {
        List<BudgetEntry> entries = loadEntries(startDate, endDate);

        Map<String, BigDecimal> categoryTotals = new HashMap<>();

        for (BudgetEntry entry : entries) {
            if (!"Expense".equals(entry.getEntryType()) || entry.getCategory() == null || entry.getValue() == null) {
                continue;
            }
            categoryTotals.merge(entry.getCategory(), entry.getValue().abs(), BigDecimal::add);
        }

        return categoryTotals.entrySet().stream()
                .map(e -> new CategoryTotal(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .limit(limit)
                .toList();
    }

    /**
     * Get summary statistics for a period
     */
    public BudgetSummary getSummary(LocalDate startDate, LocalDate endDate) {
        MonthlyBudgetData data = getMonthlyIncomeExpense(startDate, endDate);

        BigDecimal totalIncome = data.income().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = data.expense().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        int months = data.income().size();
        BigDecimal avgMonthlyIncome = months > 0 ?
                totalIncome.divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        BigDecimal avgMonthlyExpense = months > 0 ?
                totalExpense.divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        return new BudgetSummary(
                totalIncome, totalExpense, netSavings,
                avgMonthlyIncome, avgMonthlyExpense
        );
    }

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

    public record CategoryTotal(String category, BigDecimal total) {}

    public record BudgetSummary(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netSavings,
            BigDecimal avgMonthlyIncome,
            BigDecimal avgMonthlyExpense
    ) {}
}
