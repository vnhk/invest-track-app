package com.bervan.investtrack.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.service.WalletService;
import com.bervan.logging.JsonLogger;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.UUID;

@CssImport("./invest-track.css")
public abstract class AbstractWalletsView extends AbstractBervanTableView<UUID, Wallet> {
    public static final String ROUTE_NAME = "/invest-track-app/wallets";
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");

    public AbstractWalletsView(WalletService service, BervanViewConfig bervanViewConfig) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, bervanViewConfig, Wallet.class);

        renderCommonComponents();
    }

    @Override
    protected Grid<Wallet> getGrid() {
        Grid<Wallet> grid = new Grid<>(Wallet.class, false);
        buildGridAutomatically(grid);

        if (grid.getColumnByKey("name") != null) {
            grid.getColumnByKey("name").setRenderer(new ComponentRenderer<>(
                    entity -> new Anchor(ROUTE_NAME + "/" + entity.getName(), entity.getName())
            ));
        }

        return grid;
    }
}
