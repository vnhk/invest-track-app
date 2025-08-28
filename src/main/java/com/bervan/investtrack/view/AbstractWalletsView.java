package com.bervan.investtrack.view;

import com.bervan.common.AbstractBervanTableDTOView;
import com.bervan.common.service.BaseService;
import com.bervan.core.model.BervanLogger;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletListTableViewDTO;
import com.vaadin.flow.component.dependency.CssImport;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletsView extends AbstractBervanTableDTOView<UUID, Wallet, WalletListTableViewDTO> {
    public static final String ROUTE_NAME = "/invest-track-app/wallets/";

    public AbstractWalletsView(BaseService<UUID, Wallet> service, BervanLogger logger) {
        super(new InvestTrackPageLayout(ROUTE_NAME), service, logger, Wallet.class, WalletListTableViewDTO.class);

        renderCommonComponents();
    }

    @Override
    protected void postSearchUpdate(List<Wallet> collect) {
        collect.add(new Wallet("Wallet 1", "desc", "PLN"));
    }
}