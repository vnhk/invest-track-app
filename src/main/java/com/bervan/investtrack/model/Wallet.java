package com.bervan.investtrack.model;

import com.bervan.common.model.BervanBaseEntity;
import com.bervan.common.model.PersistableTableData;
import com.bervan.core.model.BaseModel;
import com.bervan.ieentities.ExcelIEEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
//@HistorySupported
@Getter
@Setter
public class Wallet extends BervanBaseEntity<UUID> implements PersistableTableData<UUID>, ExcelIEEntity<UUID>, BaseModel<UUID> {
    @Id
    private UUID id;

    private String name;
    private String description;
    private String currency;

    @Transient
    private BigDecimal initialValue = BigDecimal.ZERO;
    @Transient
    private BigDecimal currentValue = BigDecimal.ZERO;
    @Transient
    private BigDecimal totalDeposits = BigDecimal.ZERO;
    @Transient
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;
    @Transient
    private BigDecimal totalEarnings = BigDecimal.ZERO;
    @Transient
    private BigDecimal returnRate = BigDecimal.ZERO;

    private String riskLevel;
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
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

    public BigDecimal calculateTotalReturn() {
        return currentValue.subtract(calculateNetInvestment());
    }

    private BigDecimal calculateNetInvestment() {
        return totalDeposits.subtract(totalWithdrawals);
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

    public void updateReturnRate() {
        BigDecimal netInvestment = calculateNetInvestment();
        if (netInvestment.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalReturn = calculateTotalReturn();
            this.returnRate = totalReturn.divide(netInvestment, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            this.returnRate = BigDecimal.ZERO;
        }
    }

}