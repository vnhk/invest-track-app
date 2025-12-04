package com.bervan.investtrack.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.logging.JsonLogger;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StockPriceAlertConfigService extends BaseService<UUID, StockPriceAlertConfig> {
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");

    protected StockPriceAlertConfigService(StockPriceAlertConfigRepository repository, SearchService searchService) {
        super(repository, searchService);
    }
}
