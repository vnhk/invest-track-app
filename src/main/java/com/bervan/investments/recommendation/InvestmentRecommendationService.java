package com.bervan.investments.recommendation;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.ReportData;
import com.bervan.investtrack.service.recommendations.RecommendationStrategy;
import com.bervan.logging.BaseProcessContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

// Low-Code START
@Service
public class InvestmentRecommendationService extends BaseService<UUID, InvestmentRecommendation> {
    private final Map<String, RecommendationStrategy> strategies;

    public InvestmentRecommendationService(BaseRepository<InvestmentRecommendation, UUID> repository, SearchService searchService, Map<String, RecommendationStrategy> strategies) {
        super(repository, searchService);
        this.strategies = strategies;
    }

    @Scheduled(cron = "0 0 23 * * *", zone = "Europe/Warsaw")
    public void saveRecommendations() {
        ArrayList<String> strategiesNames = new ArrayList<>(strategies.keySet());
        LocalDate date = LocalDate.now();
        for (String strategiesName : strategiesNames) {
            ReportData reportData = strategies.get(strategiesName).loadReportData(date, BaseProcessContext.builder().processName("savingRecommendations").build());
            for (StockPriceData stockPriceData : reportData.getGoodInvestmentsBasedOnRiskyRecommendation()) {
                StockPriceData morningRecommendation = reportData.getRiskyToInvest().stream().filter(e -> e.getSymbol().equals(stockPriceData.getSymbol())).findFirst().get();
                InvestmentRecommendation recommendation = getInvestmentRecommendation(stockPriceData, strategiesName, morningRecommendation, "Risky", date, "Good");
                save(recommendation);
            }

            for (StockPriceData stockPriceData : reportData.getBadInvestmentsBasedOnRiskyRecommendation()) {
                StockPriceData morningRecommendation = reportData.getRiskyToInvest().stream().filter(e -> e.getSymbol().equals(stockPriceData.getSymbol())).findFirst().get();
                InvestmentRecommendation recommendation = getInvestmentRecommendation(stockPriceData, strategiesName, morningRecommendation, "Risky", date, "Bad");
                save(recommendation);
            }

            for (StockPriceData stockPriceData : reportData.getGoodInvestmentsBasedOnGoodRecommendation()) {
                StockPriceData morningRecommendation = reportData.getGoodToInvest().stream().filter(e -> e.getSymbol().equals(stockPriceData.getSymbol())).findFirst().get();
                InvestmentRecommendation recommendation = getInvestmentRecommendation(stockPriceData, strategiesName, morningRecommendation, "Good", date, "Good");
                save(recommendation);
            }

            for (StockPriceData stockPriceData : reportData.getBadInvestmentsBasedOnGoodRecommendation()) {
                StockPriceData morningRecommendation = reportData.getGoodToInvest().stream().filter(e -> e.getSymbol().equals(stockPriceData.getSymbol())).findFirst().get();
                InvestmentRecommendation recommendation = getInvestmentRecommendation(stockPriceData, strategiesName, morningRecommendation, "Good", date, "Bad");
                save(recommendation);
            }

            for (StockPriceData stockPriceData : reportData.getGoodInvestmentsBasedOnBestRecommendation()) {
                StockPriceData morningRecommendation = reportData.getBestToInvest().stream().filter(e -> e.getSymbol().equals(stockPriceData.getSymbol())).findFirst().get();
                InvestmentRecommendation recommendation = getInvestmentRecommendation(stockPriceData, strategiesName, morningRecommendation, "Best", date, "Good");
                save(recommendation);
            }

            for (StockPriceData stockPriceData : reportData.getBadInvestmentsBasedOnBestRecommendation()) {
                StockPriceData morningRecommendation = reportData.getBestToInvest().stream().filter(e -> e.getSymbol().equals(stockPriceData.getSymbol())).findFirst().get();
                InvestmentRecommendation recommendation = getInvestmentRecommendation(stockPriceData, strategiesName, morningRecommendation, "Best", date, "Bad");
                save(recommendation);
            }
        }
    }

    private InvestmentRecommendation getInvestmentRecommendation(StockPriceData stockPriceData, String strategiesName,
                                                                 StockPriceData morningRecommendation, String recType, LocalDate date, String result
    ) {
        InvestmentRecommendation recommendation = new InvestmentRecommendation();
        recommendation.setSymbol(stockPriceData.getSymbol());
        recommendation.setStrategy(strategiesName);
        recommendation.setChangeInPercentMorning(morningRecommendation.getChangePercent());
        recommendation.setChangeInPercentEvening(stockPriceData.getChangePercent());
        recommendation.setRecommendationType(recType);
        recommendation.setRecommendationResult(result);
        recommendation.setDate(new SimpleDateFormat("dd-MM-yyyy").format(date));
        return recommendation;
    }

}
// Low-Code END
