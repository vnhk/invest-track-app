package com.bervan.investtrack.model;

import com.bervan.common.model.PersistableTableData;
import com.bervan.common.user.User;
import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class WalletListTableViewDTO implements BaseDTO<UUID>, PersistableTableData<UUID> {
    private UUID id;

    private String name;
    private String description;
    private String currency = "PLN";
    private String riskLevel;

    @Override
    public Class<? extends BaseModel<UUID>> dtoTarget() {
        return Wallet.class;
    }

    @Override
    public String getTableFilterableColumnValue() {
        return name;
    }

    @Override
    public Set<User> getOwners() {
        return Set.of();
    }

    @Override
    public void addOwner(User user) {

    }

    @Override
    public void removeOwner(User user) {

    }

    @Override
    public boolean hasAccess(User user) {
        return false;
    }

    @Override
    public boolean hasAccess(UUID loggedUserId) {
        return false;
    }

    @Override
    public void setDeleted(Boolean value) {

    }
}
