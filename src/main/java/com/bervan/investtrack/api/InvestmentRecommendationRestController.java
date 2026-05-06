package com.bervan.investtrack.api;

import com.bervan.investments.recommendation.InvestmentRecommendation;
import com.bervan.investments.recommendation.InvestmentRecommendationService;
import com.bervan.common.config.EntityConfigValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/invest-track/recommendations")
public class InvestmentRecommendationRestController {

    private final InvestmentRecommendationService recommendationService;
    private final EntityConfigValidator validator;

    public InvestmentRecommendationRestController(InvestmentRecommendationService recommendationService,
                                                   EntityConfigValidator validator) {
        this.recommendationService = recommendationService;
        this.validator = validator;
    }

    record RecommendationDto(
            UUID id, String symbol, String strategy, BigDecimal changeInPercentMorning,
            BigDecimal changeInPercentEvening, String date, String recommendationType,
            String recommendationResult, LocalDateTime modificationDate
    ) {}

    record ValidationErrorResponse(List<EntityConfigValidator.FieldError> errors) {}

    private RecommendationDto toDto(InvestmentRecommendation r) {
        return new RecommendationDto(r.getId(), r.getSymbol(), r.getStrategy(),
                r.getChangeInPercentMorning(), r.getChangeInPercentEvening(),
                r.getDate(), r.getRecommendationType(), r.getRecommendationResult(),
                r.getModificationDate());
    }

    @GetMapping
    public ResponseEntity<Page<RecommendationDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String strategy,
            @RequestParam(required = false) String recommendationType,
            @RequestParam(required = false) String recommendationResult,
            @RequestParam(required = false) String date
    ) {

        Set<InvestmentRecommendation> all = recommendationService.load(PageRequest.of(0, Integer.MAX_VALUE));
        List<RecommendationDto> dtos = all.stream()
                .filter(r -> symbol == null || symbol.equalsIgnoreCase(r.getSymbol()))
                .filter(r -> strategy == null || strategy.equals(r.getStrategy()))
                .filter(r -> recommendationType == null || recommendationType.equals(r.getRecommendationType()))
                .filter(r -> recommendationResult == null || recommendationResult.equals(r.getRecommendationResult()))
                .filter(r -> date == null || date.equals(r.getDate()))
                .map(this::toDto)
                .sorted("desc".equalsIgnoreCase(direction)
                        ? Comparator.comparing(RecommendationDto::date, Comparator.nullsLast(Comparator.<String>reverseOrder()))
                        : Comparator.comparing(RecommendationDto::date, Comparator.nullsLast(Comparator.<String>naturalOrder())))
                .toList();

        int total = dtos.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return ResponseEntity.ok(new PageImpl<>(dtos.subList(from, to), PageRequest.of(page, size), total));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> req) {

        List<EntityConfigValidator.FieldError> errors = validator.validate("InvestmentRecommendation", req);
        if (!errors.isEmpty()) return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors));

        InvestmentRecommendation rec = new InvestmentRecommendation();
        rec.setId(UUID.randomUUID());
        applyFields(rec, req);
        rec.setModificationDate(LocalDateTime.now());
        rec.setDeleted(false);
        InvestmentRecommendation saved = recommendationService.save(rec);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Map<String, Object> req) {

        List<EntityConfigValidator.FieldError> errors = validator.validate("InvestmentRecommendation", req);
        if (!errors.isEmpty()) return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors));

        Optional<InvestmentRecommendation> match = recommendationService.load(PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(r -> r.getId().equals(id)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        InvestmentRecommendation rec = match.get();
        applyFields(rec, req);
        rec.setModificationDate(LocalDateTime.now());
        InvestmentRecommendation saved = recommendationService.save(rec);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Optional<InvestmentRecommendation> match = recommendationService.load(PageRequest.of(0, Integer.MAX_VALUE)).stream()
                .filter(r -> r.getId().equals(id)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();
        recommendationService.delete(match.get());
        return ResponseEntity.noContent().build();
    }

    private void applyFields(InvestmentRecommendation rec, Map<String, Object> req) {
        if (req.containsKey("symbol")) rec.setSymbol((String) req.get("symbol"));
        if (req.containsKey("strategy")) rec.setStrategy((String) req.get("strategy"));
        if (req.containsKey("changeInPercentMorning"))
            rec.setChangeInPercentMorning(new BigDecimal(req.get("changeInPercentMorning").toString()));
        if (req.containsKey("changeInPercentEvening"))
            rec.setChangeInPercentEvening(new BigDecimal(req.get("changeInPercentEvening").toString()));
        if (req.containsKey("date")) rec.setDate((String) req.get("date"));
        if (req.containsKey("recommendationType")) rec.setRecommendationType((String) req.get("recommendationType"));
        if (req.containsKey("recommendationResult")) rec.setRecommendationResult((String) req.get("recommendationResult"));
    }
}
