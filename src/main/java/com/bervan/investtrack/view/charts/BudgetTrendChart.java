package com.bervan.investtrack.view.charts;

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

/**
 * Line chart showing expense trends per category over time
 */
@JsModule("./investing-chart-component.js")
@Tag("canvas")
public class BudgetTrendChart extends Component implements HasSize {

    /**
     * @param categoryTrends map of category -> (month -> amount)
     */
    public BudgetTrendChart(Map<String, Map<String, BigDecimal>> categoryTrends) {
        renderChart(categoryTrends);
    }

    private void renderChart(Map<String, Map<String, BigDecimal>> categoryTrends) {
        setId("categoryTrendChart_" + UUID.randomUUID());

        if (categoryTrends == null || categoryTrends.isEmpty()) {
            return;
        }

        // Get all months (sorted)
        Set<String> allMonths = new TreeSet<>();
        for (Map<String, BigDecimal> monthData : categoryTrends.values()) {
            allMonths.addAll(monthData.keySet());
        }
        List<String> months = new ArrayList<>(allMonths);

        // Build category data object for JS
        JreJsonObject categoriesDataJson = new JreJsonObject(new JreJsonFactory());
        for (Map.Entry<String, Map<String, BigDecimal>> entry : categoryTrends.entrySet()) {
            String category = entry.getKey();
            Map<String, BigDecimal> monthData = entry.getValue();

            JreJsonArray valuesArray = new JreJsonArray(new JreJsonFactory());
            int i = 0;
            for (String month : months) {
                BigDecimal value = monthData.getOrDefault(month, BigDecimal.ZERO);
                valuesArray.set(i++, value.doubleValue());
            }
            categoriesDataJson.set(category, valuesArray);
        }

        JreJsonObject monthsJson = getJreJsonObject(months);

        UI.getCurrent().getPage().executeJs(
                "window.renderCategoryTrendsChart($0, $1, $2)",
                getElement(),
                monthsJson.get("data"),
                categoriesDataJson
        );
    }

    private static JreJsonObject getJreJsonObject(List<?> values) {
        JreJsonObject jreJsonObject = new JreJsonObject(new JreJsonFactory());
        JreJsonArray jreJsonArray = new JreJsonArray(new JreJsonFactory());

        for (int i = 0; i < values.size(); i++) {
            jreJsonArray.set(i, values.get(i).toString());
        }
        jreJsonObject.set("data", jreJsonArray);
        return jreJsonObject;
    }
}
