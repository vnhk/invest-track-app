package com.bervan.investtrack.view;

import com.bervan.common.component.table.builders.ColumnForGridBuilder;
import com.bervan.common.model.PersistableTableData;
import com.bervan.common.model.VaadinBervanColumnConfig;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.investtrack.model.VaadinStockPriceAlertConfigColumn;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;

@Slf4j
public class PriceAlertConfigColumnBuilder implements ColumnForGridBuilder {

    private static final PriceAlertConfigColumnBuilder INSTANCE = new PriceAlertConfigColumnBuilder();

    private PriceAlertConfigColumnBuilder() {

    }

    public static PriceAlertConfigColumnBuilder getInstance() {
        return INSTANCE;
    }

    @Override
    public <ID extends Serializable, T extends PersistableTableData<ID>> Renderer<T> build(Field field, VaadinBervanColumnConfig config) {
        return createConfigComponent(field, config);
    }

    @Override
    public <ID extends Serializable, T extends PersistableTableData<ID>> boolean supports(Class<?> extension, VaadinBervanColumnConfig config, Class<T> tClass) {
        return extension == VaadinStockPriceAlertConfigColumn.class;
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public boolean isSortable() {
        return false;
    }

    private <ID extends Serializable, T extends PersistableTableData<ID>> ComponentRenderer<Span, T> createConfigComponent(Field f, VaadinBervanColumnConfig config) {
        return new ComponentRenderer<>(Span::new, configColumnUpdater(f, config));
    }

    private <ID extends Serializable, T extends PersistableTableData<ID>> SerializableBiConsumer<Span, T> configColumnUpdater(Field f, VaadinBervanColumnConfig config) {
        return (span, record) -> {
            try {
                span.setClassName("modern-cell-content");
                f.setAccessible(true);
                Object o = f.get(record);
                f.setAccessible(false);
                if (o != null) {
                    String sb = getStockPriceAlertConfigText((StockPriceAlertConfig) o);
                    span.add(new Div(new Text(sb), new Hr()));
                } else {
                    span.add(new Span("Config not created."));
                }
            } catch (Exception e) {
                log.error("Could not create column in table!", e);
                throw new RuntimeException("Could not create column in table!");
            }
        };
    }

    private String getStockPriceAlertConfigText(StockPriceAlertConfig stockPriceAlertConfig) {
        return (stockPriceAlertConfig.getOperator() == null ? "" : stockPriceAlertConfig.getOperator()) +
                (stockPriceAlertConfig.getPrice() == null ? "" : stockPriceAlertConfig.getPrice()) +
                " (" +
                (stockPriceAlertConfig.getAmountOfNotifications() == null ? "" : stockPriceAlertConfig.getAmountOfNotifications()) +
                " left).";
    }

}
