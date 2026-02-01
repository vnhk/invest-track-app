package com.bervan.investtrack.view.charts;

import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.service.CurrencyConverter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Doughnut chart showing asset allocation by currency or risk level
 */
@JsModule("./investing-chart-component.js")
@Tag("canvas")
public class AssetAllocationChart extends Component implements HasSize {

    public enum GroupBy {
        CURRENCY, RISK_LEVEL, WALLET
    }

    public AssetAllocationChart(List<Wallet> wallets, CurrencyConverter currencyConverter, GroupBy groupBy) {
        renderChart(wallets, currencyConverter, groupBy);
    }

    private void renderChart(List<Wallet> wallets, CurrencyConverter currencyConverter, GroupBy groupBy) {
        setId("allocationChart_" + UUID.randomUUID());

        Map<String, BigDecimal> allocation = calculateAllocation(wallets, currencyConverter, groupBy);

        List<String> labels = new ArrayList<>(allocation.keySet());
        List<BigDecimal> values = labels.stream()
                .map(allocation::get)
                .toList();

        JreJsonObject labelsJson = getJreJsonObject(labels);
        JreJsonObject valuesJson = getJreJsonObject(values);
        JreJsonObject colorsJson = getJreJsonObject(getColors(labels.size()));

        UI.getCurrent().getPage().executeJs(
                "window.renderAssetAllocationChart($0, $1, $2, $3)",
                getElement(),
                labelsJson.get("data"),
                valuesJson.get("data"),
                colorsJson.get("data")
        );
    }

    private Map<String, BigDecimal> calculateAllocation(List<Wallet> wallets,
                                                         CurrencyConverter currencyConverter,
                                                         GroupBy groupBy) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (Wallet wallet : wallets) {
            BigDecimal valuePLN = currencyConverter.convert(
                    wallet.getCurrentValue(),
                    CurrencyConverter.Currency.of(wallet.getCurrency()),
                    CurrencyConverter.Currency.PLN
            );

            String key = switch (groupBy) {
                case CURRENCY -> wallet.getCurrency() != null ? wallet.getCurrency() : "Unknown";
                case RISK_LEVEL -> wallet.getRiskLevel() != null ? wallet.getRiskLevel() : "Unspecified";
                case WALLET -> wallet.getName() != null ? wallet.getName() : "Unnamed";
            };

            result.merge(key, valuePLN, BigDecimal::add);
        }

        // Sort by value descending
        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(result.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Map<String, BigDecimal> sortedResult = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : sorted) {
            sortedResult.put(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP));
        }

        return sortedResult;
    }

    private List<String> getColors(int count) {
        List<String> colors = Arrays.asList(
                "rgba(99, 102, 241, 0.8)",   // Indigo
                "rgba(34, 211, 238, 0.8)",   // Cyan
                "rgba(16, 185, 129, 0.8)",   // Emerald
                "rgba(245, 158, 11, 0.8)",   // Amber
                "rgba(239, 68, 68, 0.8)",    // Red
                "rgba(139, 92, 246, 0.8)",   // Violet
                "rgba(236, 72, 153, 0.8)",   // Pink
                "rgba(59, 130, 246, 0.8)",   // Blue
                "rgba(168, 162, 158, 0.8)",  // Stone
                "rgba(251, 191, 36, 0.8)"    // Yellow
        );

        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(colors.get(i % colors.size()));
        }
        return result;
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
