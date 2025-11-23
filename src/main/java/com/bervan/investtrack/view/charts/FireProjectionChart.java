package com.bervan.investtrack.view.charts;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;

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
            List<Double> onlyDeposits
    ) {
        setId("fireProjection_" + UUID.randomUUID());
        renderChart(years, baseline, plus20, minus20, onlyDeposits);
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
                             List<Double> onlyDeposits) {

        UI.getCurrent().getPage().executeJs(
                "window.renderFireProjectionChart($0, $1, $2, $3, $4, $5)",
                getElement(),
                toArray(years),
                toArray(baseline),
                toArray(plus20),
                toArray(minus20),
                toArray(onlyDeposits)
        );
    }
}