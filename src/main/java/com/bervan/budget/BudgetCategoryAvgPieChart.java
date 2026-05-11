package com.bervan.budget;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonObject;

import java.math.BigDecimal;
import java.util.*;

@JsModule("./investing-chart-component.js")
@Tag("canvas")
@Deprecated
public class BudgetCategoryAvgPieChart extends Component implements HasSize {

    public BudgetCategoryAvgPieChart(Map<String, BigDecimal> categoryAverages, List<String> colors) {
        setId("budgetAvgPie_" + UUID.randomUUID());

        List<String> labels = new ArrayList<>(categoryAverages.keySet());
        List<BigDecimal> values = labels.stream().map(categoryAverages::get).toList();

        JreJsonObject labelsJson = toJsonObject(labels);
        JreJsonObject valuesJson = toJsonObject(values);
        JreJsonObject colorsJson = toJsonObject(colors);

        UI.getCurrent().getPage().executeJs(
                "window.renderBudgetCategoryPieChart($0, $1, $2, $3)",
                getElement(),
                labelsJson.get("data"),
                valuesJson.get("data"),
                colorsJson.get("data")
        );
    }

    private static JreJsonObject toJsonObject(List<?> items) {
        JreJsonObject obj = new JreJsonObject(new JreJsonFactory());
        JreJsonArray arr = new JreJsonArray(new JreJsonFactory());
        for (int i = 0; i < items.size(); i++) arr.set(i, items.get(i).toString());
        obj.set("data", arr);
        return obj;
    }
}
