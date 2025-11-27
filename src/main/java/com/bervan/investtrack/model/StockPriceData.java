package com.bervan.investtrack.model;

import com.bervan.ieentities.ExcelIEEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockPriceData implements ExcelIEEntity<Long> {
    private Long id;
    @EqualsAndHashCode.Include
    private String symbol;
    @EqualsAndHashCode.Include
    private String date;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private Integer transactions;
}
