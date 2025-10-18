package com.bervan.investtrack.service;

import com.bervan.common.component.AutoConfigurableField;
import com.bervan.common.component.CommonComponentHelper;
import com.bervan.common.component.CommonComponentUtils;
import com.bervan.common.component.builders.ComponentForFieldBuilder;
import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.config.ClassViewAutoConfigColumn;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.investtrack.model.VaadinStockPriceAlertConfigColumn;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class StockPriceConfigFieldBuilder implements ComponentForFieldBuilder {
    private static StockPriceConfigFieldBuilder INSTANCE;
    private final BervanViewConfig bervanViewConfig;
    private CommonComponentHelper<UUID, StockPriceAlertConfig> componentHelper = new CommonComponentHelper<>(StockPriceAlertConfig.class);

    private StockPriceConfigFieldBuilder(BervanViewConfig bervanViewConfig) {
        this.bervanViewConfig = bervanViewConfig;
    }

    public synchronized static StockPriceConfigFieldBuilder getInstance(BervanViewConfig bervanViewConfig) {
        if (INSTANCE == null) {
            INSTANCE = new StockPriceConfigFieldBuilder(bervanViewConfig);
        }
        return INSTANCE;
    }


    @Override
    public AutoConfigurableField build(Field field, Object item, Object value, ClassViewAutoConfigColumn config) {
        if (item == null) {
            return new StockPriceAlertConfigAutoConfigurableField(null, bervanViewConfig);
        }
        return new StockPriceAlertConfigAutoConfigurableField((StockPriceAlert) item, bervanViewConfig);
    }

    @Override
    public boolean supports(String typeName, ClassViewAutoConfigColumn config) {
        return VaadinStockPriceAlertConfigColumn.class.getSimpleName().equals(config.getExtension());
    }

    private class StockPriceAlertConfigAutoConfigurableField extends VerticalLayout implements AutoConfigurableField<StockPriceAlertConfig> {
        private final Map<Field, AutoConfigurableField> fieldsHolder = new HashMap<>();
        private final Map<Field, VerticalLayout> fieldsLayoutHolder = new HashMap<>();
        private StockPriceAlertConfig stockPriceAlertConfig;
        private boolean readOnly;

        public StockPriceAlertConfigAutoConfigurableField(StockPriceAlert stockPriceAlert, BervanViewConfig bervanViewConfig) {
            if (stockPriceAlert != null) {
                this.stockPriceAlertConfig = stockPriceAlert.getStockPriceAlertConfig();
            }

            if (this.stockPriceAlertConfig == null) {
                this.stockPriceAlertConfig = new StockPriceAlertConfig();
                this.stockPriceAlertConfig.setId(UUID.randomUUID()); //why I need to set this manually?
            }

            try {
                VerticalLayout verticalLayout = CommonComponentUtils.buildFormLayout(StockPriceAlertConfig.class, stockPriceAlertConfig, fieldsHolder, fieldsLayoutHolder, bervanViewConfig);
                add(new H3("Alert price config"), verticalLayout);
            } catch (Exception e) {
                log.error("Could not build form layout for StockPriceAlertConfig", e);
                throw new RuntimeException("Could not build form layout for Stock Price Alert Config");
            }
        }

        @Override
        public StockPriceAlertConfig getValue() {
            for (Map.Entry<Field, AutoConfigurableField> fieldAutoConfigurableFieldEntry : fieldsHolder.entrySet()) {
                try {
                    fieldAutoConfigurableFieldEntry.getKey().setAccessible(true);
                    fieldAutoConfigurableFieldEntry.getKey().set(stockPriceAlertConfig, componentHelper.getFieldValueForNewItemDialog(fieldAutoConfigurableFieldEntry));
                    fieldAutoConfigurableFieldEntry.getKey().setAccessible(false);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not set field value for Stock Price Alert Config");
                }
            }

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
        public void validate() {
            //??????
        }

        @Override
        public boolean isInvalid() {
            //??????
            return false;
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }
    }

}
