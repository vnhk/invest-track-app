package com.bervan.investtrack.model;

import com.bervan.common.model.BervanBaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
public class WalletTransaction extends BervanBaseEntity<UUID> {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    private String transactionType;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private String category;

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

//    public enum TransactionType {
//        DEPOSIT("Deposit"),
//        WITHDRAWAL("Withdrawal"),
//        EARNING("Earning"),
//        LOSS("Loss"),
//        FEE("Fee"),
//        DIVIDEND("DIVIDEND"),
//        INTEREST("INTEREST");
//
//        private final String displayName;
//
//        TransactionType(String displayName) {
//            this.displayName = displayName;
//        }
//
//        public String getDisplayName() {
//            return displayName;
//        }
//    }

}