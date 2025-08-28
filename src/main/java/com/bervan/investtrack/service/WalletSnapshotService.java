package com.bervan.investtrack.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.WalletSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WalletSnapshotService extends BaseService<UUID, WalletSnapshot> {
    protected WalletSnapshotService(BaseRepository<WalletSnapshot, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

    public List<WalletSnapshot> findByWalletId(UUID id) {
        return ((WalletSnapshotRepository) repository).findByWalletId(id);
    }
}
