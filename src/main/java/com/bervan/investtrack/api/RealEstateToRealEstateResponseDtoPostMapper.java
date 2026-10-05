package com.bervan.investtrack.api;

import com.bervan.core.model.PostMapper;
import com.bervan.investtrack.model.RealEstate;
import org.springframework.stereotype.Service;

@Service
public class RealEstateToRealEstateResponseDtoPostMapper implements PostMapper<RealEstate, RealEstateResponseDto> {

    @Override
    public void map(RealEstate entity, RealEstateResponseDto dto) {
        dto.setCurrentValue(entity.getCurrentValue());
        dto.setInitialValue(entity.getInitialValue());
        dto.setReturnRate(entity.getReturnRate());
        dto.setInitialDate(entity.getInitialDate());
        dto.setLatestDate(entity.getLatestDate());
    }

    @Override
    public Class<RealEstate> getFromType() {
        return RealEstate.class;
    }

    @Override
    public Class<RealEstateResponseDto> getToType() {
        return RealEstateResponseDto.class;
    }
}
