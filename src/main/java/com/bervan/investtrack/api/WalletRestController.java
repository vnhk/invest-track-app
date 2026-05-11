package com.bervan.investtrack.api;

import com.bervan.common.config.EntityConfigValidator;
import com.bervan.common.controller.BaseOwnedController;
import com.bervan.common.controller.BaseOwnedController.ImportResult;
import com.bervan.common.mapper.BervanDTOMapper;
import com.bervan.common.service.AuthService;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.InvestmentCalculationService;
import com.bervan.investtrack.service.WalletService;
import com.bervan.investtrack.service.WalletSnapshotService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/invest-track/wallets")
public class WalletRestController extends BaseOwnedController<Wallet, UUID> {

    private final WalletSnapshotService snapshotService;
    private final InvestmentCalculationService calculationService;

    protected WalletRestController(WalletService walletService, WalletSnapshotService snapshotService,
                                   InvestmentCalculationService calculationService,
                                   BervanDTOMapper mapper, EntityConfigValidator validator) {
        super(walletService, mapper, validator, "Wallet");
        this.snapshotService = snapshotService;
        this.calculationService = calculationService;
    }

    @GetMapping
    public ResponseEntity<Page<WalletDto>> list(
            @RequestParam MultiValueMap<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return super.search(allParams, page, size, WalletDto.class, Wallet.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletDto> getById(@PathVariable UUID id) {
        return super.getById(id, WalletDto.class);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WalletCreateRequest req) {
        return super.create(req, WalletDto.class);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody WalletDto req) {
        // manual update — Wallet has List<WalletSnapshot> snapshots, super.update() must not be used
        Optional<Wallet> match = service.loadById(id);
        if (match.isEmpty()) return ResponseEntity.notFound().build();
        Wallet wallet = match.get();
        if (req.getName() != null) wallet.setName(req.getName());
        if (req.getDescription() != null) wallet.setDescription(req.getDescription());
        if (req.getCurrency() != null) wallet.setCurrency(req.getCurrency());
        if (req.getRiskLevel() != null) wallet.setRiskLevel(req.getRiskLevel());
        if (req.getWalletType() != null) wallet.setWalletType(req.getWalletType());
        if (req.getCompareWithSP500() != null) wallet.setCompareWithSP500(req.getCompareWithSP500());
        wallet.setModificationDate(LocalDateTime.now());
        Wallet saved = service.save(wallet);
        return ResponseEntity.ok(mapper.map(saved, WalletDto.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return super.delete(id);
    }

    // ── Snapshot sub-resource ──────────────────────────────────────────────────

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<List<WalletSnapshotDto>> getSnapshots(@PathVariable UUID id) {
        List<WalletSnapshot> snapshots = snapshotService.findByWalletId(id);
        List<WalletSnapshotDto> dtos = snapshots.stream()
                .map(s -> new WalletSnapshotDto(s.getId(), id, s.getSnapshotDate(),
                        s.getPortfolioValue(), s.getMonthlyDeposit(), s.getMonthlyWithdrawal(),
                        s.getMonthlyEarnings(), s.getNotes()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable UUID id) {
        Optional<Wallet> walletOpt = service.loadById(id);
        if (walletOpt.isEmpty()) return ResponseEntity.notFound().build();

        Wallet wallet = walletOpt.get();
        List<WalletSnapshot> snapshots = snapshotService.findByWalletId(id);

        var metrics = new java.util.LinkedHashMap<String, Object>();
        metrics.put("currentValue", wallet.getCurrentValue());
        metrics.put("totalDeposits", wallet.getTotalDeposits());
        metrics.put("totalWithdrawals", wallet.getTotalWithdrawals());
        metrics.put("totalEarnings", wallet.getTotalEarnings());
        metrics.put("returnRate", wallet.getReturnRate());

        if (snapshots.size() >= 2) {
            var scale = java.math.RoundingMode.HALF_UP;
            BigDecimal twr = calculationService.calculateTWR(snapshots);
            metrics.put("twr", twr.multiply(BigDecimal.valueOf(100)).setScale(2, scale));

            WalletSnapshot first = snapshots.stream().min(java.util.Comparator.comparing(WalletSnapshot::getSnapshotDate)).get();
            WalletSnapshot last = snapshots.stream().max(java.util.Comparator.comparing(WalletSnapshot::getSnapshotDate)).get();
            double years = java.time.temporal.ChronoUnit.DAYS.between(first.getSnapshotDate(), last.getSnapshotDate()) / 365.0;
            if (years > 0 && first.getPortfolioValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cagr = calculationService.calculateCAGR(first.getPortfolioValue(), last.getPortfolioValue(), years);
                metrics.put("cagr", cagr.multiply(BigDecimal.valueOf(100)).setScale(2, scale));
            }

            metrics.put("monthlyReturns", calculationService.calculateMonthlyReturns(snapshots));
            metrics.put("yearlyReturns", calculationService.calculateYearlyReturns(snapshots));
        }
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/{walletId}/snapshots")
    public ResponseEntity<WalletSnapshotDto> createSnapshot(@PathVariable UUID walletId,
                                                            @RequestBody SnapshotCreateRequest req) {
        Optional<Wallet> walletOpt = service.loadById(walletId);
        if (walletOpt.isEmpty()) return ResponseEntity.notFound().build();

        WalletSnapshot snapshot = new WalletSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setWallet(walletOpt.get());
        snapshot.setSnapshotDate(req.getSnapshotDate());
        snapshot.setPortfolioValue(req.getPortfolioValue());
        snapshot.setMonthlyDeposit(req.getMonthlyDeposit() != null ? req.getMonthlyDeposit() : BigDecimal.ZERO);
        snapshot.setMonthlyWithdrawal(req.getMonthlyWithdrawal() != null ? req.getMonthlyWithdrawal() : BigDecimal.ZERO);
        snapshot.setMonthlyEarnings(req.getMonthlyEarnings() != null ? req.getMonthlyEarnings() : BigDecimal.ZERO);
        snapshot.setNotes(req.getNotes());
        snapshot.setDeleted(false);
        snapshot.addOwner(AuthService.getLoggedUser().get());
        WalletSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new WalletSnapshotDto(saved.getId(), walletId, saved.getSnapshotDate(),
                saved.getPortfolioValue(), saved.getMonthlyDeposit(), saved.getMonthlyWithdrawal(),
                saved.getMonthlyEarnings(), saved.getNotes()));
    }

    @PutMapping("/{walletId}/snapshots/{snapshotId}")
    public ResponseEntity<WalletSnapshotDto> updateSnapshot(@PathVariable UUID walletId,
                                                            @PathVariable UUID snapshotId,
                                                            @RequestBody SnapshotCreateRequest req) {
        List<WalletSnapshot> snapshots = snapshotService.findByWalletId(walletId);
        Optional<WalletSnapshot> match = snapshots.stream().filter(s -> s.getId().equals(snapshotId)).findFirst();
        if (match.isEmpty()) return ResponseEntity.notFound().build();

        WalletSnapshot snapshot = match.get();
        if (req.getSnapshotDate() != null) snapshot.setSnapshotDate(req.getSnapshotDate());
        if (req.getPortfolioValue() != null) snapshot.setPortfolioValue(req.getPortfolioValue());
        if (req.getMonthlyDeposit() != null) snapshot.setMonthlyDeposit(req.getMonthlyDeposit());
        if (req.getMonthlyWithdrawal() != null) snapshot.setMonthlyWithdrawal(req.getMonthlyWithdrawal());
        if (req.getMonthlyEarnings() != null) snapshot.setMonthlyEarnings(req.getMonthlyEarnings());
        if (req.getNotes() != null) snapshot.setNotes(req.getNotes());
        WalletSnapshot saved = snapshotService.save(snapshot);
        return ResponseEntity.ok(new WalletSnapshotDto(saved.getId(), walletId, saved.getSnapshotDate(),
                saved.getPortfolioValue(), saved.getMonthlyDeposit(), saved.getMonthlyWithdrawal(),
                saved.getMonthlyEarnings(), saved.getNotes()));
    }

    @DeleteMapping("/{walletId}/snapshots/{snapshotId}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable UUID walletId, @PathVariable UUID snapshotId) {
        List<WalletSnapshot> snapshots = snapshotService.findByWalletId(walletId);
        if (snapshots.stream().noneMatch(s -> s.getId().equals(snapshotId))) return ResponseEntity.notFound().build();
        ((WalletService) service).deleteSnapshot(snapshotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return super.exportAll(WalletDto.class, "wallets");
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importData(@RequestParam("file") MultipartFile file) {
        return super.importAll(file, WalletDto.class);
    }
}
