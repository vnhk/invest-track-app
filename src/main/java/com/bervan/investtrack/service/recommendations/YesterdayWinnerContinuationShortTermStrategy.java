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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("Yesterday Winner Continuation Strategy")
public class YesterdayWinnerContinuationShortTermStrategy implements ShortTermRecommendationStrategy {

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final BaseExcelImport baseExcelImport;
    private final FileDiskStorageService fileDiskStorageService;

    public YesterdayWinnerContinuationShortTermStrategy(FileDiskStorageService fileDiskStorageService) {
        this.baseExcelImport = new BaseExcelImport(List.of(StockPriceData.class));
        this.fileDiskStorageService = fileDiskStorageService;
    }

    @Override
    public ReportData loadReportData(LocalDate day, BaseProcessContext ctx) {
        ReportData reportData = new ReportData();

        LocalDate yesterday = day.minusDays(1);
        String yesterdayEvening = fileName(yesterday, "Evening");
        String todayMorning = fileName(day, "Morning");

        if (!fileDiskStorageService.isTmpFile(yesterdayEvening) ||
                !fileDiskStorageService.isTmpFile(todayMorning)) {
            return reportData;
        }

        List<StockPriceData> yesterdayEveningData;
        List<StockPriceData> todayMorningData;

        try (Workbook wb = baseExcelImport.load(
                fileDiskStorageService.getTmpFile(yesterdayEvening).toFile())) {
            yesterdayEveningData = (List<StockPriceData>) baseExcelImport.importExcel(wb);
        } catch (Exception e) {
            log.error(ctx.map(), "YesterdayWinnerContinuationStrategy yesterday evening load error", e);
            return reportData;
        }

        try (Workbook wb = baseExcelImport.load(
                fileDiskStorageService.getTmpFile(todayMorning).toFile())) {
            todayMorningData = (List<StockPriceData>) baseExcelImport.importExcel(wb);
        } catch (Exception e) {
            log.error(ctx.map(), "YesterdayWinnerContinuationStrategy today morning load error", e);
            return reportData;
        }

        List<StockPriceData> candidates = yesterdayEveningData.stream()
                .filter(d -> d.getSymbol() != null)
                .filter(d -> d.getChangePercent() != null && d.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .filter(d -> d.getTransactions() != null && d.getTransactions() >= 10)
                .sorted(Comparator.comparing(StockPriceData::getChangePercent).reversed())
                .limit(10)
                .toList();

        int bestSize = Math.min(3, candidates.size());
        int goodSize = Math.min(4, Math.max(0, candidates.size() - bestSize));

        List<StockPriceData> best = filterMorning(todayMorningData, candidates.subList(0, bestSize));
        List<StockPriceData> good = filterMorning(todayMorningData, candidates.subList(bestSize, bestSize + goodSize));
        List<StockPriceData> risky = filterMorning(todayMorningData, candidates.subList(bestSize + goodSize, candidates.size()));

        reportData.setBestToInvest(best);
        reportData.setGoodToInvest(good);
        reportData.setRiskyToInvest(risky);

        Map<String, BigDecimal> morningMap = todayMorningData.stream()
                .filter(d -> d.getSymbol() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol,
                        StockPriceData::getChangePercent,
                        (a, b) -> a));
        reportData.setMorningMap(todayMorningData.stream()
                .filter(d -> d.getSymbol() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol,
                        Function.identity(),
                        (a, b) -> a)));

        String todayEvening = fileName(day, "Evening");
        if (fileDiskStorageService.isTmpFile(todayEvening)) {
            try (Workbook wb = baseExcelImport.load(
                    fileDiskStorageService.getTmpFile(todayEvening).toFile())) {

                List<StockPriceData> eveningData =
                        (List<StockPriceData>) baseExcelImport.importExcel(wb);

                reportData.setGoodInvestmentsBasedOnBestRecommendation(
                        getGoodComparedToMorning(reportData.getBestToInvest(), eveningData, morningMap));
                reportData.setGoodInvestmentsBasedOnGoodRecommendation(
                        getGoodComparedToMorning(reportData.getGoodToInvest(), eveningData, morningMap));
                reportData.setGoodInvestmentsBasedOnRiskyRecommendation(
                        getGoodComparedToMorning(reportData.getRiskyToInvest(), eveningData, morningMap));

                reportData.setBadInvestmentsBasedOnBestRecommendation(
                        getBadComparedToMorning(reportData.getBestToInvest(), eveningData, morningMap));
                reportData.setBadInvestmentsBasedOnGoodRecommendation(
                        getBadComparedToMorning(reportData.getGoodToInvest(), eveningData, morningMap));
                reportData.setBadInvestmentsBasedOnRiskyRecommendation(
                        getBadComparedToMorning(reportData.getRiskyToInvest(), eveningData, morningMap));

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
                        probability(concatGood(reportData),
                                concatBad(reportData)));

            } catch (Exception e) {
                log.error(ctx.map(), "YesterdayWinnerContinuationStrategy today evening load error", e);
            }
        }

        return reportData;
    }

    private List<StockPriceData> filterMorning(List<StockPriceData> morningData, List<StockPriceData> symbols) {
        Set<String> symbolsSet = symbols.stream()
                .map(StockPriceData::getSymbol)
                .collect(Collectors.toSet());
        return morningData.stream()
                .filter(d -> d.getSymbol() != null && symbolsSet.contains(d.getSymbol()))
                .toList();
    }
}