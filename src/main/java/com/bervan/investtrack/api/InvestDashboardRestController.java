package com.bervan.investtrack.api;

import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.investtrack.service.ETFDataService;
import com.bervan.investtrack.service.WalletService;
import com.bervan.investtrack.service.WalletSnapshotService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invest-track")
public class InvestDashboardRestController {

    private final WalletService walletService;
    private final WalletSnapshotService snapshotService;
    private final ETFDataService ETFDataService;
    private final InvestDashboardHelper investDashboardHelper;
    private final BudgetEntryService budgetEntryService;

    public InvestDashboardRestController(WalletService walletService, WalletSnapshotService snapshotService,
                                         ETFDataService ETFDataService, InvestDashboardHelper investDashboardHelper, BudgetEntryService budgetEntryService) {
        this.walletService = walletService;
        this.snapshotService = snapshotService;
        this.ETFDataService = ETFDataService;
        this.investDashboardHelper = investDashboardHelper;
        this.budgetEntryService = budgetEntryService;
    }

    @GetMapping(path = "/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {

        // Load all wallets with snapshots
        List<Wallet> allWallets = new ArrayList<>(walletService.load(PageRequest.of(0, Integer.MAX_VALUE)));
        for (Wallet w : allWallets) {
            List<WalletSnapshot> snapshots = snapshotService.findByWalletId(w.getId());
            w.getSnapshots().clear();
            w.getSnapshots().addAll(snapshots);
        }

        Map<String, Object> result = investDashboardHelper.getDashboard(allWallets);

        return ResponseEntity.ok(result);
    }
}