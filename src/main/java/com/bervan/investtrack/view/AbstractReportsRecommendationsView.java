package com.bervan.investtrack.view;

import com.bervan.common.component.BervanButton;
import com.bervan.common.component.BervanDatePicker;
import com.bervan.common.view.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.ReportData;
import com.bervan.investtrack.service.StockPriceReportService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractReportsRecommendationsView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/recommendations";

    private final StockPriceReportService stockPriceReportService;
    private final VerticalLayout content = new VerticalLayout();
    private final VerticalLayout tabsContent = new VerticalLayout();

    protected AbstractReportsRecommendationsView(StockPriceReportService stockPriceReportService) {
        add(new InvestTrackPageLayout(ROUTE_NAME, null));
        this.stockPriceReportService = stockPriceReportService;
        BervanButton triggerMorning = new BervanButton("Trigger Morning", buttonClickEvent -> {
            stockPriceReportService.loadStockPricesMorning();
        });
        BervanButton triggerEvening = new BervanButton("Trigger Evening", buttonClickEvent -> {
            stockPriceReportService.loadStockPricesBeforeClose();
        });
        BervanDatePicker datePicker = new BervanDatePicker();
        datePicker.setValue(LocalDate.now());
        datePicker.addValueChangeListener(e -> {
            buildView(stockPriceReportService, e.getValue());
        });
        add(new HorizontalLayout(triggerMorning, triggerEvening, datePicker));
        add(tabsContent);
        add(content);

        buildView(stockPriceReportService, LocalDate.now());
    }

    private void buildView(StockPriceReportService stockPriceReportService, LocalDate value) {
        tabsContent.removeAll();
        ReportData reportData = stockPriceReportService.loadReportData(value);
        Tabs tabs = getTabs(reportData);
        if (reportData.getGoodInvestmentTotalProbabilityBasedOnToday() != null) {
            tabsContent.add(new H3("Total probability of making good investment today: "
                    + reportData.getGoodInvestmentTotalProbabilityBasedOnToday().multiply(BigDecimal.valueOf(100)) + "%"));
        }
        tabsContent.add(tabs);
        showBestToInvest(reportData);
    }

    private TextField getSearch(ListDataProvider<StockPriceData> dataProvider) {
        TextField search = new TextField();
        search.setPlaceholder("Search...");
        search.setClearButtonVisible(true);

        // Filter logic
        search.addValueChangeListener(e -> {
            String filterText = e.getValue().trim().toLowerCase();
            dataProvider.clearFilters();

            if (!filterText.isEmpty()) {
                dataProvider.addFilter(item ->
                        item.getSymbol().toLowerCase().contains(filterText)
                );
            }
        });
        return search;
    }

    private Tabs getTabs(ReportData reportData) {
        Tabs tabs = new Tabs();
        Tab bestToInvestToday = new Tab("Best to invest today");
        Tab goodToInvestToday = new Tab("Good to invest today");
        Tab riskyToInvestToday = new Tab("Risky to invest today");
        tabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab == bestToInvestToday) {
                showBestToInvest(reportData);
            } else if (selectedTab == goodToInvestToday) {
                showGoodToInvest(reportData);
            } else if (selectedTab == riskyToInvestToday) {
                showRiskyToInvest(reportData);
            }
        });

        tabs.add(bestToInvestToday);
        tabs.add(goodToInvestToday);
        tabs.add(riskyToInvestToday);
        return tabs;
    }

    private void showGoodToInvest(ReportData reportData) {
        content.removeAll();

        if (reportData.getGoodToInvest() == null) {
            showWarningNotification("No stocks found for today. Please try again later.");
            return;
        }


        VerticalLayout goodGrid = getGrid(reportData.getGoodToInvest());
        content.add(new H3("Good to invest today:"));
        content.add(goodGrid);

        Map<String, BigDecimal> morningMap = toMorningMap(reportData.getGoodToInvest());

        if (!reportData.getGoodInvestmentsBasedOnGoodRecommendation().isEmpty()) {

            content.add(new H3("Good investments you could make today based on recommendations:"));
            content.add(
                    getGridWithMorningDiff(
                            reportData.getGoodInvestmentsBasedOnGoodRecommendation(),
                            morningMap
                    )
            );

            content.add(new H3("Bad investments you could make today.... based on recommendations:"));
            content.add(
                    getGridWithMorningDiff(
                            reportData.getBadInvestmentsBasedOnGoodRecommendation(),
                            morningMap
                    )
            );

            BigDecimal probability = reportData.getGoodInvestmentProbabilityBasedOnGoodToday();
            content.add(new H3("Probability of making good investment today: " + probability.multiply(BigDecimal.valueOf(100)) + "%"));
        }
    }

    private VerticalLayout getGrid(List<StockPriceData> data) {
        Grid<StockPriceData> grid = new Grid<>(StockPriceData.class);
        grid.setColumns("symbol", "changePercent", "transactions");
        Grid.Column<StockPriceData> changeCol = grid.getColumnByKey("changePercent");

        grid.sort(Collections.singletonList(
                new GridSortOrder<>(changeCol, SortDirection.DESCENDING)
        ));

        ListDataProvider<StockPriceData> dataProvider = new ListDataProvider<>(data);
        grid.setDataProvider(dataProvider);

        TextField search = getSearch(dataProvider);

        return new VerticalLayout(search, grid);
    }

    private void showRiskyToInvest(ReportData reportData) {
        content.removeAll();

        if (reportData.getRiskyToInvest() == null) {
            showWarningNotification("No stocks found for today. Please try again later.");
            return;
        }

        VerticalLayout goodGrid = getGrid(reportData.getRiskyToInvest());
        content.add(new H3("Risky to invest today:"));
        content.add(goodGrid);
        Map<String, BigDecimal> morningMap = toMorningMap(reportData.getRiskyToInvest());

        if (!reportData.getGoodInvestmentsBasedOnRiskyRecommendation().isEmpty()) {

            content.add(new H3("Good investments you could make today based on recommendations:"));
            content.add(
                    getGridWithMorningDiff(
                            reportData.getGoodInvestmentsBasedOnRiskyRecommendation(),
                            morningMap
                    )
            );

            content.add(new H3("Bad investments you could make today.... based on recommendations:"));
            content.add(
                    getGridWithMorningDiff(
                            reportData.getBadInvestmentsBasedOnRiskyRecommendation(),
                            morningMap
                    )
            );

            BigDecimal probability = reportData.getGoodInvestmentProbabilityBasedOnRiskyToday();
            content.add(new H3("Probability of making good investment today: " + probability.multiply(BigDecimal.valueOf(100)) + "%"));
        }
    }

    private void showBestToInvest(ReportData reportData) {
        content.removeAll();
        if (reportData.getBestToInvest() == null) {
            showWarningNotification("No stocks found for today. Please try again later.");
            return;
        }

        VerticalLayout goodGrid = getGrid(reportData.getBestToInvest());
        content.add(new H3("Best to invest today:"));
        content.add(goodGrid);

        Map<String, BigDecimal> morningMap = toMorningMap(reportData.getBestToInvest());

        if (!reportData.getGoodInvestmentsBasedOnBestRecommendation().isEmpty()) {
            content.add(new H3("Good investments you could make today based on recommendations:"));
            content.add(getGridWithMorningDiff(reportData.getGoodInvestmentsBasedOnBestRecommendation(), morningMap));

            content.add(new H3("Bad investments you could make today.... based on recommendations:"));
            content.add(getGridWithMorningDiff(reportData.getBadInvestmentsBasedOnBestRecommendation(), morningMap));

            BigDecimal probability = reportData.getGoodInvestmentProbabilityBasedOnBestToday();
            content.add(new H3("Probability of making good investment today: " + probability.multiply(BigDecimal.valueOf(100)) + "%"));
        }
    }

    private Map<String, BigDecimal> toMorningMap(List<StockPriceData> morningData) {
        return morningData.stream()
                .collect(Collectors.toMap(
                        StockPriceData::getSymbol,
                        StockPriceData::getChangePercent
                ));
    }

    private VerticalLayout getGridWithMorningDiff(List<StockPriceData> data,
                                                  Map<String, BigDecimal> morningMap) {

        Grid<StockPriceData> grid = new Grid<>(StockPriceData.class);
        grid.setColumns("symbol", "changePercent", "transactions");

        grid.addColumn(item -> {
            BigDecimal morning = morningMap.get(item.getSymbol());
            if (morning == null) return "-";

            return item.getChangePercent()
                    .subtract(morning)
                    .toString();
        }).setHeader("Diff vs Morning (%)");

        Grid.Column<StockPriceData> changeCol = grid.getColumnByKey("changePercent");
        grid.sort(Collections.singletonList(
                new GridSortOrder<>(changeCol, SortDirection.DESCENDING)
        ));

        ListDataProvider<StockPriceData> dataProvider = new ListDataProvider<>(data);
        grid.setDataProvider(dataProvider);

        TextField search = getSearch(dataProvider);
        return new VerticalLayout(search, grid);
    }
}