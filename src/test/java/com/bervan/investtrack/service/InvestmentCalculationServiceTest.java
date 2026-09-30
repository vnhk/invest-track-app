package com.bervan.investtrack.service;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentCalculationServiceTest {

    private InvestmentCalculationService service;

    @BeforeEach
    void setUp() {
        service = new InvestmentCalculationService();
    }

    @Test
    @DisplayName("calculateCAGRFromTWR returns unannualized TWR for periods < 1 year")
    void cagrForShortPeriodReturnsTwr() {
        BigDecimal twr = new BigDecimal("0.0183"); // 1.83%
        double years = 30.0 / 365.0; // ~0.082 years (1 month)

        BigDecimal cagr = service.calculateCAGRFromTWR(twr, years);

        assertThat(cagr).isEqualByComparingTo(new BigDecimal("0.01830000"));
    }

    @Test
    @DisplayName("calculateCAGRFromTWR compounds properly for periods >= 1 year")
    void cagrForMultiYearPeriodCompoundsCorrectly() {
        // e.g. 21% total TWR over 2 years -> CAGR = (1 + 0.21)^(1/2) - 1 = 10%
        BigDecimal twr = new BigDecimal("0.21000000");
        double years = 2.0;

        BigDecimal cagr = service.calculateCAGRFromTWR(twr, years);

        assertThat(cagr).isEqualByComparingTo(new BigDecimal("0.10000000"));
    }

    @Test
    @DisplayName("TWR and CAGR with significant deposits do not produce astronomical returns")
    void walletWithLargeDepositComputesSensibleTwrAndCagr() {
        Wallet wallet = new Wallet();
        wallet.setName("Test Wallet");

        WalletSnapshot s1 = new WalletSnapshot();
        s1.setWallet(wallet);
        s1.setSnapshotDate(LocalDate.of(2026, 8, 31));
        s1.setPortfolioValue(new BigDecimal("500.00"));
        s1.setMonthlyDeposit(new BigDecimal("500.00"));
        s1.setMonthlyWithdrawal(BigDecimal.ZERO);

        WalletSnapshot s2 = new WalletSnapshot();
        s2.setWallet(wallet);
        s2.setSnapshotDate(LocalDate.of(2026, 9, 30));
        s2.setPortfolioValue(new BigDecimal("3564.00"));
        s2.setMonthlyDeposit(new BigDecimal("3000.00"));
        s2.setMonthlyWithdrawal(BigDecimal.ZERO);

        List<WalletSnapshot> snapshots = List.of(s1, s2);

        BigDecimal twr = service.calculateTWR(snapshots);
        // Period 1: start=500 + 3000 = 3500, end=3564 -> return = 64/3500 = 0.01828571 (1.83%)
        assertThat(twr).isEqualByComparingTo(new BigDecimal("0.01828571"));

        double years = 30.0 / 365.0;
        BigDecimal cagr = service.calculateCAGRFromTWR(twr, years);
        assertThat(cagr).isEqualByComparingTo(new BigDecimal("0.01828571"));
    }

    @Test
    @DisplayName("calculateCAGRFromTWR handles edge cases gracefully")
    void cagrEdgeCases() {
        assertThat(service.calculateCAGRFromTWR(null, 2.0)).isEqualTo(BigDecimal.ZERO);
        assertThat(service.calculateCAGRFromTWR(new BigDecimal("0.10"), 0)).isEqualTo(BigDecimal.ZERO);
        assertThat(service.calculateCAGRFromTWR(new BigDecimal("-1.00"), 2.0)).isEqualByComparingTo(new BigDecimal("-1.00000000"));
    }
}
