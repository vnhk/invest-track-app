package com.bervan.investments.recommendation;

import com.bervan.common.model.BervanOwnedBaseEntity;
import com.bervan.common.model.PersistableTableOwnedData;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

// Low-Code START
@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class InvestmentRecommendation extends BervanOwnedBaseEntity<UUID> implements PersistableTableOwnedData<UUID> {

    // Default constructor
    public InvestmentRecommendation() {
        // constructor body
    }

    @Id
    private UUID id;

    private Boolean deleted = false;

    private LocalDateTime modificationDate;

    private BigDecimal changeInPercentEvening;

    private BigDecimal changeInPercentMorning;

    private String date;

    private String symbol;

    private String strategy;

    private String recommendationType; //risky/good/best

    private String recommendationResult; //Good/Bad

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
