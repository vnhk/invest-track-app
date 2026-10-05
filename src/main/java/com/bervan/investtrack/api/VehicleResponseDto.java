package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.core.model.PostCustomMappers;
import com.bervan.investtrack.model.Vehicle;
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
@PostCustomMappers(mappers = {VehicleToVehicleResponseDtoPostMapper.class})
public class VehicleResponseDto implements BaseDTO<UUID> {
    private UUID id;
    private String brand;
    private String model;
    private String description;
    private String vehicleType;
    private Integer productionYear;
    private BigDecimal currentValue;
    private BigDecimal initialValue;
    private BigDecimal returnRate;
    private LocalDate initialDate;
    private LocalDate latestDate;
    private LocalDateTime createdDate;
    private LocalDateTime modificationDate;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Vehicle.class;
    }
}
