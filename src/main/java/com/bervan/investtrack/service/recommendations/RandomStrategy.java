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

        if (!fileDiskStorageService.isTmpFile(todayMorning)) {
            return reportData;
        }

        try (Workbook wb = baseExcelImport.load(
                fileDiskStorageService.getTmpFile(todayMorning).toFile())) {

            List<StockPriceData> morningData =
                    (List<StockPriceData>) baseExcelImport.importExcel(wb);

            // shuffle for randomness
            List<StockPriceData> shuffled = new ArrayList<>(morningData);
            Collections.shuffle(shuffled, random);

            int total = shuffled.size();
            int bestSize = (int) (total * 0.2);
            int goodSize = (int) (total * 0.5);

            List<StockPriceData> best = shuffled.subList(0, bestSize);
            List<StockPriceData> good = shuffled.subList(bestSize, bestSize + goodSize);
            List<StockPriceData> risky = shuffled.subList(bestSize + goodSize, total);

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
                    probability(reportData.getGoodInvestmentsBasedOnBestRecommendation(),
                            reportData.getBadInvestmentsBasedOnBestRecommendation()));

            reportData.setGoodInvestmentProbabilityBasedOnGoodToday(
                    probability(reportData.getGoodInvestmentsBasedOnGoodRecommendation(),
                            reportData.getBadInvestmentsBasedOnGoodRecommendation()));

            reportData.setGoodInvestmentProbabilityBasedOnRiskyToday(
                    probability(reportData.getGoodInvestmentsBasedOnRiskyRecommendation(),
                            reportData.getBadInvestmentsBasedOnRiskyRecommendation()));

            reportData.setGoodInvestmentTotalProbabilityBasedOnToday(
                    probability(
                            concatGood(reportData),
                            concatBad(reportData))
            );

        } catch (Exception e) {
            log.error(ctx.map(), "RandomStrategy evening load error", e);
        }

        return reportData;
    }

    private String fileName(LocalDate day, String part) {
        return "STOCKS_PL_" + day.getDayOfMonth() + "_" + day.getMonthValue() + "_" + part + ".xlsx";
    }

    private List<StockPriceData> getGood(List<StockPriceData> rec, List<StockPriceData> evening) {
        Map<String, StockPriceData> map = evening.stream()
                .filter(e -> e.getSymbol() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol, Function.identity(), (a, b) -> a));

        return rec.stream()
                .map(r -> map.get(r.getSymbol()))
                .filter(Objects::nonNull)
                .filter(e -> e.getChangePercent() != null && e.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private List<StockPriceData> getBad(List<StockPriceData> rec, List<StockPriceData> evening) {
        Map<String, StockPriceData> map = evening.stream()
                .filter(e -> e.getSymbol() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol, Function.identity(), (a, b) -> a));

        return rec.stream()
                .map(r -> map.get(r.getSymbol()))
                .filter(Objects::nonNull)
                .filter(e -> e.getChangePercent() != null && e.getChangePercent().compareTo(BigDecimal.ZERO) < 0)
                .toList();
    }

    private BigDecimal probability(List<?> good, List<?> bad) {
        int g = good == null ? 0 : good.size();
        int b = bad == null ? 0 : bad.size();
        int t = g + b;
        return t == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(g)
                .divide(BigDecimal.valueOf(t), 2, RoundingMode.HALF_UP);
    }

    private List<?> concatGood(ReportData r) {
        return Stream.of(
                r.getGoodInvestmentsBasedOnBestRecommendation(),
                r.getGoodInvestmentsBasedOnGoodRecommendation(),
                r.getGoodInvestmentsBasedOnRiskyRecommendation()
        ).filter(Objects::nonNull).flatMap(List::stream).toList();
    }

    private List<?> concatBad(ReportData r) {
        return Stream.of(
                r.getBadInvestmentsBasedOnBestRecommendation(),
                r.getBadInvestmentsBasedOnGoodRecommendation(),
                r.getBadInvestmentsBasedOnRiskyRecommendation()
        ).filter(Objects::nonNull).flatMap(List::stream).toList();
    }
}