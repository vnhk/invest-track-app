package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockPriceAlertConfigRepository extends BaseRepository<StockPriceAlertConfig, UUID> {
    List<StockPriceAlertConfig> findAllByDeletedIsFalseOrDeletedNull();
}
