package com.bervan.investtrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SP500DataService.calculateBenchmarkValues().
 *
 * Cache is injected via reflection so no HTTP calls are made.
 *
 * SP500 prices used in tests:
 *   2024-01: 5000, 2024-02: 6000, 2024-03: 6000
 * USD/PLN rates: 4.0 (all months)
 * USD/EUR rates: 0.9 (all months)
 */
class SP500DataServiceTest {

    private SP500DataService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SP500DataService();

        Map<YearMonth, BigDecimal> sp500 = new TreeMap<>();
        sp500.put(YearMonth.of(2024, 1), new BigDecimal("5000"));
        sp500.put(YearMonth.of(2024, 2), new BigDecimal("6000"));
        sp500.put(YearMonth.of(2024, 3), new BigDecimal("6000"));

        Map<YearMonth, BigDecimal> usdPln = new TreeMap<>();
        usdPln.put(YearMonth.of(2024, 1), new BigDecimal("4.0"));
        usdPln.put(YearMonth.of(2024, 2), new BigDecimal("4.0"));
        usdPln.put(YearMonth.of(2024, 3), new BigDecimal("4.0"));

        Map<YearMonth, BigDecimal> usdEur = new TreeMap<>();
        usdEur.put(YearMonth.of(2024, 1), new BigDecimal("0.9"));
        usdEur.put(YearMonth.of(2024, 2), new BigDecimal("0.9"));
        usdEur.put(YearMonth.of(2024, 3), new BigDecimal("0.9"));

