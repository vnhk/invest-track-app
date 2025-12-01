package com.bervan.investtrack.service.scrap;

import com.bervan.logging.JsonLogger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class GPWScrapStockPriceService implements ScrapStockPriceService {
    public static final String GPW = "GPW";
    public static final String BASE = "https://www.gpw.pl";
    private final JsonLogger log = JsonLogger.getLogger(getClass());

    @Override
    public Optional<BigDecimal> getStockPrice(String symbol) {
        try {

            Connection connect = Jsoup.connect(BASE + "/spolka?isin=" + symbol.trim())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .timeout(30000)
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1");
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
