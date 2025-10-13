package com.bervan.investtrack.view;

import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableDTOView;
import com.bervan.core.model.BervanLogger;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletListTableViewDTO;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.UUID;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletsView extends AbstractBervanTableDTOView<UUID, Wallet, WalletListTableViewDTO> {
    public static final String ROUTE_NAME = "/invest-track-app/wallets";

    public AbstractWalletsView(BaseService<UUID, Wallet> service, BervanLogger logger) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, logger, Wallet.class, WalletListTableViewDTO.class);

        renderCommonComponents();
    }

    @Override
    protected void preColumnAutoCreation(Grid grid) {
        grid.addComponentColumn(entity -> {
                    Icon linkIcon = new Icon(VaadinIcon.LINK);
                    linkIcon.getStyle().set("cursor", "pointer");
                    return new Anchor(ROUTE_NAME + "/" + ((WalletListTableViewDTO) entity).getName(), new HorizontalLayout(linkIcon));
                }).setKey("link")
                .setWidth("6px")
                .setResizable(false);
    }
}