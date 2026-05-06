package com.bervan.investtrack.api;

import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import com.bervan.core.model.PostCustomMappers;
import com.bervan.investtrack.model.Wallet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PostCustomMappers(mappers = {WalletToWalletDtoPostMapper.class})
public class WalletDto implements BaseDTO<UUID> {
    private UUID id;
    private String name;
    private String description;
    private String currency;
    private String riskLevel;
    private String walletType;
    private Boolean compareWithSP500;
    private LocalDateTime createdDate;
    private LocalDateTime modificationDate;
    // computed from snapshots — set by WalletToWalletDtoPostMapper
    private BigDecimal currentValue;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalEarnings;
    private BigDecimal returnRate;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Wallet.class;
    }
}
