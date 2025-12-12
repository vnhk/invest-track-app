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
public class StrategyBGRHistoryChart extends Component implements HasSize {

    //best//good//risky
    public StrategyBGRHistoryChart(
            List<String> dates, List<String> bestRecPercent, List<String> goodRecPercent, List<String> riskyRecPercent) {
        setId("StrategyBGRHistoryChart_" + UUID.randomUUID());
        renderChart(dates, bestRecPercent, goodRecPercent, riskyRecPercent);
    }

    private static JreJsonArray toArray(List<?> list) {
        JreJsonArray arr = new JreJsonArray(new JreJsonFactory());
        for (int i = 0; i < list.size(); i++) {
            arr.set(i, list.get(i).toString());
        }
        return arr;
    }

    private void renderChart(List<String> dates, List<String> bestRecPercent, List<String> goodRecPercent, List<String> riskyRecPercent) {

        UI.getCurrent().getPage().executeJs(
                "window.renderStrategyBGRHistoryChart($0, $1, $2, $3, $4, $5, $6, $7)",
                getElement(),
                toArray(dates),
                toArray(bestRecPercent),
                toArray(goodRecPercent),
                toArray(riskyRecPercent)
        );
    }
}