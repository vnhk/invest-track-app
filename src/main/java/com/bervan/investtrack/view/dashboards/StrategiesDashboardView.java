package com.bervan.investtrack.view.dashboards;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.bervan.investments.recommendation.InvestmentRecommendation;
import com.bervan.investments.recommendation.InvestmentRecommendationService;
import com.bervan.investtrack.service.recommendations.RecommendationStrategy;
import com.bervan.investtrack.view.charts.StrategyBGRHistoryChart;
import com.bervan.logging.JsonLogger;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

public class StrategiesDashboardView extends VerticalLayout {
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final Map<String, RecommendationStrategy> strategies;
    private final InvestmentRecommendationService recommendationService;

    public StrategiesDashboardView(Map<String, RecommendationStrategy> strategies,
                                   InvestmentRecommendationService recommendationService) {
        this.strategies = strategies;
        this.recommendationService = recommendationService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        int TEN_YEARS = 3650;

        Map<String, List<InvestmentRecommendation>> data = new HashMap<>();

        for (String strategy : strategies.keySet()) {
            SearchRequest request = new SearchRequest();
            request.addCriterion("STRATEGY", InvestmentRecommendation.class, "strategy", SearchOperation.EQUALS_OPERATION, strategy);
            List<InvestmentRecommendation> recommendations = recommendationService.load(request, Pageable.ofSize(TEN_YEARS), "id", SortDirection.ASC);
            data.put(strategy, recommendations);
        }

        add(createMainContent(data));
    }

    private VerticalLayout createMainContent(Map<String, List<InvestmentRecommendation>> data) {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);

        List<StrategyBGRHistoryChart> historyBGRCharts = getStrategyBGRHistoryCharts(data);

        VerticalLayout gridLayout = new VerticalLayout();
        gridLayout.setSizeFull();
        gridLayout.setPadding(false);
        gridLayout.setSpacing(false);

        HorizontalLayout row = null;
        int count = 0;
        for (StrategyBGRHistoryChart chart : historyBGRCharts) {
            if (count % 2 == 0) {
                row = new HorizontalLayout();
                row.setWidthFull();
                row.setPadding(false);
                row.setSpacing(true);
                gridLayout.add(row);
            }

            chart.setWidth("50%");
            chart.setHeight("400px");
            row.add(chart);

            count++;
        }

        content.add(gridLayout);
        content.getStyle().set("overflow", "auto");
        return content;
    }

    private List<StrategyBGRHistoryChart> getStrategyBGRHistoryCharts(Map<String, List<InvestmentRecommendation>> data) {
        List<StrategyBGRHistoryChart> historyBGRCharts = new ArrayList<>();

        List<String> dates = new ArrayList<>();
        List<String> bestRecPercent = new ArrayList<>();
        List<String> goodRecPercent = new ArrayList<>();
        List<String> riskyRecPercent = new ArrayList<>();

        for (String strategy : data.keySet()) {

            Map<String, List<InvestmentRecommendation>> groupedByDate =
                    data.get(strategy).stream().collect(Collectors.groupingBy(InvestmentRecommendation::getDate));

            for (String date : groupedByDate.keySet()) {
                dates.add(date);
                groupedByDate.get(date).sort(Comparator.comparing(InvestmentRecommendation::getDate));
                List<InvestmentRecommendation> investmentRecommendations = groupedByDate.get(date);
                Map<String, List<InvestmentRecommendation>> groupedByType = investmentRecommendations.stream()
                        .collect(Collectors.groupingBy(InvestmentRecommendation::getRecommendationType));
                List<InvestmentRecommendation> risky = groupedByType.get("Risky");
                if (risky == null || risky.isEmpty()) {
                    log.warn("No risky recommendations found for strategy: " + strategy + " on date: " + date);
                } else {
                    riskyRecPercent.add(calculateChanceOfSuccess(risky));
                }
                List<InvestmentRecommendation> good = groupedByType.get("Good");
                if (good == null || good.isEmpty()) {
                    log.warn("No good recommendations found for strategy: " + strategy + " on date: " + date);
                } else {
                    goodRecPercent.add(calculateChanceOfSuccess(good));
                }
                List<InvestmentRecommendation> best = groupedByType.get("Best");
                if (best == null || best.isEmpty()) {
                    log.warn("No best recommendations found for strategy: " + strategy + " on date: " + date);
                } else {
                    bestRecPercent.add(calculateChanceOfSuccess(best));
                }

            }

            historyBGRCharts.add(new StrategyBGRHistoryChart(dates, bestRecPercent, goodRecPercent, riskyRecPercent));
        }
        return historyBGRCharts;
    }

    private String calculateChanceOfSuccess(List<InvestmentRecommendation> recommendations) {
        Map<String, List<InvestmentRecommendation>> groupedByResult = recommendations.stream().collect(Collectors.groupingBy(InvestmentRecommendation::getRecommendationResult));
        List<InvestmentRecommendation> goodRecommendations = groupedByResult.get("Good");
        List<InvestmentRecommendation> badRecommendations = groupedByResult.get("Bad");
        int goodRecommendationsSize = 0;
        int badRecommendationsSize = 0;
        if (goodRecommendations != null) {
            goodRecommendationsSize = goodRecommendations.size();
        }
        if (badRecommendations != null) {
            badRecommendationsSize = badRecommendations.size();
        }
        int totalRecommendations = goodRecommendationsSize + badRecommendationsSize;
        double chanceOfSuccess = (double) goodRecommendationsSize / totalRecommendations;
        return chanceOfSuccess + "";
    }
}