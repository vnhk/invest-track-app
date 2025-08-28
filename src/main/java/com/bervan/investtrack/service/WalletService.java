package com.bervan.investtrack.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Wallet;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WalletService extends BaseService<UUID, Wallet> {
    protected WalletService(BaseRepository<Wallet, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }
}
