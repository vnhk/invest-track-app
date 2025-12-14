package com.bervan.investtrack.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import com.bervan.common.model.PersistableTableOwnedData;
import com.bervan.ieentities.ExcelIEEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Where(clause = "deleted = false or deleted is null")
public class WalletSnapshot extends BervanOwnedBaseEntity<UUID> implements PersistableTableOwnedData<UUID>, ExcelIEEntity<UUID> {
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

    @Override
    public String getTableFilterableColumnValue() {
        return snapshotDate.toString();
    }
}