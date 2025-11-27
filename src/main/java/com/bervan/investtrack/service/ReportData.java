package com.bervan.investtrack.service;

import com.bervan.investtrack.model.StockPriceData;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ReportData {
    private List<StockPriceData> bestToInvest = new ArrayList<>();
    private List<StockPriceData> goodToInvest = new ArrayList<>();
    private List<StockPriceData> riskyToInvest = new ArrayList<>();
    private List<StockPriceData> goodInvestmentsBasedOnBestRecommendation = new ArrayList<>();
    private List<StockPriceData> goodInvestmentsBasedOnGoodRecommendation = new ArrayList<>();
    private List<StockPriceData> goodInvestmentsBasedOnRiskyRecommendation = new ArrayList<>();
    private List<StockPriceData> badInvestmentsBasedOnBestRecommendation = new ArrayList<>();
    private List<StockPriceData> badInvestmentsBasedOnGoodRecommendation = new ArrayList<>();
    private List<StockPriceData> badInvestmentsBasedOnRiskyRecommendation = new ArrayList<>();
    private BigDecimal goodInvestmentProbabilityBasedOnBestToday = BigDecimal.valueOf(0);
    private BigDecimal goodInvestmentProbabilityBasedOnGoodToday = BigDecimal.valueOf(0);
    private BigDecimal goodInvestmentProbabilityBasedOnRiskyToday = BigDecimal.valueOf(0);
    private BigDecimal goodInvestmentTotalProbabilityBasedOnToday = BigDecimal.valueOf(0);
    private Map<String, StockPriceData> morningMap = new HashMap<>();
}
