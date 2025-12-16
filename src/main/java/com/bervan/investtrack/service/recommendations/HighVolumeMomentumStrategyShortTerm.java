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

@Service("High Volume Momentum Strategy")
public class HighVolumeMomentumStrategyShortTerm implements ShortTermRecommendationStrategy {

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final BaseExcelImport baseExcelImport;
    private final FileDiskStorageService fileDiskStorageService;

    public HighVolumeMomentumStrategyShortTerm(FileDiskStorageService fileDiskStorageService) {
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
            log.error(ctx.map(), "HighVolumeMomentumStrategy morning error", e);
            return reportData;
        }

        // ---------- MORNING RECOMMENDATIONS ----------
        List<StockPriceData> candidates = morningData.stream()
                .filter(d -> d.getSymbol() != null)
                .filter(d -> d.getChangePercent() != null && d.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .filter(d -> d.getTransactions() != null && d.getTransactions() >= 10)
                .sorted(Comparator.comparing(StockPriceData::getTransactions).reversed())
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

        // store morning closing prices for evening comparison
        Map<String, BigDecimal> morningMap = morningData.stream()
                .filter(d -> d.getSymbol() != null && d.getChangePercent() != null)
                .collect(Collectors.toMap(
                        StockPriceData::getSymbol,
                        StockPriceData::getChangePercent,
                        (a, b) -> a
                ));

        reportData.setMorningMap(morningData.stream()
                .filter(d -> d.getSymbol() != null)
                .collect(Collectors.toMap(
                        StockPriceData::getSymbol,
                        Function.identity(),
                        (a, b) -> a))
        );

        // ---------- EVENING ANALYSIS ----------
        if (!fileDiskStorageService.isTmpFile(todayEvening)) {
            return reportData;
        }

        List<StockPriceData> eveningData;
        try (Workbook wb = baseExcelImport.load(fileDiskStorageService.getTmpFile(todayEvening).toFile())) {
            eveningData = (List<StockPriceData>) baseExcelImport.importExcel(wb);
        } catch (Exception e) {
            log.error(ctx.map(), "HighVolumeMomentumStrategy evening error", e);
            return reportData;
        }

        // compare with morning prices
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