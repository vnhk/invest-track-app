package com.bervan.investtrack.service;

import com.bervan.investtrack.model.WalletSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class WalletUtils {

    /**
     * Calculates the percentage return for a wallet over a selected period.
     *
     * @param snapshots list of wallet snapshots in chronological order
     * @param year the year to calculate return for
     * @param month if null → calculate for the whole year; if 1-12 → calculate for specific month
     * @return percentage return for the selected period
     */
    public static BigDecimal calculateReturn(List<WalletSnapshot> snapshots, int year, Integer month) {
        if (snapshots.isEmpty()) return BigDecimal.ZERO;

        // Filter snapshots by year and optionally by month
        List<WalletSnapshot> periodSnapshots = snapshots.stream()
                .filter(s -> s.getSnapshotDate().getYear() == year
                        && (month == null || s.getSnapshotDate().getMonthValue() == month))
                .sorted((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .toList();

        if (periodSnapshots.isEmpty()) return BigDecimal.ZERO;

        // Take the first and last snapshot of the period
        WalletSnapshot start = periodSnapshots.get(0);
        WalletSnapshot end = periodSnapshots.get(periodSnapshots.size() - 1);

        // Calculate net deposits in the period (deposits minus withdrawals)
        BigDecimal netDeposits = periodSnapshots.stream()
                .map(s -> s.getMonthlyDeposit().subtract(s.getMonthlyWithdrawal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percentage return:
        // (end balance - (start balance + net deposits)) / (start balance + net deposits) * 100
        return end.getPortfolioValue()
                .subtract(start.getPortfolioValue().add(netDeposits))
                .divide(start.getPortfolioValue().add(netDeposits), 18, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}