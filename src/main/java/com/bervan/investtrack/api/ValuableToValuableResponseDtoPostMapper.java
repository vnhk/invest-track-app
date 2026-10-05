package com.bervan.investtrack.api;

import com.bervan.core.model.PostMapper;
import com.bervan.investtrack.model.Valuable;
import org.springframework.stereotype.Service;

@Service
public class ValuableToValuableResponseDtoPostMapper implements PostMapper<Valuable, ValuableResponseDto> {

    @Override
    public void map(Valuable entity, ValuableResponseDto dto) {
        dto.setCurrentValue(entity.getCurrentValue());
        dto.setInitialValue(entity.getInitialValue());
        dto.setReturnRate(entity.getReturnRate());
        dto.setInitialDate(entity.getInitialDate());
        dto.setLatestDate(entity.getLatestDate());
    }

    @Override
    public Class<Valuable> getFromType() {
        return Valuable.class;
    }

    @Override
    public Class<ValuableResponseDto> getToType() {
        return ValuableResponseDto.class;
    }
}
