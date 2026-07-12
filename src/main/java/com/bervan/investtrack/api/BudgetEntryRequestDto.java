package com.bervan.investtrack.api;

import com.bervan.budget.entry.BudgetEntry;
import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.core.model.FieldMapperConfig;
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
public class BudgetEntryRequestDto implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String category;
    private String currency;
    private BigDecimal value;
    private LocalDate entryDate;
    private String paymentMethod;
    private String entryType;
    private String notes;
    @FieldMapperConfig(mapper = BudgetTagDtoToModelMapper.class)
    private String tags; // comma separated temp will be replaced with Set<BudgetTagDto> in the future when react is ready for multiple selection
    // TODO: add multiple selection in react and replace comma separated logic
    private Boolean isRecurring;
    private LocalDateTime modificationDate;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return BudgetEntry.class;
    }
}

