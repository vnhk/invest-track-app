package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockPriceAlertRepository extends BaseRepository<StockPriceAlert, UUID> {
    List<StockPriceAlert> findAllByDeletedIsFalseOrDeletedNull();

    @Query("SELECT DISTINCT c FROM StockPriceAlert p JOIN p.emails c WHERE (p.deleted IS FALSE OR p.deleted IS NULL) AND p = :stockPriceAlert")
    List<String> loadAllEmails(StockPriceAlert stockPriceAlert);

    @Query("SELECT DISTINCT c FROM StockPriceAlert p JOIN p.emails c WHERE (p.deleted IS FALSE OR p.deleted IS NULL)")
    List<String> loadAllEmails();

    @Query("SELECT DISTINCT  p.stockPriceAlertConfig FROM StockPriceAlert p WHERE (p.deleted IS FALSE OR p.deleted IS NULL) AND p = :stockPriceAlert")
    StockPriceAlertConfig loadStockPriceAlertConfig(StockPriceAlert stockPriceAlert);
}
