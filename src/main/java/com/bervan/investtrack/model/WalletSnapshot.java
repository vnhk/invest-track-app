package com.bervan.investtrack.model;

import com.bervan.common.model.BervanBaseEntity;
import com.bervan.common.model.PersistableTableData;
import com.bervan.common.model.VaadinBervanColumn;
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
public class WalletSnapshot extends BervanBaseEntity<UUID> implements PersistableTableData<UUID> {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @NotNull
    @VaadinBervanColumn(displayName = "Snapshot Date", internalName = "snapshotDate")
    private LocalDate snapshotDate;
    @NotNull
    @VaadinBervanColumn(displayName = "Portfolio Value", internalName = "portfolioValue")
    private BigDecimal portfolioValue;
    @NotNull
    @VaadinBervanColumn(displayName = "Total Deposit", internalName = "totalDeposit")
    private BigDecimal monthlyDeposit;
    @NotNull
    @VaadinBervanColumn(displayName = "Total Withdrawal", internalName = "totalWithdrawal")
    private BigDecimal monthlyWithdrawal;
    @NotNull
    @VaadinBervanColumn(displayName = "Total Earnings", internalName = "totalEarnings")
    private BigDecimal monthlyEarnings;
    @VaadinBervanColumn(displayName = "Notes", internalName = "notes")
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