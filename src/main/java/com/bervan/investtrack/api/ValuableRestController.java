package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.ImportResult;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.common.service.AuthService;
import com.bervan.investtrack.model.Valuable;
import com.bervan.investtrack.model.ValuableSnapshot;
import com.bervan.investtrack.service.ValuableService;
import com.bervan.investtrack.service.ValuableSnapshotService;
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
@RequestMapping("/api/invest-track/valuable")
public class ValuableRestController extends BaseOwnedController<Valuable, UUID> {
    private JsonLogger log = JsonLogger.getLogger(ValuableRestController.class, "invest-track");
    private final ValuableSnapshotService snapshotService;

    protected ValuableRestController(ValuableService service,
                                     ValuableSnapshotService snapshotService,
                                     BervanDTOMapper mapper,
                                     EntityConfigValidator validator) {
        super(service, mapper, validator, "Valuable");
        this.snapshotService = snapshotService;
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
        ((ValuableService) service).deleteValuable(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<List<AssetSnapshotDto>> getSnapshots(@PathVariable UUID id) {
        List<ValuableSnapshot> snapshots = snapshotService.findByValuableId(id);
        List<AssetSnapshotDto> dtos = snapshots.stream()
                .map(s -> new AssetSnapshotDto(s.getId(), id, s.getSnapshotDate(), s.getAssetValue(), s.getNotes()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/snapshots")
    public ResponseEntity<AssetSnapshotDto> createSnapshot(@PathVariable UUID id,
                                                           @RequestBody AssetSnapshotCreateRequest req) {
        Optional<Valuable> valuableOpt = service.loadById(id);
        if (valuableOpt.isEmpty()) return ResponseEntity.notFound().build();

        ValuableSnapshot snapshot = new ValuableSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setValuable(valuableOpt.get());
        snapshot.setSnapshotDate(req.getSnapshotDate());
        snapshot.setAssetValue(req.getAssetValue());
        snapshot.setNotes(req.getNotes());
        snapshot.setDeleted(false);
        snapshot.addOwner(AuthService.getLoggedUser().get());
        ValuableSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new AssetSnapshotDto(saved.getId(), id, saved.getSnapshotDate(), saved.getAssetValue(), saved.getNotes()));
    }

    @PutMapping("/{id}/snapshots/{snapshotId}")
    public ResponseEntity<AssetSnapshotDto> updateSnapshot(@PathVariable UUID id,
                                                           @PathVariable UUID snapshotId,
                                                           @RequestBody AssetSnapshotCreateRequest req) {
        List<ValuableSnapshot> snapshots = snapshotService.findByValuableId(id);
        Optional<ValuableSnapshot> match = snapshots.stream().filter(s -> s.getId().equals(snapshotId)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        ValuableSnapshot snapshot = match.get();
        if (req.getSnapshotDate() != null) snapshot.setSnapshotDate(req.getSnapshotDate());
        if (req.getAssetValue() != null) snapshot.setAssetValue(req.getAssetValue());
        if (req.getNotes() != null) snapshot.setNotes(req.getNotes());
        ValuableSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new AssetSnapshotDto(saved.getId(), id, saved.getSnapshotDate(), saved.getAssetValue(), saved.getNotes()));
    }

    @DeleteMapping("/{id}/snapshots/{snapshotId}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable UUID id, @PathVariable UUID snapshotId) {
        List<ValuableSnapshot> snapshots = snapshotService.findByValuableId(id);
        if (snapshots.stream().noneMatch(s -> s.getId().equals(snapshotId))) return ResponseEntity.notFound().build();
        ((ValuableService) service).deleteSnapshot(snapshotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/snapshots/export")
    public ResponseEntity<byte[]> exportSnapshotsCsv(@PathVariable UUID id) {
        String csv = snapshotService.exportSnapshotsToCsv(id);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"valuable-" + id + "-snapshots.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable UUID id) {
        Optional<Valuable> valuableOpt = service.loadById(id);
        if (valuableOpt.isEmpty()) return ResponseEntity.notFound().build();

        Valuable valuable = valuableOpt.get();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("initialValue", valuable.getInitialValue());
        metrics.put("currentValue", valuable.getCurrentValue());
        metrics.put("returnRate", valuable.getReturnRate());
        BigDecimal totalGain = valuable.getCurrentValue().subtract(valuable.getInitialValue());
        metrics.put("totalGain", totalGain);
        metrics.put("initialDate", valuable.getInitialDate());
        metrics.put("latestDate", valuable.getLatestDate());
        metrics.put("snapshotsCount", valuable.getSnapshots() != null ? valuable.getSnapshots().size() : 0);
        return ResponseEntity.ok(metrics);
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
