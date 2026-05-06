package com.bervan.investtrack.api;

import com.bervan.core.model.PostMapper;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import org.springframework.stereotype.Service;

@Service
public class StockAlertToAlertDtoPostMapper implements PostMapper<StockPriceAlert, StockAlertDto> {

    @Override
    public void map(StockPriceAlert alert, StockAlertDto dto) {
        StockPriceAlertConfig config = alert.getStockPriceAlertConfig();
        if (config == null) return;
        dto.setConfig(new AlertConfigDto(
                config.getId(),
                config.getPrice(),
                config.getOperator(),
                config.getAmountOfNotifications(),
                config.getCheckIntervalMinutes(),
                config.getAnotherNotificationEachPercentage(),
                config.getPreviouslyNotifiedDate(),
                config.getPreviouslyNotifiedPrice()
        ));
    }

    @Override
    public Class<StockPriceAlert> getFromType() {
        return StockPriceAlert.class;
    }

    @Override
    public Class<StockAlertDto> getToType() {
        return StockAlertDto.class;
    }
}
