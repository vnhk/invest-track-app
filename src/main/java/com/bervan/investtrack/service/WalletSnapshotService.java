package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.WalletSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WalletSnapshotService extends BaseService<UUID, WalletSnapshot> {
    protected WalletSnapshotService(BaseRepository<WalletSnapshot, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

    public List<WalletSnapshot> findByWalletId(UUID id) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.addCriterion("WALLET_ID", WalletSnapshot.class, "wallet.id", SearchOperation.EQUALS_OPERATION, id.toString());
        return load(searchRequest, Pageable.ofSize(1000000), "snapshotDate", SortDirection.DESC, null).stream().toList();
    }

    public String exportSnapshotsToCsv(UUID walletId) {
        List<WalletSnapshot> snapshots = findByWalletId(walletId);
        StringBuilder csv = new StringBuilder();

        csv.append("Snapshot Date,Portfolio Value,Monthly Deposit,Monthly Withdrawal,Monthly Earnings,Monthly Return Rate,Notes\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (WalletSnapshot snapshot : snapshots) {
            csv.append(snapshot.getSnapshotDate().format(dateFormatter)).append(",");
            csv.append(snapshot.getPortfolioValue()).append(",");
            csv.append(snapshot.getMonthlyDeposit()).append(",");
            csv.append(snapshot.getMonthlyWithdrawal()).append(",");
            csv.append(snapshot.getMonthlyEarnings()).append(",");
            csv.append("\"").append(snapshot.getNotes() != null ? snapshot.getNotes().replace("\"", "\"\"") : "").append("\"");
            csv.append("\n");
        }

        return csv.toString();
    }

}
