package com.bervan.investtrack.model;

public enum WalletType {
    INVESTMENT("Investment"),
    SAVINGS("Savings"),
    BONDS("Bonds"),
    CRYPTO("Crypto"),
    CASH("Cash");

    private final String displayName;

    WalletType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isInvestmentLike() {
        return this == INVESTMENT || this == BONDS || this == CRYPTO;
    }
}
