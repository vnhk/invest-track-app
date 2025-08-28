package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletSnapshotRepository extends BaseRepository<WalletSnapshot, UUID> {

    List<WalletSnapshot> findByWalletId(UUID id);
}
