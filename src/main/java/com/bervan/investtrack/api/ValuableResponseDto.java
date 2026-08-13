package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.investtrack.model.RealEstate;
import com.bervan.investtrack.model.Valuable;
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
public class ValuableResponseDto implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String description;
    private String valuableType;
    private BigDecimal currentValue;
    private BigDecimal purchasePrice;
    private BigDecimal purchaseCosts;
    private LocalDate purchaseDate;
    private LocalDateTime createdDate;
    private LocalDateTime modificationDate;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Valuable.class;
    }
}
