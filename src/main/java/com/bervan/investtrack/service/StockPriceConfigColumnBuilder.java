package com.bervan.investtrack.service;

import com.bervan.common.component.AutoConfigurableField;
import com.bervan.common.component.builders.ComponentForFieldBuilder;
import com.bervan.common.model.VaadinBervanColumnConfig;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.investtrack.model.VaadinStockPriceAlertConfigColumn;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

@Service
public class StockPriceConfigColumnBuilder implements ComponentForFieldBuilder {

    @Override
    public AutoConfigurableField build(Field field, Object item, Object value, VaadinBervanColumnConfig config) {
        return new StockPriceAlertConfigAutoConfigurableField((StockPriceAlert) item);
    }

    @Override
    public boolean supports(Class<?> extension, VaadinBervanColumnConfig config) {
        return config.getExtension().equals(VaadinStockPriceAlertConfigColumn.class);
    }

    private static class StockPriceAlertConfigAutoConfigurableField extends VerticalLayout implements AutoConfigurableField<StockPriceAlertConfig> {
        StockPriceAlertConfig stockPriceAlertConfig;
        private boolean readOnly;

        public StockPriceAlertConfigAutoConfigurableField(StockPriceAlert item) {
            if (item == null) {
                return;
            }

            stockPriceAlertConfig = item.getStockPriceAlertConfig();
            if (stockPriceAlertConfig != null) {
                String sb = (stockPriceAlertConfig.getOperator() == null ? "" : stockPriceAlertConfig.getOperator()) +
                        (stockPriceAlertConfig.getPrice() == null ? "" : stockPriceAlertConfig.getPrice()) +
                        " (" +
                        (stockPriceAlertConfig.getAmountOfNotifications() == null ? "" : stockPriceAlertConfig.getAmountOfNotifications()) +
                        " left).";
                add(new Div(new Text(sb), new Hr()));
            } else {
                add(new Span("Config not created."));
            }
        }

        @Override
        public StockPriceAlertConfig getValue() {
            return stockPriceAlertConfig;
        }

        @Override
        public void setValue(StockPriceAlertConfig obj) {
            stockPriceAlertConfig = obj;
        }

        @Override
        public void setWidthFull() {

        }

        @Override
        public void setId(String id) {
            super.setId(id);
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }
    }

}
