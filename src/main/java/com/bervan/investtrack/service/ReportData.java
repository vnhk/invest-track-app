package com.bervan.investtrack.service;

import com.bervan.investtrack.model.StockPriceData;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ReportData {
    private List<StockPriceData> bestToInvest;
    private List<StockPriceData> goodToInvest;
    private List<StockPriceData> riskyToInvest;
    private List<StockPriceData> goodInvestmentsBasedOnBestRecommendation;
    private List<StockPriceData> goodInvestmentsBasedOnGoodRecommendation;
    private List<StockPriceData> goodInvestmentsBasedOnRiskyRecommendation;
    private List<StockPriceData> badInvestmentsBasedOnBestRecommendation;
    private List<StockPriceData> badInvestmentsBasedOnGoodRecommendation;
    private List<StockPriceData> badInvestmentsBasedOnRiskyRecommendation;
    private BigDecimal goodInvestmentProbabilityBasedOnBestToday;
    private BigDecimal goodInvestmentProbabilityBasedOnGoodToday;
    private BigDecimal goodInvestmentProbabilityBasedOnRiskyToday;
    private BigDecimal goodInvestmentTotalProbabilityBasedOnToday;
    private Map<String, StockPriceData> morningMap;
}
