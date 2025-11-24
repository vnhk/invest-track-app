package com.bervan.investtrack.view.dashboards;

import com.bervan.common.view.AbstractPageView;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
public abstract class AbstractWalletsBaseDashboardView extends AbstractPageView {

    public AbstractWalletsBaseDashboardView() {
        setSizeFull();
    }

    protected Div createCard(String title, Object value, VaadinIcon iconType) {
        Icon icon = iconType.create();
        icon.setSize("30px");

        H3 titleText = new H3(title);
        titleText.getStyle().set("margin", "0");
        Span valueText = new Span(value.toString());
        valueText.getStyle()
                .set("font-size", "1.5em")
                .set("font-weight", "600");

        VerticalLayout textLayout = new VerticalLayout(titleText, valueText);
        textLayout.setSpacing(false);
        textLayout.setPadding(false);
        textLayout.setAlignItems(Alignment.START);

        HorizontalLayout content = new HorizontalLayout(icon, textLayout);
        content.setAlignItems(Alignment.CENTER);
        content.setWidthFull();
        content.getStyle().set("gap", "1em");

        Div card = new Div(content);
        card.setClassName("dashboard-card-value");
        card.getStyle()
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.1)")
                .set("padding", "16px 24px")
                .set("width", "280px")
                .set("text-align", "left");

        return card;
    }

    /**
     * Aggregates data based on the selected time period
     */
    protected <T> Map<String, List<T>> aggregateData(Map<UUID, List<T>> originalData, String period) {
        Map<String, List<T>> result = new HashMap<>();
        int stepSize = getStepSize(period);

        for (Map.Entry<UUID, List<T>> entry : originalData.entrySet()) {
            String walletId = entry.getKey().toString();
            List<T> originalList = entry.getValue();
            List<T> aggregatedList = new ArrayList<>();

            if ("Monthly".equals(period)) {
                // No aggregation needed, return original data because snapshots are created once a month
                aggregatedList.addAll(originalList);
            } else {
                // Take every stepSize-th element, but prefer the last element from each group
                for (int i = stepSize - 1; i < originalList.size(); i += stepSize) {
                    aggregatedList.add(originalList.get(i));
                }

                // If we have remaining elements that don't form a complete group,
                // add the last available value
                int lastProcessedIndex = ((originalList.size() - 1) / stepSize) * stepSize + stepSize - 1;
                if (lastProcessedIndex < originalList.size() - 1) {
                    aggregatedList.add(originalList.get(originalList.size() - 1));
                }
            }

            result.put(walletId, aggregatedList);
        }

        return result;
    }

    /**
     * Returns the step size for aggregation based on selected period
     */
    protected int getStepSize(String period) {
        return switch (period) {
            case "Monthly" -> 1;
            case "Two-Monthly" -> 2;
            case "Quarterly" -> 3;
            case "Half-Yearly" -> 6;
            case "Yearly" -> 12;
            default -> 1;
        };
    }

    protected String formatPercentage(BigDecimal percentage) {
        if (percentage == null) return "0.00%";
        return String.format("%.2f%%", percentage);
    }

    protected Div getGridContainer() {
        Div gridContainer = new Div();
        gridContainer.addClassName("wallet-grid-container");
        gridContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("grid-template-rows", "1fr 1fr")
                .set("gap", "20px")
                .set("height", "80vh")
                .set("width", "90vw")
                .set("padding", "20px");
        return gridContainer;
    }

    /**
     * Creates a single wallet tile with title, separator and chart
     *
     * @param walletName Name of the wallet
     * @return VerticalLayout containing the wallet tile
     */
    protected VerticalLayout createWalletTile(String walletName) {
        VerticalLayout tile = new VerticalLayout();
        tile.addClassName("wallet-tile");
        tile.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("background", "white")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        H3 title = new H3(walletName);
        title.getStyle()
                .set("margin", "0 0 10px 0")
                .set("color", "#333")
                .set("text-align", "center");

        Hr separator = new Hr();
        separator.getStyle()
                .set("margin", "10px 0")
                .set("border", "1px solid #eee");

        tile.add(title, separator);
        tile.setSpacing(false);
        tile.setPadding(false);
        tile.setWidthFull();
        tile.setHeightFull();

        return tile;
    }
}