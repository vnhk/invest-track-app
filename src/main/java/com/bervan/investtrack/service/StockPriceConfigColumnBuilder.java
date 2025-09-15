package com.bervan.investtrack.service;

import com.bervan.common.component.AutoConfigurableField;
import com.bervan.common.component.builders.ComponentForFieldBuilder;
import com.bervan.common.model.VaadinBervanColumnConfig;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.investtrack.model.VaadinStockPriceAlertConfigColumn;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.lang.reflect.Field;

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
