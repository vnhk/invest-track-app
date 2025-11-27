package com.bervan.investtrack.view;

import com.bervan.common.view.AbstractPageView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.ReportData;
import com.bervan.investtrack.service.StockPriceReportService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public abstract class AbstractReportsRecommendationsView extends AbstractPageView {
    public static final String ROUTE_NAME = "/invest-track-app/recommendations";

    private final StockPriceReportService stockPriceReportService;
    private final VerticalLayout content = new VerticalLayout();

    protected AbstractReportsRecommendationsView(StockPriceReportService stockPriceReportService, StockPriceReportService stockPriceReportService1) {
        this.stockPriceReportService = stockPriceReportService1;
        add(new InvestTrackPageLayout(ROUTE_NAME, null));

        ReportData reportData = stockPriceReportService.loadReportData();

        Tabs tabs = getTabs(reportData);

        if (reportData.getGoodInvestmentTotalProbabilityBasedOnToday() != null) {
            add(new H3("Total probability of making good investment today: "
                    + reportData.getGoodInvestmentTotalProbabilityBasedOnToday().setScale(2, BigDecimal.ROUND_HALF_UP) + "%"));
        }

        add(tabs);
        add(content);

        showBestToInvest(reportData);
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


        Grid<StockPriceData> goodGrid = new Grid<>(StockPriceData.class);
        goodGrid.setItems(reportData.getGoodToInvest());
        content.add(new H3("Good to invest today:"));
        content.add(goodGrid);

        if (!reportData.getGoodInvestmentsBasedOnGoodRecommendation().isEmpty()) {
            Grid<StockPriceData> goodAfternoonGrid = new Grid<>(StockPriceData.class);
            goodAfternoonGrid.setItems(reportData.getGoodInvestmentsBasedOnGoodRecommendation());
            content.add(new H3("Good investments you could make today based on recommendations:"));
            content.add(goodAfternoonGrid);

            Grid<StockPriceData> badAfternoonGrid = new Grid<>(StockPriceData.class);
            goodGrid.setItems(reportData.getBadInvestmentsBasedOnGoodRecommendation());
            content.add(new H3("Bad investments you could make today.... based on recommendations:"));
            content.add(badAfternoonGrid);

            BigDecimal probability = reportData.getGoodInvestmentProbabilityBasedOnGoodToday();
            content.add(new H3("Probability of making good investment today: "
                    + probability.setScale(2, BigDecimal.ROUND_HALF_UP) + "%"));
        }
    }

    private void showRiskyToInvest(ReportData reportData) {
        content.removeAll();

        if (reportData.getRiskyToInvest() == null) {
            showWarningNotification("No stocks found for today. Please try again later.");
            return;
        }

        Grid<StockPriceData> goodGrid = new Grid<>(StockPriceData.class);
        goodGrid.setItems(reportData.getRiskyToInvest());
        content.add(new H3("Risky to invest today:"));
        content.add(goodGrid);

        if (!reportData.getGoodInvestmentsBasedOnRiskyRecommendation().isEmpty()) {
            Grid<StockPriceData> goodAfternoonGrid = new Grid<>(StockPriceData.class);
            goodAfternoonGrid.setItems(reportData.getGoodInvestmentsBasedOnRiskyRecommendation());
            content.add(new H3("Good investments you could make today based on recommendations:"));
            content.add(goodAfternoonGrid);

            Grid<StockPriceData> badAfternoonGrid = new Grid<>(StockPriceData.class);
            goodGrid.setItems(reportData.getBadInvestmentsBasedOnRiskyRecommendation());
            content.add(new H3("Bad investments you could make today.... based on recommendations:"));
            content.add(badAfternoonGrid);

            BigDecimal probability = reportData.getGoodInvestmentProbabilityBasedOnRiskyToday();
            content.add(new H3("Probability of making good investment today: "
                    + probability.setScale(2, BigDecimal.ROUND_HALF_UP) + "%"));
        }
    }

    private void showBestToInvest(ReportData reportData) {
        content.removeAll();
        if (reportData.getBestToInvest() == null) {
            showWarningNotification("No stocks found for today. Please try again later.");
            return;
        }

        Grid<StockPriceData> goodGrid = new Grid<>(StockPriceData.class);
        content.add(new H3("Best to invest today:"));
        goodGrid.setItems(reportData.getBestToInvest());
        content.add(goodGrid);

        if (!reportData.getGoodInvestmentsBasedOnBestRecommendation().isEmpty()) {
            Grid<StockPriceData> goodAfternoonGrid = new Grid<>(StockPriceData.class);
            goodAfternoonGrid.setItems(reportData.getGoodInvestmentsBasedOnBestRecommendation());
            content.add(new H3("Good investments you could make today based on recommendations:"));
            content.add(goodAfternoonGrid);

            Grid<StockPriceData> badAfternoonGrid = new Grid<>(StockPriceData.class);
            goodGrid.setItems(reportData.getBadInvestmentsBasedOnBestRecommendation());
            content.add(new H3("Bad investments you could make today.... based on recommendations:"));
            content.add(badAfternoonGrid);

            BigDecimal probability = reportData.getGoodInvestmentProbabilityBasedOnBestToday();
            content.add(new H3("Probability of making good investment today: "
                    + probability.setScale(2, BigDecimal.ROUND_HALF_UP) + "%"));
        }
    }
}