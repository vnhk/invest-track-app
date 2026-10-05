package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.investtrack.model.Valuable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValuableCreateRequest implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String description;
    private String valuableType;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Valuable.class;
    }
}
