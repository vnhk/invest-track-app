package com.bervan.investtrack.view.charts;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonNumber;
import elemental.json.impl.JreJsonObject;

import java.util.List;
import java.util.UUID;

@JsModule("./investing-chart-component.js")
@Tag("canvas")
public class FireProjectionChart extends Component implements HasSize {

    public FireProjectionChart(
            List<Integer> years,
            List<Double> baseline,
            List<Double> plus20,
            List<Double> minus20,
            double currentBalance
    ) {
        setId("fireProjection_" + UUID.randomUUID());
        renderChart(years, baseline, plus20, minus20, currentBalance);
    }

    private static JreJsonArray toArray(List<?> list) {
        JreJsonArray arr = new JreJsonArray(new JreJsonFactory());
        for (int i = 0; i < list.size(); i++) {
            arr.set(i, list.get(i).toString());
        }
        return arr;
    }

    private void renderChart(List<Integer> years,
                             List<Double> baseline,
                             List<Double> plus20,
                             List<Double> minus20,
                             double currentBalance) {

        UI.getCurrent().getPage().executeJs(
                "window.renderFireProjectionChart($0, $1, $2, $3, $4, $5)",
                getElement(),
                toArray(years),
                toArray(baseline),
                toArray(plus20),
                toArray(minus20),
                createCurrentPoint(currentBalance)
        );
    }

    private JreJsonObject createCurrentPoint(double currentBalance) {
        JreJsonObject point = new JreJsonObject(new JreJsonFactory());
        point.set("x", new JreJsonNumber(0));
        point.set("y", new JreJsonNumber(currentBalance));
        return point;
    }
}