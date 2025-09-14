package com.bervan.investtrack.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class StockPriceAlertConfigService extends BaseService<UUID, StockPriceAlertConfig> {

    protected StockPriceAlertConfigService(StockPriceAlertConfigRepository repository, SearchService searchService) {
        super(repository, searchService);
    }
}
