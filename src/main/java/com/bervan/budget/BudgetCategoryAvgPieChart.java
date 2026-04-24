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
public class BudgetCategoryAvgPieChart extends Component implements HasSize {

    public BudgetCategoryAvgPieChart(Map<String, BigDecimal> categoryAverages) {
        setId("budgetAvgPie_" + UUID.randomUUID());

        List<String> labels = new ArrayList<>(categoryAverages.keySet());
        List<BigDecimal> values = labels.stream().map(categoryAverages::get).toList();
        List<String> colors = getColors(labels.size());

        JreJsonObject labelsJson = toJsonObject(labels);
        JreJsonObject valuesJson = toJsonObject(values);
        JreJsonObject colorsJson = toJsonObject(colors);

        UI.getCurrent().getPage().executeJs(
                "window.renderAssetAllocationChart($0, $1, $2, $3)",
                getElement(),
                labelsJson.get("data"),
                valuesJson.get("data"),
                colorsJson.get("data")
        );
    }

    private List<String> getColors(int count) {
        List<String> palette = Arrays.asList(
                "rgba(239, 68,  68,  0.85)",
                "rgba(245, 158, 11,  0.85)",
                "rgba(99,  102, 241, 0.85)",
                "rgba(34,  211, 238, 0.85)",
                "rgba(16,  185, 129, 0.85)",
                "rgba(139, 92,  246, 0.85)",
                "rgba(236, 72,  153, 0.85)",
                "rgba(59,  130, 246, 0.85)",
                "rgba(168, 162, 158, 0.85)",
                "rgba(251, 191, 36,  0.85)"
        );
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(palette.get(i % palette.size()));
        return result;
    }

    private static JreJsonObject toJsonObject(List<?> items) {
        JreJsonObject obj = new JreJsonObject(new JreJsonFactory());
        JreJsonArray arr = new JreJsonArray(new JreJsonFactory());
        for (int i = 0; i < items.size(); i++) arr.set(i, items.get(i).toString());
        obj.set("data", arr);
        return obj;
    }
}
