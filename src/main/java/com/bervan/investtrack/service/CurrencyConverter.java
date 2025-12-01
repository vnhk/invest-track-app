package com.bervan.investtrack.service;

import com.bervan.logging.JsonLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.EnumMap;
import java.util.Map;

@Service
public class CurrencyConverter {
    private final JsonLogger log = JsonLogger.getLogger(getClass());

    private final Map<Currency, BigDecimal> plnPerUnit = new EnumMap<>(Currency.class);
    private final int scale;
    private final RoundingMode roundingMode;

    public CurrencyConverter() {
        this.scale = 4;
        this.roundingMode = RoundingMode.HALF_UP;
        plnPerUnit.put(Currency.PLN, BigDecimal.ONE);
        plnPerUnit.put(Currency.EUR, new BigDecimal("4.30"));
        plnPerUnit.put(Currency.USD, new BigDecimal("3.70"));
    }

    public CurrencyConverter(Map<Currency, BigDecimal> initialRates, int scale, RoundingMode roundingMode) {
        this.scale = scale;
        this.roundingMode = roundingMode;
        plnPerUnit.putAll(initialRates);
    }

    @PostConstruct
    public void init() {
        updateRates();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void updateRates() {
        try {
            var client = HttpClient.newHttpClient();

            // Request USD-based currency rates
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"))
                    .GET()
                    .build();

            // Execute request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            // Extract PLN and EUR values relative to USD
            // 1 USD = X PLN
            BigDecimal usdToPln = root.get("usd").get("pln").decimalValue();
            BigDecimal usdToEur = root.get("usd").get("eur").decimalValue();

            setRate(Currency.PLN, BigDecimal.valueOf(1));
            setRate(Currency.USD, usdToPln);
            setRate(Currency.EUR, usdToPln.divide(usdToEur, MathContext.DECIMAL64));

            log.info("Updated currency rates: {}", plnPerUnit.entrySet());
        } catch (Exception e) {
            log.error("Failed to update currency rates: {}", e.getMessage(), e);
        }
    }

    /**
     * Convert amount from one currency to another.
     * @param amount amount in 'from' currency
     * @param from source currency
     * @param to target currency
     * @return converted amount in 'to' currency
     */
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (amount == null) throw new IllegalArgumentException("amount must not be null");
        BigDecimal rateFrom = plnPerUnit.get(from);
        BigDecimal rateTo = plnPerUnit.get(to);
        if (rateFrom == null || rateTo == null) throw new IllegalStateException("Missing rate for from/to currency");

        // Convert 'amount' -> PLN, then PLN -> target currency
        BigDecimal amountInPln = amount.multiply(rateFrom);
        return amountInPln.divide(rateTo, scale, roundingMode);
    }

    /** Update a single currency rate (PLN per 1 unit). */
    public void setRate(Currency currency, BigDecimal plnPerUnitRate) {
        if (currency == null || plnPerUnitRate == null) throw new IllegalArgumentException("args must not be null");
        plnPerUnit.put(currency, plnPerUnitRate);
    }

    public enum Currency {
        PLN, EUR, USD;

        public static Currency of(String currency) {
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("Currency code must not be null or empty");
            }
            try {
                return Currency.valueOf(currency.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported currency: " + currency);
            }
        }
    }
}
