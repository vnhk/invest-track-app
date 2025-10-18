package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class WalletService extends BaseService<UUID, Wallet> {

    private final WalletSnapshotService snapshotService;

    protected WalletService(BaseRepository<Wallet, UUID> repository, SearchService searchService, WalletSnapshotService snapshotService) {
        super(repository, searchService);
        this.snapshotService = snapshotService;
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

//    @Transactional
//    public Wallet addSnapshot(UUID walletId, WalletSnapshot snapshot) {
//        Wallet wallet = findById(walletId);
//        snapshot.setId(UUID.randomUUID());
//        snapshot.setWallet(wallet);
//
//        snapshot = snapshotService.save(snapshot);
//        wallet.getSnapshots().add(snapshot);
//
//        return findById(walletId);
//    }

    @Transactional
    public void deleteWallet(UUID walletId) {
        Wallet wallet = findById(walletId);

        wallet.getSnapshots().forEach(e -> snapshotService.deleteById(e.getId()));

        repository.deleteById(walletId);
    }

    @Transactional
    public void deleteSnapshot(UUID snapshotId) {
        snapshotService.deleteById(snapshotId);
    }

    @Transactional(readOnly = true)
    public Wallet getWalletWithSnapshots(String walletName) {
        Wallet wallet = getWalletByName(walletName).iterator().next();
        wallet.getSnapshots().size(); // This triggers lazy loading
        return wallet;
    }

    private Wallet findById(UUID walletId) {
        return repository.findById(walletId).orElseThrow(() -> new IllegalArgumentException("Wallet with id " + walletId + " not found"));
    }
}
