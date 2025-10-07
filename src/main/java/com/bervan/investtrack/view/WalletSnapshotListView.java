package com.bervan.investtrack.view;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.core.model.BervanLogger;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.vaadin.flow.data.provider.SortDirection;

import java.util.UUID;

public class WalletSnapshotListView extends AbstractBervanTableView<UUID, WalletSnapshot> {
    private final Wallet wallet;
    private boolean firstLoad = true;

    public WalletSnapshotListView(BaseService<UUID, WalletSnapshot> service, BervanLogger bervanLogger, Wallet wallet) {
        super(null, service, bervanLogger, WalletSnapshot.class);
        this.wallet = wallet;
        pageSize = 10000;
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
    protected WalletSnapshot customizeSavingInCreateForm(WalletSnapshot newItem) {
        newItem.setWallet(wallet);
        return newItem;
    }
}
