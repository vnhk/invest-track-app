package com.bervan.investtrack.api;

import com.bervan.core.model.PostMapper;
import com.bervan.investtrack.model.Vehicle;
import org.springframework.stereotype.Service;

@Service
public class VehicleToVehicleResponseDtoPostMapper implements PostMapper<Vehicle, VehicleResponseDto> {

    @Override
    public void map(Vehicle entity, VehicleResponseDto dto) {
        dto.setCurrentValue(entity.getCurrentValue());
        dto.setInitialValue(entity.getInitialValue());
        dto.setReturnRate(entity.getReturnRate());
        dto.setInitialDate(entity.getInitialDate());
        dto.setLatestDate(entity.getLatestDate());
    }

    @Override
    public Class<Vehicle> getFromType() {
        return Vehicle.class;
    }

    @Override
    public Class<VehicleResponseDto> getToType() {
        return VehicleResponseDto.class;
    }
}
