package com.bervan.investtrack.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotCreateRequest {
    private LocalDate snapshotDate;
    private BigDecimal portfolioValue;
    private BigDecimal monthlyDeposit;
    private BigDecimal monthlyWithdrawal;
    private BigDecimal monthlyEarnings;
    private String notes;
}
