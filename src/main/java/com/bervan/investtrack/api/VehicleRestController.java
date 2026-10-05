package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.ImportResult;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.common.service.AuthService;
import com.bervan.investtrack.model.Vehicle;
import com.bervan.investtrack.model.VehicleSnapshot;
import com.bervan.investtrack.service.VehicleService;
import com.bervan.investtrack.service.VehicleSnapshotService;
import com.bervan.logging.JsonLogger;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/invest-track/vehicle")
public class VehicleRestController extends BaseOwnedController<Vehicle, UUID> {
    private JsonLogger log = JsonLogger.getLogger(VehicleRestController.class, "invest-track");
    private final VehicleSnapshotService snapshotService;

    protected VehicleRestController(VehicleService service,
                                    VehicleSnapshotService snapshotService,
                                    BervanDTOMapper mapper,
                                    EntityConfigValidator validator) {
        super(service, mapper, validator, "Vehicle");
        this.snapshotService = snapshotService;
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
        ((VehicleService) service).deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<List<AssetSnapshotDto>> getSnapshots(@PathVariable UUID id) {
        List<VehicleSnapshot> snapshots = snapshotService.findByVehicleId(id);
        List<AssetSnapshotDto> dtos = snapshots.stream()
                .map(s -> new AssetSnapshotDto(s.getId(), id, s.getSnapshotDate(), s.getAssetValue(), s.getNotes()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/snapshots")
    public ResponseEntity<AssetSnapshotDto> createSnapshot(@PathVariable UUID id,
                                                           @RequestBody AssetSnapshotCreateRequest req) {
        Optional<Vehicle> vehicleOpt = service.loadById(id);
        if (vehicleOpt.isEmpty()) return ResponseEntity.notFound().build();

        VehicleSnapshot snapshot = new VehicleSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setVehicle(vehicleOpt.get());
        snapshot.setSnapshotDate(req.getSnapshotDate());
        snapshot.setAssetValue(req.getAssetValue());
        snapshot.setNotes(req.getNotes());
        snapshot.setDeleted(false);
        snapshot.addOwner(AuthService.getLoggedUser().get());
        VehicleSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new AssetSnapshotDto(saved.getId(), id, saved.getSnapshotDate(), saved.getAssetValue(), saved.getNotes()));
    }

    @PutMapping("/{id}/snapshots/{snapshotId}")
    public ResponseEntity<AssetSnapshotDto> updateSnapshot(@PathVariable UUID id,
                                                           @PathVariable UUID snapshotId,
                                                           @RequestBody AssetSnapshotCreateRequest req) {
        List<VehicleSnapshot> snapshots = snapshotService.findByVehicleId(id);
        Optional<VehicleSnapshot> match = snapshots.stream().filter(s -> s.getId().equals(snapshotId)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        VehicleSnapshot snapshot = match.get();
        if (req.getSnapshotDate() != null) snapshot.setSnapshotDate(req.getSnapshotDate());
        if (req.getAssetValue() != null) snapshot.setAssetValue(req.getAssetValue());
        if (req.getNotes() != null) snapshot.setNotes(req.getNotes());
        VehicleSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new AssetSnapshotDto(saved.getId(), id, saved.getSnapshotDate(), saved.getAssetValue(), saved.getNotes()));
    }

    @DeleteMapping("/{id}/snapshots/{snapshotId}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable UUID id, @PathVariable UUID snapshotId) {
        List<VehicleSnapshot> snapshots = snapshotService.findByVehicleId(id);
        if (snapshots.stream().noneMatch(s -> s.getId().equals(snapshotId))) return ResponseEntity.notFound().build();
        ((VehicleService) service).deleteSnapshot(snapshotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/snapshots/export")
    public ResponseEntity<byte[]> exportSnapshotsCsv(@PathVariable UUID id) {
        String csv = snapshotService.exportSnapshotsToCsv(id);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vehicle-" + id + "-snapshots.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable UUID id) {
        Optional<Vehicle> vehicleOpt = service.loadById(id);
        if (vehicleOpt.isEmpty()) return ResponseEntity.notFound().build();

        Vehicle vehicle = vehicleOpt.get();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("initialValue", vehicle.getInitialValue());
        metrics.put("currentValue", vehicle.getCurrentValue());
        metrics.put("returnRate", vehicle.getReturnRate());
        BigDecimal totalGain = vehicle.getCurrentValue().subtract(vehicle.getInitialValue());
        metrics.put("totalGain", totalGain);
        metrics.put("initialDate", vehicle.getInitialDate());
        metrics.put("latestDate", vehicle.getLatestDate());
        metrics.put("snapshotsCount", vehicle.getSnapshots() != null ? vehicle.getSnapshots().size() : 0);
        return ResponseEntity.ok(metrics);
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
