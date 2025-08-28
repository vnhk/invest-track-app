package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Wallet;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class WalletService extends BaseService<UUID, Wallet> {
    protected WalletService(BaseRepository<Wallet, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

    @Override
    public Wallet save(Wallet data) {
        if (data.getId() == null) {
            String name = data.getName();
            Set<Wallet> loaded = getWalletByName(name);
            if (!loaded.isEmpty()) {
                throw new IllegalArgumentException("Wallet with name " + name + " already exists");
            }

        }
        return super.save(data);
    }

    public Set<Wallet> getWalletByName(String name) {
        SearchRequest request = new SearchRequest();
        request.addCriterion("WALLET_NAME", Wallet.class, "name", SearchOperation.EQUALS_OPERATION, name);
        return load(request, Pageable.ofSize(1));
    }
}
