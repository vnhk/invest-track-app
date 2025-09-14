package com.bervan.investtrack.service.scrap;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class GPWScrapStockPriceService implements ScrapStockPriceService {

    public static final String GPW = "GPW";
    public static final String BASE = "https://www.gpw.pl";

    @Override
    public Optional<BigDecimal> getStockPrice(String symbol) {
        try {
            Connection connect = Jsoup.connect(BASE + "/spolka?isin=" + symbol);
            Document document = connect.get();

            String price = document.getElementsByClass("container")
                    .select("span.summary").text().replace(",", ".");
            return Optional.of(new BigDecimal(price));

        } catch (Exception e) {
            log.error("Failed to get stock price for symbol: {}", symbol, e);
        }

        return Optional.empty();
    }

    @Override
    public boolean supports(String exchange) {
        return GPW.equals(exchange);
    }

    @Override
    public String getExchange() {
        return GPW;
    }

    @Override
    public String getBaseUrl() {
        return BASE;
    }
}
