package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.investtrack.model.Wallet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreateRequest implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String description;
    private String currency;
    private String riskLevel;
    private String walletType;
    private Boolean compareWithSP500;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Wallet.class;
    }
}
