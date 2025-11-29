package com.bervan.investtrack.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.service.WalletService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractWalletsView extends AbstractBervanTableView<UUID, Wallet> {
    public static final String ROUTE_NAME = "/invest-track-app/wallets";

    public AbstractWalletsView(WalletService service, BervanViewConfig bervanViewConfig) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, bervanViewConfig, Wallet.class);

        renderCommonComponents();
    }

    @Override
    protected void preColumnAutoCreation(Grid<Wallet> grid) {
        grid.addComponentColumn(entity -> {
                    Icon linkIcon = new Icon(VaadinIcon.LINK);
                    linkIcon.getStyle().set("cursor", "pointer");
                    return new Anchor(ROUTE_NAME + "/" + entity.getName(), new HorizontalLayout(linkIcon));
                }).setKey("link")
                .setWidth("6px")
                .setResizable(false);
    }
}