package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.investtrack.model.RealEstate;
import com.bervan.investtrack.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateRequest implements BaseDTO<UUID> {
    private UUID id;
    private String brand;
    private String model;
    private String description;
    private String vehicleType;
    private Integer productionYear;
    private BigDecimal currentValue;
    private BigDecimal purchasePrice;
    private BigDecimal purchaseCosts;
    private LocalDate purchaseDate;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Vehicle.class;
    }
}
