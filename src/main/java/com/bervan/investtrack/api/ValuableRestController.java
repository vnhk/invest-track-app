package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.ImportResult;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.investtrack.model.Valuable;
import com.bervan.investtrack.service.ValuableService;
import com.bervan.logging.JsonLogger;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/invest-track/valuable")
public class ValuableRestController extends BaseOwnedController<Valuable, UUID> {
    private JsonLogger log = JsonLogger.getLogger(ValuableRestController.class, "invest-track");

    protected ValuableRestController(ValuableService service,
                                     BervanDTOMapper mapper,
                                     EntityConfigValidator validator) {
        super(service, mapper, validator, "Valuable");
    }

    @GetMapping
    public ResponseEntity<Page<ValuableResponseDto>> list(
            @RequestParam MultiValueMap<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return super.search(allParams, page, size, ValuableResponseDto.class, Valuable.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ValuableResponseDto> getById(@PathVariable UUID id) {
        return super.getById(id, ValuableResponseDto.class);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ValuableCreateRequest req) {
        return super.create(req, ValuableResponseDto.class);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ValuableCreateRequest req) {
        req.setId(id);
        return super.update(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return super.delete(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam MultiValueMap<String, String> allParams) {
        return super.exportAll(allParams, ValuableResponseDto.class, "valuables", Valuable.class);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importData(@RequestParam("file") MultipartFile file) {
        return super.importAll(file, ValuableCreateRequest.class);
    }
}
