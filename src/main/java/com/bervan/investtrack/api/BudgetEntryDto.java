package com.bervan.investtrack.api;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEntryDto implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String category;
    private String currency;
    private BigDecimal value;
    private LocalDate entryDate;
    private String paymentMethod;
    private String entryType;
    private String notes;
    private Boolean isRecurring;
    private LocalDateTime modificationDate;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        @SuppressWarnings("unchecked")
        Class<? extends BaseModel<UUID>> t = (Class<? extends BaseModel<UUID>>)(Class<?>) BudgetEntry.class;
        return t;
    }
}

