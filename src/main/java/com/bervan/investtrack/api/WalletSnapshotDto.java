package com.bervan.investtrack.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletSnapshotDto {
    private UUID id;
    private UUID walletId;
    private LocalDate snapshotDate;
    private BigDecimal portfolioValue;
    private BigDecimal monthlyDeposit;
    private BigDecimal monthlyWithdrawal;
    private BigDecimal monthlyEarnings;
    private String notes;
}
