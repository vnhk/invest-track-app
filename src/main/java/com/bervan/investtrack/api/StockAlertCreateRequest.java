package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.investtrack.model.StockPriceAlert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertCreateRequest implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String symbol;
    private String exchange;
    private List<String> emails;
    // config fields inlined — handled manually in controller
    private BigDecimal price;
    private String operator;
    private Integer amountOfNotifications;
    private Integer checkIntervalMinutes;
    private Integer anotherNotificationEachPercentage;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return StockPriceAlert.class;
    }
}
