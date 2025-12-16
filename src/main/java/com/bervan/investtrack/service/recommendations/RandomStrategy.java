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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("Random Strategy")
public class RandomStrategy implements RecommendationStrategy {

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final BaseExcelImport baseExcelImport;
    private final FileDiskStorageService fileDiskStorageService;
    private final Random random = new Random();

    public RandomStrategy(FileDiskStorageService fileDiskStorageService) {
        this.baseExcelImport = new BaseExcelImport(List.of(StockPriceData.class));
        this.fileDiskStorageService = fileDiskStorageService;
    }

    @Override
    public ReportData loadReportData(LocalDate day, BaseProcessContext ctx) {
        ReportData reportData = new ReportData();

        String todayMorning = fileName(day, "Morning");
        String todayEvening = fileName(day, "Evening");

        // ---------- MORNING ----------
        if (!fileDiskStorageService.isTmpFile(todayMorning)) {
            return reportData;
        }

        try (Workbook wb = baseExcelImport.load(
                fileDiskStorageService.getTmpFile(todayMorning).toFile())) {

            List<StockPriceData> morningData =
                    (List<StockPriceData>) baseExcelImport.importExcel(wb);

            // Prefer stocks with at least 10 transactions (soft filter)
            List<StockPriceData> filtered = morningData.stream()
                    .filter(d -> d.getTransactions() != null && d.getTransactions() >= 10)
                    .toList();

            // Fallback to all if too few after filtering
            List<StockPriceData> baseList =
                    filtered.size() >= 10 ? filtered : morningData;

            List<StockPriceData> shuffled = new ArrayList<>(baseList);
            Collections.shuffle(shuffled, random);

            // Max 10 recommendations total
            int limit = Math.min(10, shuffled.size());
            List<StockPriceData> selected = shuffled.subList(0, limit);

            // Fixed proportions: Best / Good / Risky
            int bestSize = Math.min(3, selected.size());
            int goodSize = Math.min(4, Math.max(0, selected.size() - bestSize));

            List<StockPriceData> best = selected.subList(0, bestSize);
            List<StockPriceData> good = selected.subList(bestSize, bestSize + goodSize);
            List<StockPriceData> risky = selected.subList(bestSize + goodSize, selected.size());

            reportData.setBestToInvest(best);
            reportData.setGoodToInvest(good);
            reportData.setRiskyToInvest(risky);

            reportData.setMorningMap(
                    morningData.stream()
                            .filter(d -> d.getSymbol() != null)
                            .collect(Collectors.toMap(
                                    StockPriceData::getSymbol,
                                    Function.identity(),
                                    (a, b) -> a))
            );

        } catch (Exception e) {
            log.error(ctx.map(), "RandomStrategy morning load error", e);
            return reportData;
        }

        // ---------- EVENING ----------
        if (!fileDiskStorageService.isTmpFile(todayEvening)) {
            return reportData;
        }

        try (Workbook wb = baseExcelImport.load(
                fileDiskStorageService.getTmpFile(todayEvening).toFile())) {

            List<StockPriceData> eveningData =
                    (List<StockPriceData>) baseExcelImport.importExcel(wb);

            reportData.setGoodInvestmentsBasedOnBestRecommendation(
                    getGood(reportData.getBestToInvest(), eveningData));
            reportData.setGoodInvestmentsBasedOnGoodRecommendation(
                    getGood(reportData.getGoodToInvest(), eveningData));
            reportData.setGoodInvestmentsBasedOnRiskyRecommendation(
                    getGood(reportData.getRiskyToInvest(), eveningData));

            reportData.setBadInvestmentsBasedOnBestRecommendation(
                    getBad(reportData.getBestToInvest(), eveningData));
            reportData.setBadInvestmentsBasedOnGoodRecommendation(
                    getBad(reportData.getGoodToInvest(), eveningData));
            reportData.setBadInvestmentsBasedOnRiskyRecommendation(
                    getBad(reportData.getRiskyToInvest(), eveningData));

            reportData.setGoodInvestmentProbabilityBasedOnBestToday(
                    probability(
                            reportData.getGoodInvestmentsBasedOnBestRecommendation(),
                            reportData.getBadInvestmentsBasedOnBestRecommendation()));

            reportData.setGoodInvestmentProbabilityBasedOnGoodToday(
                    probability(
                            reportData.getGoodInvestmentsBasedOnGoodRecommendation(),
                            reportData.getBadInvestmentsBasedOnGoodRecommendation()));

            reportData.setGoodInvestmentProbabilityBasedOnRiskyToday(
                    probability(
                            reportData.getGoodInvestmentsBasedOnRiskyRecommendation(),
                            reportData.getBadInvestmentsBasedOnRiskyRecommendation()));

            reportData.setGoodInvestmentTotalProbabilityBasedOnToday(
                    probability(
                            concatGood(reportData),
                            concatBad(reportData)));

        } catch (Exception e) {
            log.error(ctx.map(), "RandomStrategy evening load error", e);
        }

        return reportData;
    }
}