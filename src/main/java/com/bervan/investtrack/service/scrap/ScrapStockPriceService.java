package com.bervan.investtrack.service.scrap;

import java.math.BigDecimal;
import java.util.Optional;

public interface ScrapStockPriceService {
    Optional<BigDecimal> getStockPrice(String symbol);
    boolean supports(String exchange);
    String getExchange();
    String getBaseUrl();
}
