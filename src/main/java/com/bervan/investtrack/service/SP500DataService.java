package com.bervan.investtrack.service;

import com.bervan.logging.JsonLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Fetches monthly S&P 500 (^GSPC) prices and historical USD/PLN, USD/EUR FX rates
 * from Yahoo Finance, then calculates a benchmark portfolio value:
 * "if you had put each deposit into S&P 500 instead, accounting for real exchange rates."
 *
 * Algorithm:
 *   For each monthly deposit in portfolio currency (PLN/EUR/USD):
 *     1. Convert deposit to USD at the historical FX rate for that month
 *     2. Buy fractional S&P 500 units: units += deposit_usd / sp500_price
 *   Benchmark value at each date = accumulated_units × sp500_price[date] × fx_rate[date]
 *
 * All data is cached for 24 hours.
 */
@Service
public class SP500DataService {
    private static final String YAHOO_BASE = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final String SP500_TICKER  = "%5EGSPC";     // ^GSPC
    private static final String USDPLN_TICKER = "USDPLN%3DX"; // USDPLN=X  (PLN per 1 USD, e.g. 4.2)
    private static final String USDEUR_TICKER = "USDEUR%3DX"; // USDEUR=X  (EUR per 1 USD, e.g. 0.92)
    private static final String INTERVAL_RANGE = "?interval=1mo&range=25y";
    private static final long CACHE_TTL_MS = 24L * 60 * 60 * 1000;

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private Map<YearMonth, BigDecimal> cachedSP500  = null;
    private Map<YearMonth, BigDecimal> cachedUsdPln = null;
    private Map<YearMonth, BigDecimal> cachedUsdEur = null;
    private long cacheTimestamp = 0L;

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Calculates benchmark portfolio values with historical FX conversion.
     *
     * @param dates              chart date labels in "dd-MM-yyyy" format
     * @param monthlyNetDeposits net deposit amount for each date, in portfolioCurrency
     * @param portfolioCurrency  "PLN", "EUR", or "USD"
     * @return benchmark values in portfolioCurrency, same length as dates; empty on data error
     */
    public List<BigDecimal> calculateBenchmarkValues(List<String> dates,
                                                      List<BigDecimal> monthlyNetDeposits,
                                                      String portfolioCurrency) {
        if (dates == null || dates.isEmpty() || monthlyNetDeposits == null) {
            return Collections.emptyList();
        }

        refreshCacheIfNeeded();
        if (cachedSP500.isEmpty()) {
            return Collections.emptyList();
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        BigDecimal units = BigDecimal.ZERO;          // fractional S&P 500 units held
        List<BigDecimal> result = new ArrayList<>(dates.size());
        BigDecimal lastValue = BigDecimal.ZERO;

        for (int i = 0; i < dates.size(); i++) {
            LocalDate date;
            try {
                date = LocalDate.parse(dates.get(i), fmt);
            } catch (DateTimeParseException e) {
                result.add(lastValue);
                continue;
            }

            YearMonth ym = YearMonth.from(date);
            BigDecimal sp500Price = findNearest(cachedSP500, ym);
            BigDecimal fxRate    = getFxRate(portfolioCurrency, ym);  // foreign-currency units per 1 USD

            if (sp500Price == null || sp500Price.compareTo(BigDecimal.ZERO) <= 0
                    || fxRate == null || fxRate.compareTo(BigDecimal.ZERO) <= 0) {
                result.add(lastValue);
                continue;
            }

            BigDecimal deposit = i < monthlyNetDeposits.size() ? monthlyNetDeposits.get(i) : BigDecimal.ZERO;
            if (deposit != null && deposit.compareTo(BigDecimal.ZERO) > 0) {
                // Convert deposit from portfolio currency → USD, then buy units
                BigDecimal depositUsd = toUsd(deposit, portfolioCurrency, fxRate);
                units = units.add(depositUsd.divide(sp500Price, 10, RoundingMode.HALF_UP));
            }

            // Benchmark value in portfolio currency
            BigDecimal valueUsd = units.multiply(sp500Price);
            lastValue = fromUsd(valueUsd, portfolioCurrency, fxRate).setScale(2, RoundingMode.HALF_UP);
            result.add(lastValue);
        }

        return result;
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private void refreshCacheIfNeeded() {
        if (cachedSP500 != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
            return;
        }
        cachedSP500  = fetchSafe(SP500_TICKER,  "S&P 500");
        cachedUsdPln = fetchSafe(USDPLN_TICKER, "USD/PLN");
        cachedUsdEur = fetchSafe(USDEUR_TICKER, "USD/EUR");
        cacheTimestamp = System.currentTimeMillis();
    }

    private Map<YearMonth, BigDecimal> fetchSafe(String ticker, String name) {
        try {
            Map<YearMonth, BigDecimal> data = fetchFromYahoo(ticker);
            log.info("Fetched {} monthly {} data points", data.size(), name);
            return data;
        } catch (Exception e) {
            log.warn("Could not fetch {} data: {}", name, e.getMessage());
            return cachedSP500 != null ? cachedSP500 : Collections.emptyMap(); // keep stale if available
        }
    }

    private Map<YearMonth, BigDecimal> fetchFromYahoo(String ticker) throws Exception {
        String url = YAHOO_BASE + ticker + INTERVAL_RANGE;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; investment-tracker/1.0)")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Yahoo Finance returned HTTP " + response.statusCode() + " for " + ticker);
        }
        return parseYahooChart(response.body());
    }

    private Map<YearMonth, BigDecimal> parseYahooChart(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode result = root.path("chart").path("result");
        if (!result.isArray() || result.isEmpty()) {
            throw new RuntimeException("Unexpected Yahoo Finance response structure");
        }

        JsonNode chartResult  = result.get(0);
        JsonNode timestamps   = chartResult.path("timestamp");

        // Prefer adjclose; fall back to quote close
        JsonNode adjCloseNode = chartResult.path("indicators").path("adjclose");
        JsonNode priceArray;
        if (adjCloseNode.isArray() && !adjCloseNode.isEmpty()) {
            priceArray = adjCloseNode.get(0).path("adjclose");
        } else {
            priceArray = chartResult.path("indicators").path("quote").get(0).path("close");
        }

        if (!timestamps.isArray() || !priceArray.isArray()) {
            throw new RuntimeException("Missing timestamp or price arrays in Yahoo Finance response");
        }

        Map<YearMonth, BigDecimal> prices = new TreeMap<>();
        for (int i = 0; i < timestamps.size() && i < priceArray.size(); i++) {
            JsonNode priceNode = priceArray.get(i);
            if (priceNode == null || priceNode.isNull()) continue;

            long epochSec = timestamps.get(i).asLong();
            LocalDate date = LocalDate.ofEpochDay(epochSec / 86400);
            YearMonth ym   = YearMonth.from(date);
            BigDecimal price = BigDecimal.valueOf(priceNode.asDouble()).setScale(6, RoundingMode.HALF_UP);
            prices.put(ym, price); // last entry per month wins
        }
        return prices;
    }

    /** Look up price for a YearMonth, searching up to 2 months back to handle gaps. */
    private BigDecimal findNearest(Map<YearMonth, BigDecimal> prices, YearMonth ym) {
        for (int offset = 0; offset <= 2; offset++) {
            BigDecimal p = prices.get(ym.minusMonths(offset));
            if (p != null) return p;
        }
        return null;
    }

    /**
     * Returns FX rate: portfolio-currency units per 1 USD.
     *  - PLN: USDPLN rate (e.g. 4.20) — 1 USD = 4.20 PLN
     *  - EUR: USDEUR rate (e.g. 0.92) — 1 USD = 0.92 EUR
     *  - USD: always 1.0
     */
    private BigDecimal getFxRate(String currency, YearMonth ym) {
        return switch (currency.toUpperCase()) {
            case "USD" -> BigDecimal.ONE;
            case "PLN" -> findNearest(cachedUsdPln != null ? cachedUsdPln : Collections.emptyMap(), ym);
            case "EUR" -> findNearest(cachedUsdEur != null ? cachedUsdEur : Collections.emptyMap(), ym);
            default    -> null;
        };
    }

    /** Convert an amount in portfolio currency to USD. fxRate = currency per 1 USD. */
    private BigDecimal toUsd(BigDecimal amount, String currency, BigDecimal fxRate) {
        if ("USD".equalsIgnoreCase(currency)) return amount;
        return amount.divide(fxRate, 10, RoundingMode.HALF_UP);
    }

    /** Convert a USD amount to portfolio currency. fxRate = currency per 1 USD. */
    private BigDecimal fromUsd(BigDecimal amountUsd, String currency, BigDecimal fxRate) {
        if ("USD".equalsIgnoreCase(currency)) return amountUsd;
        return amountUsd.multiply(fxRate);
    }
}
