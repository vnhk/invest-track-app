package com.bervan.investtrack.view.dashboards;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Heatmap grid showing monthly returns with color-coded cells
 * Green for positive returns, red for negative
 */
@CssImport("./invest-track-dashboard.css")
public class MonthlyReturnsHeatmap extends Div {

    private static final String[] MONTH_NAMES = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    /**
     * @param monthlyReturns map of "YYYY-MM" to return percentage
     */
    public MonthlyReturnsHeatmap(Map<String, BigDecimal> monthlyReturns) {
        addClassName("invest-heatmap");
        buildHeatmap(monthlyReturns);
    }

    private void buildHeatmap(Map<String, BigDecimal> monthlyReturns) {
        if (monthlyReturns == null || monthlyReturns.isEmpty()) {
            add(new Span("No data available"));
            return;
        }

        // Group by year
        Map<Integer, Map<Integer, BigDecimal>> byYear = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, BigDecimal> entry : monthlyReturns.entrySet()) {
            String[] parts = entry.getKey().split("-");
            if (parts.length == 2) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                byYear.computeIfAbsent(year, k -> new HashMap<>()).put(month, entry.getValue());
            }
        }

        // Set grid columns: year label + 12 months
        getStyle().set("grid-template-columns", "60px repeat(12, 1fr)");

        // Header row
        Div headerRow = new Div();
        headerRow.addClassName("invest-heatmap-header");

        Span yearHeader = new Span("");
        yearHeader.addClassName("invest-heatmap-year");
        add(yearHeader);

        for (String monthName : MONTH_NAMES) {
            Span monthHeader = new Span(monthName);
            add(monthHeader);
        }

        // Data rows
        for (Map.Entry<Integer, Map<Integer, BigDecimal>> yearEntry : byYear.entrySet()) {
            int year = yearEntry.getKey();
            Map<Integer, BigDecimal> monthData = yearEntry.getValue();

            // Year label
            Span yearLabel = new Span(String.valueOf(year));
            yearLabel.addClassName("invest-heatmap-year");
            add(yearLabel);

            // Month cells
            for (int month = 1; month <= 12; month++) {
                BigDecimal returnPct = monthData.get(month);
                Div cell = createCell(returnPct);
                add(cell);
            }
        }
    }

    private Div createCell(BigDecimal returnPct) {
        Div cell = new Div();
        cell.addClassName("invest-heatmap-cell");

        if (returnPct == null) {
            cell.addClassName("empty");
            cell.setText("-");
        } else {
            String formatted = returnPct.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
            cell.setText(formatted);

            int level = calculateLevel(returnPct);
            cell.addClassName("level-" + level);

            cell.getElement().setAttribute("title",
                    "Return: " + returnPct.setScale(2, RoundingMode.HALF_UP) + "%");
        }

        return cell;
    }

    /**
     * Calculate color level based on return percentage
     * Levels from -5 (worst) to +5 (best)
     */
    private int calculateLevel(BigDecimal returnPct) {
        double value = returnPct.doubleValue();

        if (value >= 5) return 5;
        if (value >= 3) return 4;
        if (value >= 2) return 3;
        if (value >= 1) return 2;
        if (value >= 0.5) return 1;
        if (value >= -0.5) return 0;
        if (value >= -1) return -1;
        if (value >= -2) return -2;
        if (value >= -3) return -3;
        if (value >= -5) return -4;
        return -5;
    }
}
