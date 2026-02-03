package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetGridService {

    private final BudgetEntryService budgetEntryService;

    public BudgetGridService(BudgetEntryService budgetEntryService) {
        this.budgetEntryService = budgetEntryService;
    }

    private String getDateRootName(BudgetEntry e) {
        return e.getEntryDate().getMonthValue() + "-" + e.getEntryDate().getYear();
    }

    public List<BudgetEntry> loadAllRecurring() {
        SearchRequest request = new SearchRequest();
        request.addCriterion("IS_RECURRING_CRITERIA", BudgetEntry.class, "isRecurring", SearchOperation.EQUALS_OPERATION, true);
        List<BudgetEntry> loaded = budgetEntryService.load(request, Pageable.ofSize(1000000000), "entryDate", SortDirection.DESC);
        return loaded;
    }

    public TreeData<BudgetRow> loadTreeData(LocalDate startDate, LocalDate endDate) {
        TreeData<BudgetRow> treeData = new TreeData<>();

        SearchRequest request = new SearchRequest();
        request.addCriterion("ENTRY_DATE_CRITERIA", BudgetEntry.class, "entryDate", SearchOperation.GREATER_EQUAL_OPERATION, startDate);
        request.addCriterion("ENTRY_DATE_CRITERIA", BudgetEntry.class, "entryDate", SearchOperation.LESS_EQUAL_OPERATION, endDate);
        List<BudgetEntry> loaded = budgetEntryService.load(request, Pageable.ofSize(1000000000), "entryDate", SortDirection.DESC);

        //group by month not by day of month
        Map<String, List<BudgetEntry>> byDate = loaded.stream().collect(Collectors.groupingBy(e -> getDateRootName(e)));

        // Sort dates descending (newest first)
        List<String> sortedDates = new ArrayList<>(byDate.keySet());
        sortedDates.sort((d1, d2) -> {
            // Format is "month-year" e.g. "1-2025"
            String[] parts1 = d1.split("-");
            String[] parts2 = d2.split("-");
            int year1 = Integer.parseInt(parts1[1]);
            int year2 = Integer.parseInt(parts2[1]);
            int month1 = Integer.parseInt(parts1[0]);
            int month2 = Integer.parseInt(parts2[0]);
            // Compare year first, then month (descending)
            if (year1 != year2) {
                return year2 - year1;
            }
            return month2 - month1;
        });

        for (String dateKey : sortedDates) {
            List<BudgetEntry> entries = byDate.get(dateKey);
            Map.Entry<String, List<BudgetEntry>> entry = Map.entry(dateKey, entries);
            BigDecimal dateSumOfAmounts = BigDecimal.ZERO;
            BudgetRow dateGroup = group(entry.getKey(), "DATE_ROW");

            treeData.addItem(null, dateGroup);
            Map<String, List<BudgetEntry>> byCategory = entry.getValue().stream().collect(Collectors.groupingBy(BudgetEntry::getCategory));

            for (Map.Entry<String, List<BudgetEntry>> category : byCategory.entrySet()) {
                BudgetRow categoryGroup = group(category.getKey(), "CATEGORY_ROW");
                treeData.addItem(dateGroup, categoryGroup);
                BigDecimal categorySumOfAmounts = BigDecimal.ZERO;

                for (BudgetEntry budgetEntry : category.getValue()) {
                    BudgetRow item = item(budgetEntry);
                    treeData.addItem(categoryGroup, item);
                    //sum all items in a category
                    categorySumOfAmounts = appendBudgetEntryAmount(item, categorySumOfAmounts);
                }
                //for each category update money details
                updateMoneyDetailsForCategory(categoryGroup, categorySumOfAmounts);
                addNewItemBudgetRow(treeData, categoryGroup, "ITEM_ROW");

                //sum all category expenses/incomes for a date
                dateSumOfAmounts = appendBudgetEntryAmount(categoryGroup, dateSumOfAmounts);
            }
            // for each date update money details
            updateMoneyDetailsForCategory(dateGroup, dateSumOfAmounts);

            addNewItemBudgetRow(treeData, dateGroup, "CATEGORY_ROW");
        }
        addNewItemBudgetRow(treeData, null, "DATE_ROW");

        return treeData;
    }

    private void updateMoneyDetailsForCategory(BudgetRow categoryGroup, BigDecimal sumOfAmounts) {
        categoryGroup.setAmount(sumOfAmounts);
        categoryGroup.setCurrency("PLN");//to be converted to one currency later
        if (sumOfAmounts.compareTo(BigDecimal.ZERO) > 0) {
            categoryGroup.setEntryType("Income");
        } else {
            categoryGroup.setAmount(categoryGroup.getAmount().multiply(BigDecimal.valueOf(-1)));
            categoryGroup.setEntryType("Expense");
        }
    }

    private BigDecimal appendBudgetEntryAmount(BudgetRow budgetEntry, BigDecimal sumOfAmounts) {
        if (budgetEntry.getAmount() != null) {
            if (budgetEntry.getEntryType().equals("Expense")) {
                sumOfAmounts = sumOfAmounts.subtract(budgetEntry.getAmount());
            } else {
                sumOfAmounts = sumOfAmounts.add(budgetEntry.getAmount());
            }
        }
        return sumOfAmounts;
    }

    public void addNewItemBudgetRow(TreeData<BudgetRow> treeData, BudgetRow parent, String rowType) {
        BudgetRow item = item(new BudgetEntry("+"));
        item.setRowType(rowType);
        treeData.addItem(parent, item);
    }

    public void addNewGroupBudgetRow(TreeData<BudgetRow> treeData, BudgetRow parent, String rowType) {
        BudgetRow item = group("+", rowType);
        treeData.addItem(parent, item);
    }

    private BudgetRow group(String name, String rowType) {
        return new BudgetRow(
                null, name, null, null, null, null, null, null, rowType, true
        );
    }

    public BudgetRow item(BudgetEntry budgetEntry) {
        return new BudgetRow(
                budgetEntry.getId(),
                budgetEntry.getName(),
                budgetEntry.getEntryType(),
                budgetEntry.getPaymentMethod(),
                budgetEntry.getEntryDate() != null ? budgetEntry.getEntryDate().format(DateTimeFormatter.ofPattern("dd.MM")) : null,
                budgetEntry.getNotes(),
                budgetEntry.getValue(),
                budgetEntry.getCurrency(),
                "ITEM_ROW",
                false
        );
    }

    public BudgetRow createDateRow(String dateStr) {
        return group(dateStr, "DATE_ROW");
    }

    public BudgetRow createCategoryRow(String category) {
        return group(category, "CATEGORY_ROW");
    }

    public void copyToMonth(BudgetRow row, LocalDate newDate) {
        if (row.getId() == null) {
            return;
        }

        BudgetEntry entry = budgetEntryService.findById(row.getId());
        BudgetEntry newBudgetEntry = getCopyForNewDate(newDate, entry);
        budgetEntryService.save(newBudgetEntry);
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
        List<BudgetEntry> budgetEntries = loadAllRecurring();
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
