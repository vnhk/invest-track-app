package com.bervan.investtrack.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

@Service
public class CurrencyConverter {

    private final Map<Currency, BigDecimal> plnPerUnit = new EnumMap<>(Currency.class);
    private final int scale;
    private final RoundingMode roundingMode;

    public CurrencyConverter() {
        this.scale = 4;
        this.roundingMode = RoundingMode.HALF_UP;
        plnPerUnit.put(Currency.PLN, BigDecimal.ONE);
        plnPerUnit.put(Currency.EUR, new BigDecimal("4.30")); //load on startup
        plnPerUnit.put(Currency.USD, new BigDecimal("3.70"));
    }

    public CurrencyConverter(Map<Currency, BigDecimal> initialRates, int scale, RoundingMode roundingMode) {
        this.scale = scale;
        this.roundingMode = roundingMode;
        plnPerUnit.putAll(initialRates);
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

    /** Bulk update of rates. */
    public void updateRates(Map<Currency, BigDecimal> rates) {
        if (rates == null) throw new IllegalArgumentException("rates must not be null");
        plnPerUnit.putAll(rates);
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
