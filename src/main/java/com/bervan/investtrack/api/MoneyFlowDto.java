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
public class MoneyFlowDto {
    private BigDecimal cashFlow;
    private BigDecimal bankFlow;
    private String fromDate;
    private String toDate;
}

