package com.bervan.investtrack.view;

import com.bervan.common.component.AutoConfigurableField;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.SortDirection;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class WalletSnapshotListView extends AbstractBervanTableView<UUID, WalletSnapshot> {
    private final Wallet wallet;
    private boolean firstLoad = true;

    public WalletSnapshotListView(BaseService<UUID, WalletSnapshot> service, Wallet wallet, BervanViewConfig bervanViewConfig) {
        super(null, service, bervanViewConfig, WalletSnapshot.class);
        this.wallet = wallet;
        pageSize = 10000;
    }

    @Override
    protected void customFieldInCreateItemLayout(Map<Field, AutoConfigurableField> fieldsHolder, Map<Field, VerticalLayout> fieldsLayoutHolder, VerticalLayout formLayout) {
        for (Map.Entry<Field, AutoConfigurableField> fieldAutoConfigurableFieldEntry : fieldsHolder.entrySet()) {
            switch (fieldAutoConfigurableFieldEntry.getKey().getName()) {
                case "snapshotDate":
                    fieldAutoConfigurableFieldEntry.getValue().setValue(LocalDate.now());
                    break;
                case "portfolioValue":
                    fieldAutoConfigurableFieldEntry.getValue().setValue(wallet.getCurrentValue());
                    break;
                case "monthlyDeposit":
                    fieldAutoConfigurableFieldEntry.getValue().setValue(BigDecimal.ZERO);
                    break;
                case "monthlyWithdrawal":
                    fieldAutoConfigurableFieldEntry.getValue().setValue(BigDecimal.ZERO);
                    break;
                case "monthlyEarnings":
                    fieldAutoConfigurableFieldEntry.getValue().setValue(BigDecimal.ZERO);
                    break;
            }
        }
    }

    @Override
    protected void customizePreLoad(SearchRequest request) {
        request.addCriterion("WALLET_OWNER_CRITERIA", WalletSnapshot.class,
                "wallet.id", SearchOperation.EQUALS_OPERATION, wallet.getId());

        if (firstLoad) {
            sortField = "snapshotDate";
            sortDirection = SortDirection.DESCENDING;
            sortDir = com.bervan.common.search.model.SortDirection.DESC;
        }

        firstLoad = false;
    }

    @Override
    protected WalletSnapshot preSaveActions(WalletSnapshot newItem) {
        newItem.setWallet(wallet);
        return newItem;
    }
}
