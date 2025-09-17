package com.bervan.investtrack.model;

import com.bervan.common.model.BervanBaseEntity;
import com.bervan.common.model.PersistableTableData;
import com.bervan.common.model.VaadinBervanColumn;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
public class StockPriceAlertConfig extends BervanBaseEntity<UUID> implements PersistableTableData<UUID> {
    @Id
    private UUID id;
    @VaadinBervanColumn(displayName = "Price", internalName = "price")
    private BigDecimal price;
    @VaadinBervanColumn(displayName = "Operator", internalName = "operator", strValues = {"<=", ">="})
    private String operator;
    @VaadinBervanColumn(displayName = "How many times notify?", internalName = "amountOfNotifications")
    private Integer amountOfNotifications = 1; //decrease every time
    @VaadinBervanColumn(displayName = "How Often Check (m)?", internalName = "checkIntervalMinutes")
    private Integer checkIntervalMinutes = 60;
    @VaadinBervanColumn(displayName = "How many % change to notify again?", internalName = "anotherNotificationEachPercentage")
    private Integer anotherNotificationEachPercentage = 10;
    private LocalDateTime previouslyNotifiedDate;
    private BigDecimal previouslyNotifiedPrice;
    private LocalDateTime previouslyCheckedDate;
    @OneToOne(cascade = CascadeType.ALL)
    private StockPriceAlert stockPriceAlert;
    private Boolean deleted = false;

    @Override
    public Boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(Boolean value) {
        this.deleted = value;
    }

    @Override
    public LocalDateTime getModificationDate() {
        return null;
    }

    @Override
    public void setModificationDate(LocalDateTime modificationDate) {

    }

    @Override
    public String getTableFilterableColumnValue() {
        return id.toString();
    }
}
