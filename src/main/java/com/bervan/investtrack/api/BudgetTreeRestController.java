package com.bervan.investtrack.api;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.bervan.investtrack.service.CurrencyConverter;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invest-track/budget-tree")
@RolesAllowed({"USER"})
public class BudgetTreeRestController {

    private final BudgetEntryService budgetEntryService;
    private final CurrencyConverter currencyConverter;

    public BudgetTreeRestController(BudgetEntryService budgetEntryService, CurrencyConverter currencyConverter) {
        this.budgetEntryService = budgetEntryService;
        this.currencyConverter = currencyConverter;
    }

    record EntryDto(UUID id, String name, String entryType, String paymentMethod,
                    String entryDate, String notes, BigDecimal amount, String currency,
                    Boolean isRecurring) {}

    record CategoryDto(String name, BigDecimal totalPln, String entryType, List<EntryDto> items) {}

    record MonthDto(String key, String label, BigDecimal totalPln, String entryType, List<CategoryDto> categories) {}

    @GetMapping
    public ResponseEntity<List<MonthDto>> getTree(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfYear(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        SearchRequest request = new SearchRequest();
        request.addCriterion("DATE_FROM", BudgetEntry.class, "entryDate", SearchOperation.GREATER_EQUAL_OPERATION, start);
        request.addCriterion("DATE_TO", BudgetEntry.class, "entryDate", SearchOperation.LESS_EQUAL_OPERATION, end);

        List<BudgetEntry> entries = budgetEntryService.load(request, Pageable.ofSize(1_000_000), "entryDate", SortDirection.DESC);

        Map<String, List<BudgetEntry>> byMonth = entries.stream()
                .filter(e -> e.getEntryDate() != null)
                .collect(Collectors.groupingBy(e -> e.getEntryDate().getMonthValue() + "-" + e.getEntryDate().getYear()));

        List<String> sortedKeys = new ArrayList<>(byMonth.keySet());
        sortedKeys.sort((a, b) -> {
            String[] pa = a.split("-"), pb = b.split("-");
            int ya = Integer.parseInt(pa[1]), yb = Integer.parseInt(pb[1]);
            int ma = Integer.parseInt(pa[0]), mb = Integer.parseInt(pb[0]);
            return ya != yb ? yb - ya : mb - ma;
        });

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

        List<MonthDto> result = new ArrayList<>();
        for (String key : sortedKeys) {
            List<BudgetEntry> monthEntries = byMonth.get(key);

            Map<String, List<BudgetEntry>> byCategory = monthEntries.stream()
                    .collect(Collectors.groupingBy(e -> e.getCategory() != null ? e.getCategory() : "Uncategorized"));

            List<CategoryDto> categories = new ArrayList<>();
            BigDecimal monthTotal = BigDecimal.ZERO;

            for (Map.Entry<String, List<BudgetEntry>> catEntry : byCategory.entrySet()) {
                List<EntryDto> items = catEntry.getValue().stream()
                        .map(e -> new EntryDto(
                                e.getId(), e.getName(), e.getEntryType(), e.getPaymentMethod(),
                                e.getEntryDate() != null ? e.getEntryDate().format(DateTimeFormatter.ofPattern("dd.MM")) : null,
                                e.getNotes(), e.getValue(), e.getCurrency(), e.getIsRecurring()))
                        .collect(Collectors.toList());

                BigDecimal catTotal = BigDecimal.ZERO;
                for (BudgetEntry e : catEntry.getValue()) {
                    catTotal = catTotal.add(toPlnSigned(e));
                }
                monthTotal = monthTotal.add(catTotal);

                BigDecimal displayCatTotal = catTotal.abs().setScale(2, RoundingMode.HALF_UP);
                String catType = catTotal.compareTo(BigDecimal.ZERO) >= 0 ? "Income" : "Expense";
                categories.add(new CategoryDto(catEntry.getKey(), displayCatTotal, catType, items));
            }

            categories.sort(Comparator.comparing(CategoryDto::name));

            BigDecimal displayMonthTotal = monthTotal.abs().setScale(2, RoundingMode.HALF_UP);
            String monthType = monthTotal.compareTo(BigDecimal.ZERO) >= 0 ? "Income" : "Expense";

            // build label from key "month-year"
            String[] parts = key.split("-");
            LocalDate firstOfMonth = LocalDate.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]), 1);
            String label = firstOfMonth.format(monthFmt);

            result.add(new MonthDto(key, label, displayMonthTotal, monthType, categories));
        }

        return ResponseEntity.ok(result);
    }

    private BigDecimal toPlnSigned(BudgetEntry e) {
        if (e.getValue() == null) return BigDecimal.ZERO;
        BigDecimal pln = toPlnSafe(e.getValue(), e.getCurrency());
        return "Expense".equals(e.getEntryType()) ? pln.negate() : pln;
    }

    private BigDecimal toPlnSafe(BigDecimal amount, String currency) {
        if (currency == null || "PLN".equals(currency)) return amount;
        try {
            return currencyConverter.convert(amount, CurrencyConverter.Currency.of(currency), CurrencyConverter.Currency.PLN);
        } catch (Exception e) {
            return amount;
        }
    }
}
