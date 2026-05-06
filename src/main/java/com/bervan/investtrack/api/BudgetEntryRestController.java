package com.bervan.investtrack.api;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.config.EntityConfigValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/invest-track/budget-entries")
public class BudgetEntryRestController {

    private final BudgetEntryService budgetEntryService;
    private final EntityConfigValidator validator;

    public BudgetEntryRestController(BudgetEntryService budgetEntryService, EntityConfigValidator validator) {
        this.budgetEntryService = budgetEntryService;
        this.validator = validator;
    }

    record BudgetEntryDto(
            UUID id, String name, String category, String currency, BigDecimal value,
            LocalDate entryDate, String paymentMethod, String entryType, String notes,
            Boolean isRecurring, LocalDateTime modificationDate
    ) {}

    record ValidationErrorResponse(List<EntityConfigValidator.FieldError> errors) {}

    private BudgetEntryDto toDto(BudgetEntry e) {
        return new BudgetEntryDto(e.getId(), e.getName(), e.getCategory(), e.getCurrency(), e.getValue(),
                e.getEntryDate(), e.getPaymentMethod(), e.getEntryType(), e.getNotes(),
                e.getIsRecurring(), e.getModificationDate());
    }

    @GetMapping
    public ResponseEntity<Page<BudgetEntryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "entryDate") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {

        Set<BudgetEntry> all = budgetEntryService.load(PageRequest.of(0, Integer.MAX_VALUE));
        List<BudgetEntryDto> dtos = all.stream()
                .filter(e -> category == null || category.equals(e.getCategory()))
                .filter(e -> entryType == null || entryType.equals(e.getEntryType()))
                .filter(e -> dateFrom == null || (e.getEntryDate() != null && !e.getEntryDate().isBefore(LocalDate.parse(dateFrom))))
                .filter(e -> dateTo == null || (e.getEntryDate() != null && !e.getEntryDate().isAfter(LocalDate.parse(dateTo))))
                .map(this::toDto)
                .sorted(Comparator.comparing(BudgetEntryDto::entryDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = dtos.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return ResponseEntity.ok(new PageImpl<>(dtos.subList(from, to), PageRequest.of(page, size), total));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        Set<BudgetEntry> all = budgetEntryService.load(PageRequest.of(0, Integer.MAX_VALUE));
        List<String> categories = all.stream()
                .map(BudgetEntry::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> req) {

        List<EntityConfigValidator.FieldError> errors = validator.validate("BudgetEntry", req);
        if (!errors.isEmpty()) return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors));

        BudgetEntry entry = new BudgetEntry();
        entry.setId(UUID.randomUUID());
        applyFields(entry, req);
        entry.setModificationDate(LocalDateTime.now());
        entry.setDeleted(false);
        BudgetEntry saved = budgetEntryService.save(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Map<String, Object> req) {

        List<EntityConfigValidator.FieldError> errors = validator.validate("BudgetEntry", req);
        if (!errors.isEmpty()) return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors));

        Optional<BudgetEntry> match = budgetEntryService.load(PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(e -> e.getId().equals(id)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        BudgetEntry entry = match.get();
        applyFields(entry, req);
        entry.setModificationDate(LocalDateTime.now());
        BudgetEntry saved = budgetEntryService.save(entry);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Optional<BudgetEntry> match = budgetEntryService.load(PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(e -> e.getId().equals(id)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();
        budgetEntryService.delete(match.get());
        return ResponseEntity.noContent().build();
    }

    private void applyFields(BudgetEntry entry, Map<String, Object> req) {
        if (req.containsKey("name")) entry.setName((String) req.get("name"));
        if (req.containsKey("category")) entry.setCategory((String) req.get("category"));
        if (req.containsKey("currency")) entry.setCurrency((String) req.get("currency"));
        if (req.containsKey("value")) entry.setValue(new BigDecimal(req.get("value").toString()));
        if (req.containsKey("entryDate")) entry.setEntryDate(LocalDate.parse((String) req.get("entryDate")));
        if (req.containsKey("paymentMethod")) entry.setPaymentMethod((String) req.get("paymentMethod"));
        if (req.containsKey("entryType")) entry.setEntryType((String) req.get("entryType"));
        if (req.containsKey("notes")) entry.setNotes((String) req.get("notes"));
        if (req.containsKey("isRecurring")) entry.setIsRecurring((Boolean) req.get("isRecurring"));
    }
}
