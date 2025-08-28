package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Wallet;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletRepository extends BaseRepository<Wallet, UUID> {

}
