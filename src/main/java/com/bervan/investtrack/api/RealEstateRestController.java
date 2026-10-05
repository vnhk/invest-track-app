package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.ImportResult;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.common.service.AuthService;
import com.bervan.investtrack.model.RealEstate;
import com.bervan.investtrack.model.RealEstateSnapshot;
import com.bervan.investtrack.service.RealEstateService;
import com.bervan.investtrack.service.RealEstateSnapshotService;
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
@RequestMapping("/api/invest-track/real-estate")
public class RealEstateRestController extends BaseOwnedController<RealEstate, UUID> {
    private JsonLogger log = JsonLogger.getLogger(RealEstateRestController.class, "invest-track");
    private final RealEstateSnapshotService snapshotService;

    protected RealEstateRestController(RealEstateService service,
                                       RealEstateSnapshotService snapshotService,
                                       BervanDTOMapper mapper,
                                       EntityConfigValidator validator) {
        super(service, mapper, validator, "RealEstate");
        this.snapshotService = snapshotService;
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
        ((RealEstateService) service).deleteRealEstate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<List<AssetSnapshotDto>> getSnapshots(@PathVariable UUID id) {
        List<RealEstateSnapshot> snapshots = snapshotService.findByRealEstateId(id);
        List<AssetSnapshotDto> dtos = snapshots.stream()
                .map(s -> new AssetSnapshotDto(s.getId(), id, s.getSnapshotDate(), s.getAssetValue(), s.getNotes()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/snapshots")
    public ResponseEntity<AssetSnapshotDto> createSnapshot(@PathVariable UUID id,
                                                           @RequestBody AssetSnapshotCreateRequest req) {
        Optional<RealEstate> realEstateOpt = service.loadById(id);
        if (realEstateOpt.isEmpty()) return ResponseEntity.notFound().build();

        RealEstateSnapshot snapshot = new RealEstateSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setRealEstate(realEstateOpt.get());
        snapshot.setSnapshotDate(req.getSnapshotDate());
        snapshot.setAssetValue(req.getAssetValue());
        snapshot.setNotes(req.getNotes());
        snapshot.setDeleted(false);
        snapshot.addOwner(AuthService.getLoggedUser().get());
        RealEstateSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new AssetSnapshotDto(saved.getId(), id, saved.getSnapshotDate(), saved.getAssetValue(), saved.getNotes()));
    }

    @PutMapping("/{id}/snapshots/{snapshotId}")
    public ResponseEntity<AssetSnapshotDto> updateSnapshot(@PathVariable UUID id,
                                                           @PathVariable UUID snapshotId,
                                                           @RequestBody AssetSnapshotCreateRequest req) {
        List<RealEstateSnapshot> snapshots = snapshotService.findByRealEstateId(id);
        Optional<RealEstateSnapshot> match = snapshots.stream().filter(s -> s.getId().equals(snapshotId)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        RealEstateSnapshot snapshot = match.get();
        if (req.getSnapshotDate() != null) snapshot.setSnapshotDate(req.getSnapshotDate());
        if (req.getAssetValue() != null) snapshot.setAssetValue(req.getAssetValue());
        if (req.getNotes() != null) snapshot.setNotes(req.getNotes());
        RealEstateSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new AssetSnapshotDto(saved.getId(), id, saved.getSnapshotDate(), saved.getAssetValue(), saved.getNotes()));
    }

    @DeleteMapping("/{id}/snapshots/{snapshotId}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable UUID id, @PathVariable UUID snapshotId) {
        List<RealEstateSnapshot> snapshots = snapshotService.findByRealEstateId(id);
        if (snapshots.stream().noneMatch(s -> s.getId().equals(snapshotId))) return ResponseEntity.notFound().build();
        ((RealEstateService) service).deleteSnapshot(snapshotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/snapshots/export")
    public ResponseEntity<byte[]> exportSnapshotsCsv(@PathVariable UUID id) {
        String csv = snapshotService.exportSnapshotsToCsv(id);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"real-estate-" + id + "-snapshots.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable UUID id) {
        Optional<RealEstate> realEstateOpt = service.loadById(id);
        if (realEstateOpt.isEmpty()) return ResponseEntity.notFound().build();

        RealEstate realEstate = realEstateOpt.get();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("initialValue", realEstate.getInitialValue());
        metrics.put("currentValue", realEstate.getCurrentValue());
        metrics.put("returnRate", realEstate.getReturnRate());
        BigDecimal totalGain = realEstate.getCurrentValue().subtract(realEstate.getInitialValue());
        metrics.put("totalGain", totalGain);
        metrics.put("initialDate", realEstate.getInitialDate());
        metrics.put("latestDate", realEstate.getLatestDate());
        metrics.put("snapshotsCount", realEstate.getSnapshots() != null ? realEstate.getSnapshots().size() : 0);
        return ResponseEntity.ok(metrics);
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
