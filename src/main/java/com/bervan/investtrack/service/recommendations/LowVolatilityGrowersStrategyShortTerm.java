package com.bervan.investtrack.service.recommendations;

import com.bervan.filestorage.service.FileDiskStorageService;
import com.bervan.ieentities.BaseExcelImport;
import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.ReportData;
import com.bervan.logging.BaseProcessContext;
import com.bervan.logging.JsonLogger;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("Low Volatility Growers Strategy")
public class LowVolatilityGrowersStrategyShortTerm implements ShortTermRecommendationStrategy {

    private static final BigDecimal MIN_CHANGE = BigDecimal.valueOf(0.5);
    private static final BigDecimal MAX_CHANGE = BigDecimal.valueOf(2.0);

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final BaseExcelImport baseExcelImport;
    private final FileDiskStorageService fileDiskStorageService;

    public LowVolatilityGrowersStrategyShortTerm(FileDiskStorageService fileDiskStorageService) {
        this.baseExcelImport = new BaseExcelImport(List.of(StockPriceData.class));
        this.fileDiskStorageService = fileDiskStorageService;
    }

    @Override
    public ReportData loadReportData(LocalDate day, BaseProcessContext ctx) {
        ReportData reportData = new ReportData();

        String todayMorning = fileName(day, "Morning");
        String todayEvening = fileName(day, "Evening");

        if (!fileDiskStorageService.isTmpFile(todayMorning)) {
            return reportData;
        }

        List<StockPriceData> morningData;
        try (Workbook wb = baseExcelImport.load(fileDiskStorageService.getTmpFile(todayMorning).toFile())) {
            morningData = (List<StockPriceData>) baseExcelImport.importExcel(wb);
        } catch (Exception e) {
            log.error(ctx.map(), "LowVolatilityGrowersStrategy morning error", e);
            return reportData;
        }

        // ---------- MORNING RECOMMENDATIONS ----------
        List<StockPriceData> candidates = morningData.stream()
                .filter(d -> d.getSymbol() != null)
                .filter(d -> d.getChangePercent() != null)
                .filter(d -> d.getChangePercent().compareTo(MIN_CHANGE) >= 0)
                .filter(d -> d.getChangePercent().compareTo(MAX_CHANGE) <= 0)
                .filter(d -> d.getTransactions() != null && d.getTransactions() >= 20)
                .sorted(Comparator.comparing(StockPriceData::getChangePercent))
                .limit(10)
                .toList();

        int bestSize = Math.min(3, candidates.size());
        int goodSize = Math.min(4, Math.max(0, candidates.size() - bestSize));

        List<StockPriceData> best = candidates.subList(0, bestSize);
        List<StockPriceData> good = candidates.subList(bestSize, bestSize + goodSize);
        List<StockPriceData> risky = candidates.subList(bestSize + goodSize, candidates.size());

        reportData.setBestToInvest(best);
        reportData.setGoodToInvest(good);
        reportData.setRiskyToInvest(risky);

        Map<String, BigDecimal> morningMap = morningData.stream()
                .filter(d -> d.getSymbol() != null && d.getChangePercent() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol, StockPriceData::getChangePercent, (a, b) -> a));

        reportData.setMorningMap(
                morningData.stream()
                        .filter(d -> d.getSymbol() != null)
                        .collect(Collectors.toMap(
                                StockPriceData::getSymbol,
                                Function.identity(),
                                (a, b) -> a))
        );

        // ---------- EVENING ----------
        if (!fileDiskStorageService.isTmpFile(todayEvening)) {
            return reportData;
        }

        List<StockPriceData> eveningData;
        try (Workbook wb = baseExcelImport.load(fileDiskStorageService.getTmpFile(todayEvening).toFile())) {
            eveningData = (List<StockPriceData>) baseExcelImport.importExcel(wb);
        } catch (Exception e) {
            log.error(ctx.map(), "LowVolatilityGrowersStrategy evening error", e);
            return reportData;
        }

        // compare evening with morning
        reportData.setGoodInvestmentsBasedOnBestRecommendation(
                getGoodComparedToMorning(best, eveningData, morningMap));
        reportData.setGoodInvestmentsBasedOnGoodRecommendation(
                getGoodComparedToMorning(good, eveningData, morningMap));
        reportData.setGoodInvestmentsBasedOnRiskyRecommendation(
                getGoodComparedToMorning(risky, eveningData, morningMap));

        reportData.setBadInvestmentsBasedOnBestRecommendation(
                getBadComparedToMorning(best, eveningData, morningMap));
        reportData.setBadInvestmentsBasedOnGoodRecommendation(
                getBadComparedToMorning(good, eveningData, morningMap));
        reportData.setBadInvestmentsBasedOnRiskyRecommendation(
                getBadComparedToMorning(risky, eveningData, morningMap));

        reportData.setGoodInvestmentProbabilityBasedOnBestToday(
                probability(reportData.getGoodInvestmentsBasedOnBestRecommendation(),
                        reportData.getBadInvestmentsBasedOnBestRecommendation()));

        reportData.setGoodInvestmentProbabilityBasedOnGoodToday(
                probability(reportData.getGoodInvestmentsBasedOnGoodRecommendation(),
                        reportData.getBadInvestmentsBasedOnGoodRecommendation()));

        reportData.setGoodInvestmentProbabilityBasedOnRiskyToday(
                probability(reportData.getGoodInvestmentsBasedOnRiskyRecommendation(),
                        reportData.getBadInvestmentsBasedOnRiskyRecommendation()));

        reportData.setGoodInvestmentTotalProbabilityBasedOnToday(
                probability(concatGood(reportData), concatBad(reportData)));

        return reportData;
    }
}