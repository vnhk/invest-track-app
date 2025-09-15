package com.bervan.investtrack.service;

import com.bervan.common.component.AutoConfigurableField;
import com.bervan.common.component.CommonComponentUtils;
import com.bervan.common.component.builders.ComponentForFieldBuilder;
import com.bervan.common.model.VaadinBervanColumnConfig;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.investtrack.model.VaadinStockPriceAlertConfigColumn;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class StockPriceConfigFieldBuilder implements ComponentForFieldBuilder {
    private static final StockPriceConfigFieldBuilder INSTANCE = new StockPriceConfigFieldBuilder();

    private StockPriceConfigFieldBuilder() {

    }

    public static StockPriceConfigFieldBuilder getInstance() {
        return INSTANCE;
    }


    @Override
    public AutoConfigurableField build(Field field, Object item, Object value, VaadinBervanColumnConfig config) {
        if (item == null) {
            return new StockPriceAlertConfigAutoConfigurableField(null);
        }
        return new StockPriceAlertConfigAutoConfigurableField((StockPriceAlert) item);
    }

    @Override
    public boolean supports(Class<?> extension, VaadinBervanColumnConfig config) {
        return config.getExtension().equals(VaadinStockPriceAlertConfigColumn.class);
    }

    private class StockPriceAlertConfigAutoConfigurableField extends VerticalLayout implements AutoConfigurableField<StockPriceAlertConfig> {
        private StockPriceAlertConfig stockPriceAlertConfig;
        private final Map<Field, AutoConfigurableField> fieldsHolder = new HashMap<>();
        private final Map<Field, VerticalLayout> fieldsLayoutHolder = new HashMap<>();
        private boolean readOnly;

        public StockPriceAlertConfigAutoConfigurableField(StockPriceAlert stockPriceAlert) {
            if (stockPriceAlert != null) {
                this.stockPriceAlertConfig = stockPriceAlert.getStockPriceAlertConfig();
            }

            if (this.stockPriceAlertConfig == null) {
                this.stockPriceAlertConfig = new StockPriceAlertConfig();
                this.stockPriceAlertConfig.setId(UUID.randomUUID()); //why I need to set this manually?
            }

            try {

                VerticalLayout verticalLayout = CommonComponentUtils.buildFormLayout(StockPriceAlertConfig.class, stockPriceAlertConfig, fieldsHolder, fieldsLayoutHolder);
                add(verticalLayout);
            } catch (Exception e) {
                log.error("Could not build form layout for StockPriceAlertConfig", e);
                throw new RuntimeException("Could not build form layout for Stock Price Alert Config");
            }
        }

        @Override
        public StockPriceAlertConfig getValue() {
            //either use fieldsHolder or use the code from AbstractBervanEntityView that will build everything...

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
