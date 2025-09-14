package com.bervan.investtrack.model;

import com.bervan.common.model.BervanBaseEntity;
import com.bervan.common.model.PersistableTableData;
import com.bervan.common.model.VaadinBervanColumn;
import com.bervan.common.model.VaadinDynamicMultiDropdownBervanColumn;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
public class StockPriceAlert extends BervanBaseEntity<UUID> implements PersistableTableData<UUID> {
    @Id
    private UUID id;

    @VaadinBervanColumn(displayName = "Alert Name", internalName = "alertName")
    private String name;
    @VaadinBervanColumn(displayName = "Symbol", internalName = "symbol")
    private String symbol;
    @VaadinBervanColumn(displayName = "Exchange", internalName = "exchange", strValues = {"GPW"})
    private String exchange;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stock_price_alert_emails", joinColumns = @JoinColumn(name = "stock_price_alert_id"))
    @Column(name = "email")
    @VaadinBervanColumn(displayName = "Emails", internalName = "emails", extension = VaadinDynamicMultiDropdownBervanColumn.class)
    private List<String> emails = new ArrayList<>();
    @OneToOne(cascade = CascadeType.ALL)
    private StockPriceAlertConfig stockPriceAlertConfig;
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
