package com.bervan.investtrack.view;

import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.core.model.BervanLogger;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractPriceAlertsView extends AbstractBervanTableView<UUID, StockPriceAlert> {
    public static final String ROUTE_NAME = "/invest-track-app/price-alerts";

    public AbstractPriceAlertsView(BaseService<UUID, StockPriceAlert> service, BervanLogger logger) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, logger, StockPriceAlert.class);

        renderCommonComponents();
    }

    @Override
    protected void buildGridAutomatically(Grid<StockPriceAlert> grid) {
        super.buildGridAutomatically(grid);
        grid.addComponentColumn(entity -> {
                    StockPriceAlertConfig stockPriceAlertConfig = entity.getStockPriceAlertConfig();
                    if (stockPriceAlertConfig != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(stockPriceAlertConfig.getOperator() == null ? "" : stockPriceAlertConfig.getOperator());
                        sb.append(stockPriceAlertConfig.getPrice() == null ? "" : stockPriceAlertConfig.getPrice());
                        sb.append(" (");
                        sb.append(stockPriceAlertConfig.getAmountOfNotifications() == null ? "" : stockPriceAlertConfig.getAmountOfNotifications());
                        sb.append(" left).");
                        return new Div(new Text(sb.toString()), new Hr());
                    }
                    return new Span();
                }).setKey("stockPriceConfig")
                .setWidth("10px")
                .setResizable(false);
    }
}