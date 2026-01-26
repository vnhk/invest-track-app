package com.bervan.budget;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetEntryService budgetEntryService;

    public BudgetService(BudgetEntryService budgetEntryService) {
        this.budgetEntryService = budgetEntryService;
    }

    private String getDateRootName(BudgetEntry e) {
        return e.getEntryDate().getMonthValue() + "-" + e.getEntryDate().getYear();
    }

    public TreeData<BudgetRow> loadTreeData(LocalDate startDate, LocalDate endDate) {
        TreeData<BudgetRow> treeData = new TreeData<>();

        SearchRequest request = new SearchRequest();
        request.addCriterion("ENTRY_DATE_CRITERIA", BudgetEntry.class, "entryDate", SearchOperation.GREATER_EQUAL_OPERATION, startDate);
        request.addCriterion("ENTRY_DATE_CRITERIA", BudgetEntry.class, "entryDate", SearchOperation.LESS_EQUAL_OPERATION, endDate);
        List<BudgetEntry> loaded = budgetEntryService.load(request, Pageable.ofSize(1000000000), "entryDate", SortDirection.DESC);
        //group by month not by days
        Map<String, List<BudgetEntry>> byDate = loaded.stream().collect(Collectors.groupingBy(e -> getDateRootName(e)));

        for (Map.Entry<String, List<BudgetEntry>> entry : byDate.entrySet()) {
            BudgetRow dateGroup = group(entry.getKey(), "DATE_ROW");
            treeData.addItem(null, dateGroup);
            Map<String, List<BudgetEntry>> byCategory = entry.getValue().stream().collect(Collectors.groupingBy(BudgetEntry::getCategory));

            for (Map.Entry<String, List<BudgetEntry>> category : byCategory.entrySet()) {
                BudgetRow categoryGroup = group(category.getKey(), "CATEGORY_ROW");
                treeData.addItem(dateGroup, categoryGroup);

                for (BudgetEntry budgetEntry : category.getValue()) {
                    treeData.addItem(categoryGroup, item(budgetEntry));
                }
                addNewItemBudgetRow(treeData, categoryGroup, "ITEM_ROW");
            }
            addNewItemBudgetRow(treeData, dateGroup, "CATEGORY_ROW");
        }
        addNewItemBudgetRow(treeData, null, "DATE_ROW");

        return treeData;
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
                name, null, null, null, null, null, null, rowType, true
        );
    }

    private BudgetRow item(BudgetEntry budgetEntry) {
        return new BudgetRow(
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

    public BudgetRow createItemRow(BudgetEntry newItem, BudgetRow date, BudgetRow category) {
        newItem.setCategory(category.getName());
        BudgetEntry saved = budgetEntryService.save(newItem);
        return item(saved);
    }

    private Date parse(BudgetRow date) {
        Date parse;
        try {
            parse = new SimpleDateFormat("dd-MM-yyyy").parse(date.getName());
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
        return parse;
    }
}
