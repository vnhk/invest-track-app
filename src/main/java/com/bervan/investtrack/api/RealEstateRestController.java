package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.ImportResult;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.investtrack.model.RealEstate;
import com.bervan.investtrack.service.RealEstateService;
import com.bervan.logging.JsonLogger;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/invest-track/real-estate")
public class RealEstateRestController extends BaseOwnedController<RealEstate, UUID> {
    private JsonLogger log = JsonLogger.getLogger(RealEstateRestController.class, "invest-track");

    protected RealEstateRestController(RealEstateService service,
                                       BervanDTOMapper mapper,
                                       EntityConfigValidator validator) {
        super(service, mapper, validator, "RealEstate");
    }

    @GetMapping
    public ResponseEntity<Page<RealEstateResponseDto>> list(
            @RequestParam MultiValueMap<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return super.search(allParams, page, size, RealEstateResponseDto.class, RealEstate.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RealEstateResponseDto> getById(@PathVariable UUID id) {
        return super.getById(id, RealEstateResponseDto.class);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RealEstateCreateRequest req) {
        return super.create(req, RealEstateResponseDto.class);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody RealEstateCreateRequest req) {
        req.setId(id);
        return super.update(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return super.delete(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam MultiValueMap<String, String> allParams) {
        return super.exportAll(allParams, RealEstateResponseDto.class, "real_estates", RealEstate.class);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importData(@RequestParam("file") MultipartFile file) {
        return super.importAll(file, RealEstateCreateRequest.class);
    }
}
