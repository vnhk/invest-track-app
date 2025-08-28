package com.bervan.investtrack.model;

import com.bervan.common.model.PersistableTableData;
import com.bervan.common.model.VaadinBervanColumn;
import com.bervan.common.user.User;
import com.bervan.core.model.BaseDTO;
import com.bervan.core.model.BaseModel;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class WalletListTableViewDTO implements BaseDTO<UUID>, PersistableTableData<UUID> {
    private UUID id;

    @VaadinBervanColumn(displayName = "Name", internalName = "name")
    private String name;

    @VaadinBervanColumn(displayName = "Description", internalName = "description")
    private String description;

    @VaadinBervanColumn(displayName = "Currency", internalName = "currency", strValues = {"PLN", "USD", "EUR"})
    private String currency = "PLN";

    @VaadinBervanColumn(displayName = "Risk Level", internalName = "riskLevel", strValues = {"Low Risk", "Medium Risk", "High Risk", "Very High Risk"})
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
