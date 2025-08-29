package com.bervan.investtrack.model;

import com.bervan.common.model.BervanBaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class WalletSnapshot extends BervanBaseEntity<UUID> {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @NotNull
    private LocalDate snapshotDate;
    @NotNull
    private BigDecimal portfolioValue;
    @NotNull
    private BigDecimal monthlyDeposit;
    @NotNull
    private BigDecimal monthlyWithdrawal;
    @NotNull
    private BigDecimal monthlyEarnings;
    private BigDecimal monthlyReturnRate;
    private String notes;

    private LocalDateTime modificationDate;
    private boolean deleted;


    @Override
    public void setDeleted(Boolean value) {
        this.deleted = value;
    }

    @Override
    public Boolean isDeleted() {
        return deleted;
    }
}