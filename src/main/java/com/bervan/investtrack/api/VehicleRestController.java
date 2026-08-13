package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.ImportResult;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.investtrack.model.Vehicle;
import com.bervan.investtrack.service.VehicleService;
import com.bervan.logging.JsonLogger;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/invest-track/vehicle")
public class VehicleRestController extends BaseOwnedController<Vehicle, UUID> {
    private JsonLogger log = JsonLogger.getLogger(VehicleRestController.class, "invest-track");

    protected VehicleRestController(VehicleService service,
                                    BervanDTOMapper mapper,
                                    EntityConfigValidator validator) {
        super(service, mapper, validator, "Vehicle");
    }

    @GetMapping
    public ResponseEntity<Page<VehicleResponseDto>> list(
            @RequestParam MultiValueMap<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return super.search(allParams, page, size, VehicleResponseDto.class, Vehicle.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDto> getById(@PathVariable UUID id) {
        return super.getById(id, VehicleResponseDto.class);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody VehicleCreateRequest req) {
        return super.create(req, VehicleResponseDto.class);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody VehicleCreateRequest req) {
        req.setId(id);
        return super.update(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return super.delete(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam MultiValueMap<String, String> allParams) {
        return super.exportAll(allParams, VehicleResponseDto.class, "vehicles", Vehicle.class);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importData(@RequestParam("file") MultipartFile file) {
        return super.importAll(file, VehicleCreateRequest.class);
    }
}
