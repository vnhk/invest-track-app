package com.bervan.investtrack.model;

import com.bervan.common.model.BervanBaseEntity;
import com.bervan.common.model.PersistableTableData;
import com.bervan.core.model.BaseModel;
import com.bervan.ieentities.ExcelIEEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
@Where(clause = "deleted = false or deleted is null")
public class Wallet extends BervanBaseEntity<UUID> implements PersistableTableData<UUID>, ExcelIEEntity<UUID>, BaseModel<UUID> {
    @Id
    private UUID id;

    private String name;
    private String description;
    private String currency;
    private String riskLevel;
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("snapshotDate ASC")
    @Where(clause = "deleted = false or deleted is null")
    private List<WalletSnapshot> snapshots = new ArrayList<>();

    private LocalDateTime modificationDate;
    private boolean deleted;

    public Wallet() {
        this.createdDate = LocalDateTime.now();
    }

    public Wallet(String name, String description, String currency) {
        this();
        this.name = name;
        this.description = description;
        this.currency = currency;
    }

    // Calculate fields dynamically from snapshots
    @Transient
    public BigDecimal getInitialValue() {
        if (snapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return snapshots.stream()
                .min((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .map(WalletSnapshot::getPortfolioValue)
                .orElse(BigDecimal.ZERO);
    }

    @Transient
    public BigDecimal getCurrentValue() {
        if (snapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return snapshots.stream()
                .max((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .map(WalletSnapshot::getPortfolioValue)
                .orElse(BigDecimal.ZERO);
    }

    @Transient
    public BigDecimal getTotalDeposits() {
        return snapshots.stream()
                .map(WalletSnapshot::getMonthlyDeposit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getTotalWithdrawals() {
        return snapshots.stream()
                .map(WalletSnapshot::getMonthlyWithdrawal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getTotalEarnings() {
        return snapshots.stream()
                .map(WalletSnapshot::getMonthlyEarnings)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getReturnRate() {
        BigDecimal netInvestment = calculateNetInvestment();
        if (netInvestment.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalReturn = calculateTotalReturn();
            return totalReturn.divide(netInvestment, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal calculateTotalReturn() {
        return getCurrentValue().subtract(calculateNetInvestment());
    }

    public BigDecimal calculateNetInvestment() {
        return getTotalDeposits().subtract(getTotalWithdrawals());
    }

    public String getTableFilterableColumnValue() {
        return name;
    }

    @Override
    public LocalDateTime getModificationDate() {
        return modificationDate;
    }

    @Override
    public void setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    @Override
    public Boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}