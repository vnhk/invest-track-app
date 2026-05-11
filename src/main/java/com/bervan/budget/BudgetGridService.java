package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.bervan.investtrack.service.CurrencyConverter;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BudgetGridService {

    private final BudgetEntryService budgetEntryService;
    private final CurrencyConverter currencyConverter;

    public BudgetGridService(BudgetEntryService budgetEntryService, CurrencyConverter currencyConverter) {
        this.budgetEntryService = budgetEntryService;
        this.currencyConverter = currencyConverter;
    }

    private String getDateRootName(BudgetEntry e) {
        return e.getEntryDate().getMonthValue() + "-" + e.getEntryDate().getYear();
    }

    public List<BudgetEntry> loadAllRecurringLastMonth() {
        SearchRequest request = new SearchRequest();
        request.addCriterion("IS_RECURRING_CRITERIA", BudgetEntry.class, "isRecurring", SearchOperation.EQUALS_OPERATION, true);
        LocalDate now = LocalDate.now();
        LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayLastMonth = firstDayLastMonth.withDayOfMonth(firstDayLastMonth.lengthOfMonth());
        request.addCriterion("DATE_FROM",
                BudgetEntry.class,
                "entryDate",
                SearchOperation.GREATER_EQUAL_OPERATION,
                firstDayLastMonth);

        // entryDate <= lastDayLastMonth
        request.addCriterion("DATE_TO",
                BudgetEntry.class,
                "entryDate",
                SearchOperation.LESS_EQUAL_OPERATION,
                lastDayLastMonth);
        List<BudgetEntry> loaded = budgetEntryService.load(request, Pageable.ofSize(1000000000), "entryDate", SortDirection.DESC);
        return loaded;
    }

    public BudgetEntry getCopyForNewDate(LocalDate newDate, BudgetEntry entry) {
        BudgetEntry newBudgetEntry = new BudgetEntry(null, entry.getName(), false, LocalDateTime.now(), entry.getCategory(),
                entry.getCurrency(), entry.getValue(), newDate, entry.getPaymentMethod(), entry.getEntryType(), entry.getNotes(), entry.getIsRecurring());
        return newBudgetEntry;
    }

    public BudgetEntry getItem(UUID id) {
        return budgetEntryService.findById(id);
    }

    public void copyRecurringToAnotherDate(LocalDate newDate) {
        List<BudgetEntry> budgetEntries = loadAllRecurringLastMonth();
        for (BudgetEntry budgetEntry : budgetEntries) {
            BudgetEntry copyForNewDate = getCopyForNewDate(newDate, budgetEntry);
            budgetEntryService.save(copyForNewDate);
        }
    }

    public List<BudgetEntry> load(Set<UUID> uuids) {
        return budgetEntryService.loadById(uuids);
    }

    public void update(List<BudgetEntry> originalSelected, LocalDate newDate, String newCategory) {
        originalSelected.forEach(e -> {
            if (newDate != null) {
                e.setEntryDate(newDate);
            }
            if (newCategory != null && !newCategory.trim().isEmpty()) {
                e.setCategory(newCategory);
            }

        });
        budgetEntryService.save(originalSelected);
    }
}
