package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.WalletSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        return load(searchRequest, Pageable.ofSize(1000000)).stream().toList();
    }
}
