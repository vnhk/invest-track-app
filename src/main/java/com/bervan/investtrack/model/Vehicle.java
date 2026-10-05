package com.bervan.investtrack.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import com.bervan.common.model.PersistableTableOwnedData;
import com.bervan.core.model.BaseModel;
import com.bervan.ieentities.ExcelIEEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
@Where(clause = "deleted = false or deleted is null")
public class Vehicle extends BervanOwnedBaseEntity<UUID> implements PersistableTableOwnedData<UUID>, ExcelIEEntity<UUID>, BaseModel<UUID> {
    @Id
    private UUID id;
    private String description;
    private String brand;
    private String model;
    private Integer productionYear;
    private String vehicleType;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("snapshotDate ASC")
    @Where(clause = "deleted = false or deleted is null")
    private List<VehicleSnapshot> snapshots = new ArrayList<>();

    private LocalDateTime createdDate;
    private LocalDateTime modificationDate;
    private boolean deleted;

    public Vehicle() {
        this.createdDate = LocalDateTime.now();
    }

    @Transient
    public BigDecimal getInitialValue() {
        if (snapshots == null || snapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return snapshots.stream()
                .min((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .map(VehicleSnapshot::getAssetValue)
                .orElse(BigDecimal.ZERO);
    }

    @Transient
    public BigDecimal getCurrentValue() {
        if (snapshots == null || snapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return snapshots.stream()
                .max((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .map(VehicleSnapshot::getAssetValue)
                .orElse(BigDecimal.ZERO);
    }

    @Transient
    public BigDecimal getReturnRate() {
        BigDecimal initial = getInitialValue();
        if (initial != null && initial.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal current = getCurrentValue();
            return current.subtract(initial)
                    .divide(initial, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Transient
    public LocalDate getInitialDate() {
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }
        return snapshots.stream()
                .min((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .map(VehicleSnapshot::getSnapshotDate)
                .orElse(null);
    }

    @Transient
    public LocalDate getLatestDate() {
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }
        return snapshots.stream()
                .max((s1, s2) -> s1.getSnapshotDate().compareTo(s2.getSnapshotDate()))
                .map(VehicleSnapshot::getSnapshotDate)
                .orElse(null);
    }

    @Override
    public String getTableFilterableColumnValue() {
        return (brand != null ? brand : "") + " " + (model != null ? model : "");
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

    @Override
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}