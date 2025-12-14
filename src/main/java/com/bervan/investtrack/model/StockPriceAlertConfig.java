package com.bervan.investtrack.model;

import com.bervan.common.model.BervanOwnedBaseEntity;
import com.bervan.common.model.PersistableTableOwnedData;
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
public class StockPriceAlertConfig extends BervanOwnedBaseEntity<UUID> implements PersistableTableOwnedData<UUID> {
    @Id
    private UUID id;
    private BigDecimal price;
    private String operator;
    private Integer amountOfNotifications = 1; //decrease every time
    private Integer checkIntervalMinutes = 60;
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
