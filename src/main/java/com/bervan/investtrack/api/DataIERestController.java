package com.bervan.investtrack.api;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.WalletService;
import com.bervan.investtrack.service.WalletSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/invest-track/data-ie")
public class DataIERestController {

    private final WalletSnapshotService snapshotService;
    private final WalletService walletService;
    private final ObjectMapper mapper;

    public DataIERestController(WalletSnapshotService snapshotService, WalletService walletService, ObjectMapper mapper) {
        this.snapshotService = snapshotService;
        this.walletService = walletService;
        this.mapper = mapper;
    }

    record SnapshotExportDto(
            UUID id,
            UUID walletId,
            String walletName,
            LocalDate snapshotDate,
            BigDecimal portfolioValue,
            BigDecimal monthlyDeposit,
            BigDecimal monthlyWithdrawal,
            BigDecimal monthlyEarnings,
            String notes
    ) {}

    record ImportResultDto(int imported, int skipped, List<String> errors) {}

    private SnapshotExportDto toExportDto(WalletSnapshot s) {
        return new SnapshotExportDto(
                s.getId(),
                s.getWallet() != null ? s.getWallet().getId() : null,
                s.getWallet() != null ? s.getWallet().getName() : null,
                s.getSnapshotDate(),
                s.getPortfolioValue(),
                s.getMonthlyDeposit(),
                s.getMonthlyWithdrawal(),
                s.getMonthlyEarnings(),
                s.getNotes()
        );
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "json") String format) {

        try {
            Set<WalletSnapshot> all = snapshotService.load(Pageable.ofSize(1000000));
            List<SnapshotExportDto> dtos = all.stream().map(this::toExportDto).toList();

            byte[] data = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dtos);
            String filename = "wallet-snapshots-" + LocalDate.now() + ".json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultDto> importData(@RequestParam("file") MultipartFile file) {

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try {
            List<?> rawList = mapper.readValue(file.getInputStream(), List.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) rawList;

            Set<Wallet> wallets = walletService.load(Pageable.ofSize(10000));
            Map<UUID, Wallet> walletById = new HashMap<>();
            for (Wallet w : wallets) walletById.put(w.getId(), w);

            for (Map<String, Object> item : items) {
                try {
                    String walletIdStr = (String) item.get("walletId");
                    if (walletIdStr == null) { skipped++; errors.add("Missing walletId in record"); continue; }
                    UUID walletId = UUID.fromString(walletIdStr);
                    Wallet wallet = walletById.get(walletId);
                    if (wallet == null) { skipped++; errors.add("Wallet not found: " + walletId); continue; }

                    WalletSnapshot snap = new WalletSnapshot();
                    snap.setId(UUID.randomUUID());
                    snap.setWallet(wallet);
                    snap.setSnapshotDate(LocalDate.parse((String) item.get("snapshotDate")));
                    snap.setPortfolioValue(new BigDecimal(item.get("portfolioValue").toString()));
                    snap.setMonthlyDeposit(new BigDecimal(item.get("monthlyDeposit").toString()));
                    snap.setMonthlyWithdrawal(new BigDecimal(item.get("monthlyWithdrawal").toString()));
                    snap.setMonthlyEarnings(new BigDecimal(item.get("monthlyEarnings").toString()));
                    snap.setNotes(item.containsKey("notes") ? (String) item.get("notes") : null);
                    snap.setModificationDate(LocalDateTime.now());
                    snap.setDeleted(false);

                    snapshotService.save(snap);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                    errors.add("Row error: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(new ImportResultDto(imported, skipped, errors));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ImportResultDto(0, 0, List.of("Parse error: " + e.getMessage())));
        }
    }
}
