package com.bervan.investtrack.view;

import com.bervan.common.component.CommonComponentHelper;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.service.StockPriceAlertService;

import java.util.UUID;

public class StockPriceAlertComponentHelper extends CommonComponentHelper<UUID, StockPriceAlert> {
    private final StockPriceAlertService service;

    public StockPriceAlertComponentHelper(StockPriceAlertService service) {
        super(StockPriceAlert.class);
        this.service = service;
    }

}
