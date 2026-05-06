package com.bervan.investtrack.api;

import com.bervan.core.model.PostMapper;
import com.bervan.investtrack.model.Wallet;
import org.springframework.stereotype.Service;

@Service
public class WalletToWalletDtoPostMapper implements PostMapper<Wallet, WalletDto> {

    @Override
    public void map(Wallet wallet, WalletDto dto) {
        dto.setCurrentValue(wallet.getCurrentValue());
        dto.setTotalDeposits(wallet.getTotalDeposits());
        dto.setTotalWithdrawals(wallet.getTotalWithdrawals());
        dto.setTotalEarnings(wallet.getTotalEarnings());
        dto.setReturnRate(wallet.getReturnRate());
    }

    @Override
    public Class<Wallet> getFromType() {
        return Wallet.class;
    }

    @Override
    public Class<WalletDto> getToType() {
        return WalletDto.class;
    }
}
