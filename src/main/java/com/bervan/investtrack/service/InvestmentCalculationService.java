package com.bervan.investtrack.service;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class InvestmentCalculationService {

    private static final int SCALE = 8;
    private static final int XIRR_MAX_ITERATIONS = 100;
    private static final double XIRR_TOLERANCE = 1e-7;

    /**
     * CAGR - Compound Annual Growth Rate
     * Formula: (EndValue/StartValue)^(1/years) - 1
     *
     * @param startValue  initial investment value
     * @param endValue    final investment value
     * @param years       number of years (can be fractional)
     * @return CAGR as decimal (0.10 = 10%)
     */
    public BigDecimal calculateCAGR(BigDecimal startValue, BigDecimal endValue, double years) {
        if (startValue == null || endValue == null || startValue.compareTo(BigDecimal.ZERO) <= 0 || years <= 0) {
            return BigDecimal.ZERO;
        }

        double ratio = endValue.divide(startValue, SCALE, RoundingMode.HALF_UP).doubleValue();
        double cagr = Math.pow(ratio, 1.0 / years) - 1.0;

        return BigDecimal.valueOf(cagr).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * CAGR adjusted for inflation
     *
     * @param startValue    initial investment value
     * @param endValue      final investment value
     * @param years         number of years
     * @param inflationRate annual inflation rate as decimal (0.038 = 3.8%)
     * @return real CAGR after inflation
     */
    public BigDecimal calculateRealCAGR(BigDecimal startValue, BigDecimal endValue, double years, BigDecimal inflationRate) {
        BigDecimal nominalCAGR = calculateCAGR(startValue, endValue, years);
        double real = (1 + nominalCAGR.doubleValue()) / (1 + inflationRate.doubleValue()) - 1;
        return BigDecimal.valueOf(real).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * TWR - Time-Weighted Return for a SINGLE wallet's snapshots
     * Formula: Π(1 + Ri) - 1 where Ri is period return
     * This eliminates the impact of cash flows
     *
     * @param snapshots sorted list of wallet snapshots FROM SINGLE WALLET
     * @return TWR as decimal
     */
    public BigDecimal calculateTWR(List<WalletSnapshot> snapshots) {
        if (snapshots == null || snapshots.size() < 2) {
            return BigDecimal.ZERO;
        }

        List<WalletSnapshot> sorted = snapshots.stream()
                .sorted(Comparator.comparing(WalletSnapshot::getSnapshotDate))
                .toList();

        double twrProduct = 1.0;

        for (int i = 1; i < sorted.size(); i++) {
            WalletSnapshot prev = sorted.get(i - 1);
            WalletSnapshot curr = sorted.get(i);

            BigDecimal prevValue = prev.getPortfolioValue();
            BigDecimal currValue = curr.getPortfolioValue();
            BigDecimal cashFlow = curr.getMonthlyDeposit().subtract(curr.getMonthlyWithdrawal());

            // Beginning value for this period = previous end value + cash flow at start of period
            BigDecimal beginValue = prevValue.add(cashFlow);

            if (beginValue.compareTo(BigDecimal.ZERO) > 0) {
                // Period return = (End - Begin) / Begin
                double periodReturn = currValue.subtract(beginValue)
                        .divide(beginValue, SCALE, RoundingMode.HALF_UP)
                        .doubleValue();
                twrProduct *= (1.0 + periodReturn);
            }
        }

        return BigDecimal.valueOf(twrProduct - 1.0).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate simple portfolio return rate (not TWR)
     * This is a simpler metric: (CurrentValue - TotalDeposits) / TotalDeposits
     *
     * @param totalDeposits sum of all deposits minus withdrawals
     * @param currentValue  current portfolio value
     * @return return rate as decimal
     */
    public BigDecimal calculateSimpleReturn(BigDecimal totalDeposits, BigDecimal currentValue) {
        if (totalDeposits == null || currentValue == null ||
                totalDeposits.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return currentValue.subtract(totalDeposits)
                .divide(totalDeposits, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * XIRR - Extended Internal Rate of Return
     * Uses Newton-Raphson method to find the rate that makes NPV = 0
     *
     * @param cashFlows list of cash flows with dates and amounts
     * @return XIRR as decimal
     */
    public BigDecimal calculateXIRR(List<CashFlow> cashFlows) {
        if (cashFlows == null || cashFlows.size() < 2) {
            return BigDecimal.ZERO;
        }

        List<CashFlow> sorted = cashFlows.stream()
                .sorted(Comparator.comparing(CashFlow::date))
                .toList();

        LocalDate firstDate = sorted.get(0).date();

        // Initial guess
        double rate = 0.1;

        for (int i = 0; i < XIRR_MAX_ITERATIONS; i++) {
            double npv = 0;
            double dnpv = 0; // derivative

            for (CashFlow cf : sorted) {
                long days = ChronoUnit.DAYS.between(firstDate, cf.date());
                double years = days / 365.0;
                double factor = Math.pow(1 + rate, years);

                npv += cf.amount().doubleValue() / factor;
                dnpv -= years * cf.amount().doubleValue() / (factor * (1 + rate));
            }

            if (Math.abs(npv) < XIRR_TOLERANCE) {
                return BigDecimal.valueOf(rate).setScale(SCALE, RoundingMode.HALF_UP);
            }

            if (Math.abs(dnpv) < 1e-10) {
                break; // Avoid division by zero
            }

            rate = rate - npv / dnpv;

            // Safety bounds
            if (rate < -0.999) rate = -0.999;
            if (rate > 10) rate = 10;
        }

        return BigDecimal.valueOf(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Convert snapshots to cash flows for XIRR calculation
     * Deposits are negative (outflow), final value is positive (inflow)
     */
    public List<CashFlow> snapshotsToCashFlows(List<WalletSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }

        List<WalletSnapshot> sorted = snapshots.stream()
                .sorted(Comparator.comparing(WalletSnapshot::getSnapshotDate))
                .toList();

        List<CashFlow> cashFlows = new ArrayList<>();

        for (WalletSnapshot snapshot : sorted) {
            BigDecimal netFlow = snapshot.getMonthlyDeposit().subtract(snapshot.getMonthlyWithdrawal());
            if (netFlow.compareTo(BigDecimal.ZERO) != 0) {
                // Deposits are negative cash flows (money going out)
                cashFlows.add(new CashFlow(snapshot.getSnapshotDate(), netFlow.negate()));
            }
        }

        // Add final portfolio value as positive cash flow
        WalletSnapshot last = sorted.get(sorted.size() - 1);
        cashFlows.add(new CashFlow(last.getSnapshotDate(), last.getPortfolioValue()));

        return cashFlows;
    }

    /**
     * Calculate returns per year
     *
     * @param snapshots wallet snapshots
     * @return map of year to return percentage
     */
    public Map<Integer, BigDecimal> calculateYearlyReturns(List<WalletSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, List<WalletSnapshot>> byYear = new TreeMap<>();
        for (WalletSnapshot s : snapshots) {
            int year = s.getSnapshotDate().getYear();
            byYear.computeIfAbsent(year, k -> new ArrayList<>()).add(s);
        }

        Map<Integer, BigDecimal> result = new LinkedHashMap<>();

        for (Map.Entry<Integer, List<WalletSnapshot>> entry : byYear.entrySet()) {
            List<WalletSnapshot> yearSnapshots = entry.getValue();
            yearSnapshots.sort(Comparator.comparing(WalletSnapshot::getSnapshotDate));

            if (yearSnapshots.size() >= 2) {
                WalletSnapshot first = yearSnapshots.get(0);
                WalletSnapshot last = yearSnapshots.get(yearSnapshots.size() - 1);

                BigDecimal yearReturn = calculatePeriodReturn(first, last, yearSnapshots);
                result.put(entry.getKey(), yearReturn.multiply(BigDecimal.valueOf(100)));
            }
        }

        return result;
    }

    /**
     * Build aggregated portfolio time series with carry-forward for multiple wallets
     * Returns a map of date -> (totalBalance, totalCashFlow)
     * For dates where a wallet has no snapshot, uses the last known value
     */
    public Map<LocalDate, PortfolioPoint> buildAggregatedTimeSeries(
            List<Wallet> wallets,
            java.util.function.BiFunction<BigDecimal, String, BigDecimal> currencyConverter) {

        // Collect all unique dates across all wallets
        Set<LocalDate> allDates = new TreeSet<>();
        for (Wallet wallet : wallets) {
            for (WalletSnapshot s : wallet.getSnapshots()) {
                allDates.add(s.getSnapshotDate());
            }
        }

        if (allDates.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<LocalDate, PortfolioPoint> result = new TreeMap<>();

        // For each wallet, build a map date -> snapshot for quick lookup
        Map<UUID, Map<LocalDate, WalletSnapshot>> walletSnapshots = new HashMap<>();
        for (Wallet wallet : wallets) {
            Map<LocalDate, WalletSnapshot> snapshotMap = new HashMap<>();
            for (WalletSnapshot s : wallet.getSnapshots()) {
                snapshotMap.put(s.getSnapshotDate(), s);
            }
            walletSnapshots.put(wallet.getId(), snapshotMap);
        }

        // Track last known values for each wallet
        Map<UUID, BigDecimal> lastKnownBalance = new HashMap<>();
        Map<UUID, BigDecimal> lastKnownCumDeposit = new HashMap<>();

        for (LocalDate date : allDates) {
            BigDecimal totalBalance = BigDecimal.ZERO;
            BigDecimal totalCashFlow = BigDecimal.ZERO;

            for (Wallet wallet : wallets) {
                UUID wId = wallet.getId();
                Map<LocalDate, WalletSnapshot> snapshotMap = walletSnapshots.get(wId);
                WalletSnapshot snapshot = snapshotMap.get(date);

                BigDecimal balance;
                BigDecimal cashFlow;

                if (snapshot != null) {
                    // Convert to PLN
                    balance = currencyConverter.apply(snapshot.getPortfolioValue(), wallet.getCurrency());
                    cashFlow = currencyConverter.apply(
                            snapshot.getMonthlyDeposit().subtract(snapshot.getMonthlyWithdrawal()),
                            wallet.getCurrency());

                    // Update last known values
                    lastKnownBalance.put(wId, balance);
                    BigDecimal prevCum = lastKnownCumDeposit.getOrDefault(wId, BigDecimal.ZERO);
                    lastKnownCumDeposit.put(wId, prevCum.add(cashFlow));
                } else {
                    // Use last known balance (carry-forward)
                    balance = lastKnownBalance.getOrDefault(wId, BigDecimal.ZERO);
                    cashFlow = BigDecimal.ZERO; // No cash flow on this date for this wallet
                }

                totalBalance = totalBalance.add(balance);
                totalCashFlow = totalCashFlow.add(cashFlow);
            }

            result.put(date, new PortfolioPoint(totalBalance, totalCashFlow));
        }

        return result;
    }

    /**
     * Calculate TWR for an aggregated portfolio time series
     */
    public BigDecimal calculateAggregatedTWR(Map<LocalDate, PortfolioPoint> timeSeries) {
        if (timeSeries == null || timeSeries.size() < 2) {
            return BigDecimal.ZERO;
        }

        List<LocalDate> dates = new ArrayList<>(timeSeries.keySet());
        Collections.sort(dates);

        double twrProduct = 1.0;

        for (int i = 1; i < dates.size(); i++) {
            PortfolioPoint prev = timeSeries.get(dates.get(i - 1));
            PortfolioPoint curr = timeSeries.get(dates.get(i));

            BigDecimal prevBalance = prev.balance();
            BigDecimal currBalance = curr.balance();
            BigDecimal cashFlow = curr.cashFlow();

            // Beginning value = previous balance + cash flow at start of period
            BigDecimal beginValue = prevBalance.add(cashFlow);

            if (beginValue.compareTo(BigDecimal.ZERO) > 0) {
                double periodReturn = currBalance.subtract(beginValue)
                        .divide(beginValue, SCALE, RoundingMode.HALF_UP)
                        .doubleValue();
                twrProduct *= (1.0 + periodReturn);
            }
        }

        return BigDecimal.valueOf(twrProduct - 1.0).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public record PortfolioPoint(BigDecimal balance, BigDecimal cashFlow) {}

    /**
     * Calculate monthly returns for heatmap display
     *
     * @param snapshots wallet snapshots
     * @return map of "YYYY-MM" to return percentage
     */
    public Map<String, BigDecimal> calculateMonthlyReturns(List<WalletSnapshot> snapshots) {
        if (snapshots == null || snapshots.size() < 2) {
            return Collections.emptyMap();
        }

        List<WalletSnapshot> sorted = snapshots.stream()
                .sorted(Comparator.comparing(WalletSnapshot::getSnapshotDate))
                .toList();

        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (int i = 1; i < sorted.size(); i++) {
            WalletSnapshot prev = sorted.get(i - 1);
            WalletSnapshot curr = sorted.get(i);

            String key = String.format("%d-%02d",
                    curr.getSnapshotDate().getYear(),
                    curr.getSnapshotDate().getMonthValue());

            BigDecimal prevValue = prev.getPortfolioValue();
            BigDecimal currValue = curr.getPortfolioValue();
            BigDecimal netCashFlow = curr.getMonthlyDeposit().subtract(curr.getMonthlyWithdrawal());

            if (prevValue.compareTo(BigDecimal.ZERO) > 0) {
                // Adjust for cash flow to get actual return
                BigDecimal adjustedPrev = prevValue.add(netCashFlow);
                if (adjustedPrev.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal returnPct = currValue.subtract(adjustedPrev)
                            .divide(adjustedPrev, SCALE, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    result.put(key, returnPct);
                }
            }
        }

        return result;
    }

    /**
     * Calculate period return with cash flow adjustment
     */
    private BigDecimal calculatePeriodReturn(WalletSnapshot first, WalletSnapshot last,
                                              List<WalletSnapshot> periodSnapshots) {
        BigDecimal startValue = first.getPortfolioValue();
        BigDecimal endValue = last.getPortfolioValue();

        // Sum all cash flows in the period (excluding first snapshot)
        BigDecimal totalCashFlow = BigDecimal.ZERO;
        for (int i = 1; i < periodSnapshots.size(); i++) {
            WalletSnapshot s = periodSnapshots.get(i);
            totalCashFlow = totalCashFlow.add(s.getMonthlyDeposit().subtract(s.getMonthlyWithdrawal()));
        }

        BigDecimal adjustedStart = startValue.add(totalCashFlow);

        if (adjustedStart.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return endValue.subtract(adjustedStart)
                .divide(adjustedStart, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Cash flow record for XIRR calculation
     */
    public record CashFlow(LocalDate date, BigDecimal amount) {}
}
