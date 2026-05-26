package com.bervan.investtrack.api;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.ValidationErrorResponse;
import com.bervan.investtrack.service.ReceiptScanningService;
import com.bervan.logging.JsonLogger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invest-track/budget-entries")
public class BudgetEntryRestController {

    private final BudgetEntryService budgetEntryService;
    private final EntityConfigValidator validator;
    private final ReceiptScanningService receiptScanningService;
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");

    public BudgetEntryRestController(BudgetEntryService budgetEntryService,
                                     EntityConfigValidator validator,
                                     ReceiptScanningService receiptScanningService) {
        this.budgetEntryService = budgetEntryService;
        this.validator = validator;
        this.receiptScanningService = receiptScanningService;
    }

    // Use top-level DTO class BudgetEntryDto and common ValidationErrorResponse

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
                .sorted(Comparator.comparing(BudgetEntryDto::getEntryDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int total = dtos.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return ResponseEntity.ok(new PageImpl<>(dtos.subList(from, to), PageRequest.of(page, size), total));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = getAvailableCategories();
        return ResponseEntity.ok(categories);
    }

    private List<String> getAvailableCategories() {
        Set<BudgetEntry> all = budgetEntryService.load(PageRequest.of(0, Integer.MAX_VALUE));
        List<String> categories = all.stream()
                .map(BudgetEntry::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        return categories;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BudgetEntryDto req) {
        // map DTO -> entity, validate, save
        BudgetEntry model = new BudgetEntry();
        if (req.getId() != null) model.setId(req.getId());
        else model.setId(UUID.randomUUID());
        applyFieldsFromDto(model, req);
        Map<String, Object> fields = new HashMap<>();
        if (req.getName() != null) fields.put("name", req.getName());
        if (req.getCategory() != null) fields.put("category", req.getCategory());
        if (req.getCurrency() != null) fields.put("currency", req.getCurrency());
        if (req.getValue() != null) fields.put("value", req.getValue());
        if (req.getEntryDate() != null) fields.put("entryDate", req.getEntryDate().toString());
        if (req.getPaymentMethod() != null) fields.put("paymentMethod", req.getPaymentMethod());
        if (req.getEntryType() != null) fields.put("entryType", req.getEntryType());
        if (req.getNotes() != null) fields.put("notes", req.getNotes());
        if (req.getIsRecurring() != null) fields.put("isRecurring", req.getIsRecurring());
        List<EntityConfigValidator.FieldError> errors = validator.validate("BudgetEntry", fields);
        if (!errors.isEmpty())
            return ResponseEntity.badRequest().body(new com.bervan.common.controller.ValidationErrorResponse(errors));
        model.setModificationDate(LocalDateTime.now());
        model.setDeleted(false);
        BudgetEntry saved = budgetEntryService.save(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody BudgetEntryDto req) {
        if (req.getId() == null) req.setId(id);
        Optional<BudgetEntry> match = budgetEntryService.load(PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(e -> e.getId().equals(id)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        BudgetEntry entry = match.get();
        applyFieldsFromDto(entry, req);
        Map<String, Object> fields2 = new HashMap<>();
        if (req.getName() != null) fields2.put("name", req.getName());
        if (req.getCategory() != null) fields2.put("category", req.getCategory());
        if (req.getCurrency() != null) fields2.put("currency", req.getCurrency());
        if (req.getValue() != null) fields2.put("value", req.getValue());
        if (req.getEntryDate() != null) fields2.put("entryDate", req.getEntryDate().toString());
        if (req.getPaymentMethod() != null) fields2.put("paymentMethod", req.getPaymentMethod());
        if (req.getEntryType() != null) fields2.put("entryType", req.getEntryType());
        if (req.getNotes() != null) fields2.put("notes", req.getNotes());
        if (req.getIsRecurring() != null) fields2.put("isRecurring", req.getIsRecurring());
        List<EntityConfigValidator.FieldError> errors2 = validator.validate("BudgetEntry", fields2);
        if (!errors2.isEmpty())
            return ResponseEntity.badRequest().body(new com.bervan.common.controller.ValidationErrorResponse(errors2));
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

    @PostMapping("/scan-receipt")
    public ResponseEntity<?> scanReceipt(@RequestBody ReceiptScanRequest req) {
        if (req.base64Image == null || req.base64Image.isBlank()) {
            return ResponseEntity.badRequest().body(new ValidationErrorResponse(
                    List.of(new EntityConfigValidator.FieldError("base64Image", "Image data is required"))
            ));
        }

        List<String> categories = getAvailableCategories();

        List<ReceiptScanningService.ParsedReceiptEntry> parsed = new ArrayList<>();
        try {
            parsed = receiptScanningService.scanReceipt(req.base64Image, categories);
        } catch (IOException e) {
            log.error("Failed to scan receipt", e);
        }

        if (parsed.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }


        List<BudgetEntryDto> result = parsed.stream().map(p -> {
            LocalDate entryDate = p.getDate() != null ? p.getDate() : (req.entryDate != null ? req.entryDate : LocalDate.now());

            BudgetEntry entry = new BudgetEntry();
            // entry.setId(UUID.randomUUID()); // Do not set ID for preview
            entry.setName(p.getName() != null ? p.getName() : "Receipt item");
            entry.setCategory(p.getCategory() != null ? p.getCategory() : "Uncategorized");
            entry.setCurrency(p.getCurrency() != null ? p.getCurrency() : "PLN");
            entry.setValue(p.getValue() != null ? p.getValue() : BigDecimal.ZERO);
            entry.setEntryDate(entryDate);
            entry.setPaymentMethod("Card");
            entry.setEntryType("Expense");
            entry.setIsRecurring(false);
            entry.setNotes(p.getNotes());
            entry.setModificationDate(LocalDateTime.now());
            entry.setDeleted(false);
            // Do not persist, just return DTO
            return toDto(entry);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private void applyFieldsFromDto(BudgetEntry entry, BudgetEntryDto req) {
        if (req.getName() != null) entry.setName(req.getName());
        if (req.getCategory() != null) entry.setCategory(req.getCategory());
        if (req.getCurrency() != null) entry.setCurrency(req.getCurrency());
        if (req.getValue() != null) entry.setValue(req.getValue());
        if (req.getEntryDate() != null) entry.setEntryDate(req.getEntryDate());
        if (req.getPaymentMethod() != null) entry.setPaymentMethod(req.getPaymentMethod());
        if (req.getEntryType() != null) entry.setEntryType(req.getEntryType());
        if (req.getNotes() != null) entry.setNotes(req.getNotes());
        if (req.getIsRecurring() != null) entry.setIsRecurring(req.getIsRecurring());
    }

    public static class ReceiptScanRequest {
        public String base64Image;
        public LocalDate entryDate;

        public ReceiptScanRequest() {
        }

        public ReceiptScanRequest(String base64Image, LocalDate entryDate) {
            this.base64Image = base64Image;
            this.entryDate = entryDate;
        }

        public String getBase64Image() {
            return base64Image;
        }

        public LocalDate getEntryDate() {
            return entryDate;
        }
    }
}
