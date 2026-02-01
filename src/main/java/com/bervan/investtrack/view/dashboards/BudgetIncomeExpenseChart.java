package com.bervan.investtrack.view.dashboards;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stacked bar chart showing monthly income vs expense
 */
@JsModule("./investing-chart-component.js")
@Tag("canvas")
public class BudgetIncomeExpenseChart extends Component implements HasSize {

    public BudgetIncomeExpenseChart(Map<String, BigDecimal> income, Map<String, BigDecimal> expense) {
        renderChart(income, expense);
    }

    private void renderChart(Map<String, BigDecimal> income, Map<String, BigDecimal> expense) {
        setId("budgetChart_" + UUID.randomUUID());

        List<String> months = income.keySet().stream().sorted().toList();
        List<BigDecimal> incomeValues = months.stream()
                .map(m -> income.getOrDefault(m, BigDecimal.ZERO))
                .toList();
        List<BigDecimal> expenseValues = months.stream()
                .map(m -> expense.getOrDefault(m, BigDecimal.ZERO))
                .toList();

        JreJsonObject monthsJson = getJreJsonObject(months);
        JreJsonObject incomeJson = getJreJsonObject(incomeValues);
        JreJsonObject expenseJson = getJreJsonObject(expenseValues);

        UI.getCurrent().getPage().executeJs(
                "window.renderBudgetIncomeExpenseChart($0, $1, $2, $3)",
                getElement(),
                monthsJson.get("data"),
                incomeJson.get("data"),
                expenseJson.get("data")
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
