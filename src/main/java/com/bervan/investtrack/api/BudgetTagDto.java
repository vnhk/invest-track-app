package com.bervan.investtrack.api;

import com.bervan.budget.BudgetEntryTag;
import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BudgetTagDto implements BaseDTO<UUID> {
    private UUID id;
    private String name;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return BudgetEntryTag.class;
    }
}
