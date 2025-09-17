package com.bervan.investtrack.view;

import com.bervan.common.component.CommonComponentUtils;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.core.model.BervanLogger;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.service.StockPriceAlertService;
import com.bervan.investtrack.service.StockPriceConfigFieldBuilder;
import com.vaadin.flow.component.dependency.CssImport;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@CssImport("./invest-track.css")
public abstract class AbstractStockPriceAlertsView extends AbstractBervanTableView<UUID, StockPriceAlert> {
    public static final String ROUTE_NAME = "/invest-track-app/price-alerts";

    public AbstractStockPriceAlertsView(BaseService<UUID, StockPriceAlert> service, BervanLogger logger) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, logger, StockPriceAlert.class);
        AbstractBervanTableView.addColumnForGridBuilder(PriceAlertConfigColumnBuilder.getInstance());
        CommonComponentUtils.addComponentBuilder(StockPriceConfigFieldBuilder.getInstance());
        renderCommonComponents();
    }


    @Override
    protected List<String> getFieldsToFetchForTable() {
        List<String> fieldsToFetchForTable = super.getFieldsToFetchForTable();
        fieldsToFetchForTable.remove("stockPriceAlertConfig"); //search is not working with one to many or one-to-one relations
        return fieldsToFetchForTable;
    }

    @Override
    protected List<StockPriceAlert> loadData() {
        //search is not working with one to many or one-to-one relations
        List<StockPriceAlert> stockPriceAlerts = super.loadData();
        for (StockPriceAlert stockPriceAlert : stockPriceAlerts) {
            stockPriceAlert.setEmails(((StockPriceAlertService) service).loadEmails(stockPriceAlert));
            stockPriceAlert.setStockPriceAlertConfig(((StockPriceAlertService) service).loadStockPriceAlertConfig(stockPriceAlert));
        }
        return stockPriceAlerts;
    }
}