        setField(service, "cachedSP500", sp500);
        setField(service, "cachedUsdPln", usdPln);
        setField(service, "cachedUsdEur", usdEur);
        setField(service, "cacheTimestamp", System.currentTimeMillis());
    }

    // ── USD ───────────────────────────────────────────────────────────────────

    @Test
    void usd_singleDeposit_growsWithSP500() {
        // deposit 1000 USD when SP500=5000 → 0.2 units
        // month 2: SP500=6000 → benchmark = 0.2 * 6000 = 1200
        List<String> dates = List.of("01-01-2024", "01-02-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("1000"), BigDecimal.ZERO);

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "USD");

        assertEquals(2, result.size());
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.get(0)),
                "Month 1 benchmark should equal deposit (no growth yet)");
        assertEquals(0, new BigDecimal("1200.00").compareTo(result.get(1)),
                "Month 2 benchmark = 0.2 units * 6000");
    }

    @Test
    void usd_twoDeposits_accumulateUnits() {
        // month 1: deposit 1000, SP500=5000 → 0.2 units, value=1000
        // month 2: deposit 600, SP500=6000 → +0.1 units → 0.3 units, value=1800
        List<String> dates = List.of("01-01-2024", "01-02-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("1000"), new BigDecimal("600"));

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "USD");

        assertEquals(0, new BigDecimal("1000.00").compareTo(result.get(0)));
        assertEquals(0, new BigDecimal("1800.00").compareTo(result.get(1)),
                "Month 2: (0.2+0.1)*6000 = 1800");
    }

    @Test
    void usd_withdrawal_sellsUnits() {
        // month 1: deposit 1000 USD, SP500=5000 → 0.2 units, benchmark=1000
        // month 2: withdraw 500 USD (deposit=-500), SP500=6000
        //   sell: 500/6000 ≈ 0.0833 units → remaining ≈ 0.1167 units
        //   benchmark = 0.1167 * 6000 ≈ 700
        // month 3: no flow, SP500=6000 → benchmark ≈ 700
        List<String> dates = List.of("01-01-2024", "01-02-2024", "01-03-2024");
        List<BigDecimal> deposits = List.of(
                new BigDecimal("1000"),
                new BigDecimal("-500"),
                BigDecimal.ZERO);

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "USD");

        assertEquals(3, result.size());
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.get(0)));
        // after withdraw: units = 0.2 - 500/6000 = 0.2 - 0.0833... = 0.1166...
        // value = 0.1166... * 6000 = 700
        assertEquals(0, new BigDecimal("700.00").compareTo(result.get(1)),
                "After withdrawal of 500 at price 6000: 1200 - 500 = 700");
        assertEquals(0, new BigDecimal("700.00").compareTo(result.get(2)),
                "Month 3: same units, same price → unchanged");
    }

    @Test
    void usd_withdrawalExceedsHoldings_clampsToZero() {
        // deposit 1000 then withdraw 9999 → should not go negative
        List<String> dates = List.of("01-01-2024", "01-02-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("1000"), new BigDecimal("-9999"));

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "USD");

        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(1)),
                "Benchmark should clamp to 0 when withdrawal exceeds holdings");
    }

    @Test
    void usd_noDeposits_benchmarkStaysZero() {
        List<String> dates = List.of("01-01-2024", "01-02-2024");
        List<BigDecimal> deposits = List.of(BigDecimal.ZERO, BigDecimal.ZERO);

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "USD");

        result.forEach(v -> assertEquals(0, BigDecimal.ZERO.compareTo(v)));
    }

    // ── PLN ───────────────────────────────────────────────────────────────────

    @Test
    void pln_depositConvertedToUsdThenBack() {
        // deposit 4000 PLN, USD/PLN=4.0, SP500=5000
        // depositUSD = 4000/4.0 = 1000 → units = 0.2
        // benchmarkUSD = 0.2 * 5000 = 1000 → benchmarkPLN = 1000 * 4.0 = 4000
        List<String> dates = List.of("01-01-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("4000"));

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "PLN");

        assertEquals(0, new BigDecimal("4000.00").compareTo(result.get(0)),
                "4000 PLN deposited at fx=4.0, SP500=5000 → benchmark = 4000 PLN");
    }

    @Test
    void pln_growthReflectedInLocalCurrency() {
        // month 1: deposit 4000 PLN, fx=4.0, SP500=5000 → 0.2 units
        // month 2: SP500=6000, fx=4.0 → 0.2 * 6000 * 4.0 = 4800 PLN
        List<String> dates = List.of("01-01-2024", "01-02-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("4000"), BigDecimal.ZERO);

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "PLN");

        assertEquals(0, new BigDecimal("4800.00").compareTo(result.get(1)),
                "0.2 units * 6000 * fx(4.0) = 4800 PLN");
    }

    @Test
    void pln_withdrawal_sellsUnitsConvertingViaFx() {
        // month 1: 4000 PLN → 1000 USD → 0.2 units, benchmark=4000 PLN
        // month 2: withdraw 2400 PLN at SP500=6000, fx=4.0
        //   withdrawUSD = 2400/4.0 = 600 → sell 600/6000 = 0.1 units → 0.1 units left
        //   benchmark = 0.1 * 6000 * 4.0 = 2400 PLN
        List<String> dates = List.of("01-01-2024", "01-02-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("4000"), new BigDecimal("-2400"));

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "PLN");

        assertEquals(0, new BigDecimal("2400.00").compareTo(result.get(1)),
                "After 2400 PLN withdrawal: 0.1 units * 6000 * 4.0 = 2400 PLN");
    }

    // ── EUR ───────────────────────────────────────────────────────────────────

    @Test
    void eur_depositConvertedViaFxRate() {
        // deposit 900 EUR, USD/EUR=0.9, SP500=5000
        // depositUSD = 900/0.9 = 1000 → units = 0.2
        // benchmarkUSD = 1000 → benchmarkEUR = 1000 * 0.9 = 900
        List<String> dates = List.of("01-01-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("900"));

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "EUR");

        assertEquals(0, new BigDecimal("900.00").compareTo(result.get(0)),
                "900 EUR at fx=0.9 → depositUSD=1000 → benchmark=900 EUR");
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    void emptyInput_returnsEmptyList() {
        assertTrue(service.calculateBenchmarkValues(List.of(), List.of(), "USD").isEmpty());
        assertTrue(service.calculateBenchmarkValues(null, List.of(), "USD").isEmpty());
    }

    @Test
    void unknownCurrency_returnsEmptyList() {
        // getFxRate returns null for unknown currency → each month is skipped
        List<String> dates = List.of("01-01-2024");
        List<BigDecimal> deposits = List.of(new BigDecimal("1000"));

        List<BigDecimal> result = service.calculateBenchmarkValues(dates, deposits, "GBP");

        // Unknown currency → fxRate null → skipped, returns lastValue=0 for each
        assertEquals(1, result.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0)));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
