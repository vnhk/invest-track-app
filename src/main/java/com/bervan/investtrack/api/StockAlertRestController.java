package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.common.service.AuthService;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.investtrack.service.StockPriceAlertService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/invest-track/stock-alerts")
public class StockAlertRestController extends BaseOwnedController<StockPriceAlert, UUID> {

    protected StockAlertRestController(StockPriceAlertService service, BervanDTOMapper mapper,
                                       EntityConfigValidator validator) {
        super(service, mapper, validator, "StockPriceAlert");
    }

    @GetMapping
    public ResponseEntity<Page<StockAlertDto>> list(
            @RequestParam MultiValueMap<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return super.search(allParams, page, size, StockAlertDto.class, StockPriceAlert.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockAlertDto> getById(@PathVariable UUID id) {
        return super.getById(id, StockAlertDto.class);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StockAlertCreateRequest req) {
        // manual create — StockPriceAlertConfig is a cascade-persisted OneToOne that needs owner set manually
        StockPriceAlertConfig config = new StockPriceAlertConfig();
        config.setId(UUID.randomUUID());
        config.setPrice(req.getPrice());
        config.setOperator(req.getOperator());
        config.setAmountOfNotifications(req.getAmountOfNotifications() != null ? req.getAmountOfNotifications() : 1);
        config.setCheckIntervalMinutes(req.getCheckIntervalMinutes() != null ? req.getCheckIntervalMinutes() : 60);
        config.setAnotherNotificationEachPercentage(req.getAnotherNotificationEachPercentage() != null ? req.getAnotherNotificationEachPercentage() : 10);
        config.setDeleted(false);
        config.addOwner(AuthService.getLoggedUser().get());

        StockPriceAlert alert = (StockPriceAlert) mapper.map(req);
        alert.setStockPriceAlertConfig(config);
        config.setStockPriceAlert(alert);

        var errors = validator.validateAll("StockPriceAlert", alert);
        if (!errors.isEmpty()) return ResponseEntity.badRequest().body(new com.bervan.investtrack.api.ValidationErrorResponse(errors));

        StockPriceAlert saved = service.save(alert);
        return ResponseEntity.ok(mapper.map(saved, StockAlertDto.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody StockAlertCreateRequest req) {
        // manual update — StockPriceAlert has List<String> emails (@ElementCollection) and nested config
        Optional<StockPriceAlert> match = service.loadById(id);
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        StockPriceAlert alert = match.get();
        if (req.getName() != null) alert.setName(req.getName());
        if (req.getSymbol() != null) alert.setSymbol(req.getSymbol());
        if (req.getExchange() != null) alert.setExchange(req.getExchange());
        if (req.getEmails() != null) alert.setEmails(req.getEmails());

        StockPriceAlertConfig config = alert.getStockPriceAlertConfig();
        if (config == null) {
            config = new StockPriceAlertConfig();
            config.setId(UUID.randomUUID());
            config.setDeleted(false);
            config.addOwner(AuthService.getLoggedUser().get());
            config.setStockPriceAlert(alert);
            alert.setStockPriceAlertConfig(config);
        }
        if (req.getPrice() != null) config.setPrice(req.getPrice());
        if (req.getOperator() != null) config.setOperator(req.getOperator());
        if (req.getAmountOfNotifications() != null) config.setAmountOfNotifications(req.getAmountOfNotifications());
        if (req.getCheckIntervalMinutes() != null) config.setCheckIntervalMinutes(req.getCheckIntervalMinutes());
        if (req.getAnotherNotificationEachPercentage() != null) config.setAnotherNotificationEachPercentage(req.getAnotherNotificationEachPercentage());

        StockPriceAlert saved = service.save(alert);
        return ResponseEntity.ok(mapper.map(saved, StockAlertDto.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return super.delete(id);
    }
}
