package com.bervan.budget.entry;

import com.bervan.common.model.BervanBaseEntity;
import com.bervan.common.model.PersistableTableData;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Low-Code START
@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class BudgetEntry extends BervanBaseEntity<UUID> implements PersistableTableData<UUID> {

    @Id
    private UUID id;
    private Boolean deleted = false;
    private LocalDateTime modificationDate;
    private String category;
    private Boolean isRecurring;
    private String name;
//    @CollectionTable(name = "budget_entry_owners")
//    private List<String> entryOwners = new ArrayList<>();
    private String currency;
    private BigDecimal value;
    private LocalDate entryDate;
    private String paymentMethod;
    private String entryType;
    private String notes;

    // Default constructor
    public BudgetEntry() {
        // constructor body
    }

    public BudgetEntry(String name) {
        this.name = name;
    }

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
        return modificationDate;
    }

    @Override
    public void setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    @Override
    public String getTableFilterableColumnValue() {
        return id.toString();
    }
}
// Low-Code END
