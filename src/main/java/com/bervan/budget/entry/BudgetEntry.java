package com.bervan.budget.entry;

import com.bervan.budget.BudgetEntryTag;
import com.bervan.common.model.BervanBaseEntity;
import com.bervan.common.model.PersistableTableData;
import com.bervan.core.model.BaseModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Low-Code START
@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@AllArgsConstructor
public class BudgetEntry extends BervanBaseEntity<UUID> implements PersistableTableData<UUID>, BaseModel<UUID> {

    @Id
    private UUID id;
    private String name;
    private Boolean deleted = false;
    private LocalDateTime modificationDate;
    private String category;
    private String currency;
    private BigDecimal value;
    private LocalDate entryDate;
    private String paymentMethod;
    private String entryType;
    private String notes;
    private Boolean isRecurring;
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "budget_entry_tags",
            joinColumns = @JoinColumn(name = "budget_entry_id"),
            inverseJoinColumns = @JoinColumn(name = "budget_tag_id")
    )
    private Set<BudgetEntryTag> tags = new HashSet<>();

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
