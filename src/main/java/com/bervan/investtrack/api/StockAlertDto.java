package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.core.model.PostCustomMappers;
import com.bervan.investtrack.model.StockPriceAlert;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PostCustomMappers(mappers = {StockAlertToAlertDtoPostMapper.class})
public class StockAlertDto implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String symbol;
    private String exchange;
    private List<String> emails;
    // populated by StockAlertToAlertDtoPostMapper from stockPriceAlertConfig
    private AlertConfigDto config;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return StockPriceAlert.class;
    }
}
