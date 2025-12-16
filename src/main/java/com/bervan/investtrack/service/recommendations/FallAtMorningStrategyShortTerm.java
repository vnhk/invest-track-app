package com.bervan.investtrack.service.recommendations;

import com.bervan.filestorage.service.FileDiskStorageService;
import com.bervan.ieentities.BaseExcelImport;
import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.ReportData;
import com.bervan.logging.BaseProcessContext;
import com.bervan.logging.JsonLogger;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("Fall At Morning Strategy")
public class FallAtMorningStrategyShortTerm implements ShortTermRecommendationStrategy {
    public static Integer minAmountOfTransactionsBestToInvest = 50;
    public static Integer minAmountOfTransactionsGoodToInvest = 25;
    public static Integer minAmountOfTransactionsRiskyToInvest = 5;
    public static BigDecimal maxPercentageChangeBestToInvest = BigDecimal.valueOf(-0.01);
    public static BigDecimal minPercentageChangeBestToInvest = BigDecimal.valueOf(-0.30);
    public static BigDecimal maxPercentageChangeGoodToInvest = BigDecimal.valueOf(-0.31);
    public static BigDecimal minPercentageChangeGoodToInvest = BigDecimal.valueOf(-0.70);
    public static BigDecimal maxPercentageChangeRiskyToInvest = BigDecimal.valueOf(-0.71);
    public static BigDecimal minPercentageChangeRiskyToInvest = BigDecimal.valueOf(-1.50);
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    private final BaseExcelImport baseExcelImport;
    private final FileDiskStorageService fileDiskStorageService;

    public FallAtMorningStrategyShortTerm(FileDiskStorageService fileDiskStorageService) {
        this.baseExcelImport = new BaseExcelImport(List.of(StockPriceData.class));
        this.fileDiskStorageService = fileDiskStorageService;
    }

    public ReportData loadReportData(LocalDate day, BaseProcessContext recommendationContext) {
        ReportData reportData = new ReportData();

        String todayMorning = "STOCKS_PL_" + day.getDayOfMonth() + "_"
                + day.getMonthValue() + "_" + "Morning" + ".xlsx";
        String todayEvening = "STOCKS_PL_" + day.getDayOfMonth() + "_"
                + day.getMonthValue() + "_" + "Evening" + ".xlsx";


        LocalDate dayBefore = day.minusDays(7);

        String yesterdayMorning = "STOCKS_PL_" + dayBefore.getDayOfMonth() + "_"
                + dayBefore.getMonthValue() + "_" + "Morning" + ".xlsx";
        String yesterdayEvening = "STOCKS_PL_" + dayBefore.getDayOfMonth() + "_"
                + dayBefore.getMonthValue() + "_" + "Evening" + ".xlsx";
        //check how many + stocks increased at Evening dayBefore and show %

        boolean todayMorningloaded = false;
        if (fileDiskStorageService.isTmpFile(todayMorning)) {
            Path tmpFile = fileDiskStorageService.getTmpFile(todayMorning);
            log.debug(recommendationContext.map(), "Loading excel file: " + tmpFile.toAbsolutePath());
            try (Workbook workbook = baseExcelImport.load(tmpFile.toFile())) {

                List<StockPriceData> data = (List<StockPriceData>) baseExcelImport.importExcel(workbook);

                Map<String, StockPriceData> morningBySymbol = data.stream()
                        .filter(d -> d.getSymbol() != null)
                        .collect(Collectors.toMap(StockPriceData::getSymbol, Function.identity(), (a, b) -> a));

                List<StockPriceData> risky = getRiskyToInvest(data);
                List<StockPriceData> best = getBestToInvest(data);
                List<StockPriceData> good = getGoodToInvest(data, best, risky);

                reportData.setBestToInvest(best);
                reportData.setGoodToInvest(good);
                reportData.setRiskyToInvest(risky);

                reportData.setMorningMap(morningBySymbol);

                todayMorningloaded = true;
            } catch (IOException e) {
                log.error(recommendationContext.map(), "Error loading excel file: " + tmpFile.toAbsolutePath(), e);
            }
        }

        if (todayMorningloaded && fileDiskStorageService.isTmpFile(todayEvening)) {
            Path tmpFile = fileDiskStorageService.getTmpFile(todayEvening);
            log.debug(recommendationContext.map(), "Loading excel file: " + tmpFile.toAbsolutePath());
            try (Workbook workbook = baseExcelImport.load(tmpFile.toFile())) {
                List<StockPriceData> todayEveningData = (List<StockPriceData>) baseExcelImport.importExcel(workbook);

                reportData.setGoodInvestmentsBasedOnBestRecommendation(getGoodInvestmentsBasedRecommendation(reportData.getBestToInvest(), todayEveningData));
                reportData.setGoodInvestmentsBasedOnGoodRecommendation(getGoodInvestmentsBasedRecommendation(reportData.getGoodToInvest(), todayEveningData));
                reportData.setGoodInvestmentsBasedOnRiskyRecommendation(getGoodInvestmentsBasedRecommendation(reportData.getRiskyToInvest(), todayEveningData));
                reportData.setBadInvestmentsBasedOnBestRecommendation(getBadInvestmentsBasedRecommendation(reportData.getBestToInvest(), todayEveningData));
                reportData.setBadInvestmentsBasedOnGoodRecommendation(getBadInvestmentsBasedRecommendation(reportData.getGoodToInvest(), todayEveningData));
                reportData.setBadInvestmentsBasedOnRiskyRecommendation(getBadInvestmentsBasedRecommendation(reportData.getRiskyToInvest(), todayEveningData));

                reportData.setGoodInvestmentProbabilityBasedOnBestToday(calculateProbability(reportData.getGoodInvestmentsBasedOnBestRecommendation(), reportData.getBadInvestmentsBasedOnBestRecommendation()));
                reportData.setGoodInvestmentProbabilityBasedOnGoodToday(calculateProbability(reportData.getGoodInvestmentsBasedOnGoodRecommendation(), reportData.getBadInvestmentsBasedOnGoodRecommendation()));
                reportData.setGoodInvestmentProbabilityBasedOnRiskyToday(calculateProbability(reportData.getGoodInvestmentsBasedOnRiskyRecommendation(), reportData.getBadInvestmentsBasedOnRiskyRecommendation()));
                reportData.setGoodInvestmentTotalProbabilityBasedOnToday(calculateTotalProbability(reportData, recommendationContext));
            } catch (IOException e) {
                log.error(recommendationContext.map(), "Error loading excel file: " + tmpFile.toAbsolutePath(), e);
            }
        }

        return reportData;
    }

    private BigDecimal calculateProbability(List<StockPriceData> goodRecommendation, List<StockPriceData> badRecommendation) {
        int good = (goodRecommendation == null) ? 0 : goodRecommendation.size();
        int bad = (badRecommendation == null) ? 0 : badRecommendation.size();
        int total = good + bad;
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(good).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalProbability(ReportData reportData, BaseProcessContext recommendationContext) {
        int goodCount = safeSize(reportData.getGoodInvestmentsBasedOnBestRecommendation())
                + safeSize(reportData.getGoodInvestmentsBasedOnGoodRecommendation())
                + safeSize(reportData.getGoodInvestmentsBasedOnRiskyRecommendation());
        log.info(recommendationContext.map(), "calculateTotalProbability: Good count: " + goodCount);

        int badCount = safeSize(reportData.getBadInvestmentsBasedOnBestRecommendation())
                + safeSize(reportData.getBadInvestmentsBasedOnGoodRecommendation())
                + safeSize(reportData.getBadInvestmentsBasedOnRiskyRecommendation());

        log.info(recommendationContext.map(), "calculateTotalProbability: Bad count: " + badCount);

        int total = goodCount + badCount;
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(goodCount)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNullScale(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

    private int safeSize(List<?> l) {
        return (l == null) ? 0 : l.size();
    }

    private List<StockPriceData> getGoodInvestmentsBasedRecommendation(List<StockPriceData> recommendationData, List<StockPriceData> todayEveningData) {
        Map<String, StockPriceData> afternoonMap = todayEveningData.stream()
                .filter(d -> d.getSymbol() != null)
                .filter(d -> d.getChangePercent().compareTo(BigDecimal.ZERO) >= 0)
                .collect(Collectors.toMap(StockPriceData::getSymbol, Function.identity(), (a, b) -> a));

        List<StockPriceData> good = new ArrayList<>();
        for (StockPriceData rec : recommendationData) {
            StockPriceData aft = afternoonMap.get(rec.getSymbol());
            if (aft != null && aft.getChangePercent() != null && aft.getChangePercent().compareTo(rec.getChangePercent()) > 0) {
                good.add(aft);
            }
        }
        return good;
    }

    private List<StockPriceData> getBadInvestmentsBasedRecommendation(List<StockPriceData> recommendationData, List<StockPriceData> todayEveningData) {
        Map<String, StockPriceData> afternoonMap = todayEveningData.stream()
                .filter(d -> d.getSymbol() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol, Function.identity(), (a, b) -> a));

        List<StockPriceData> bad = new ArrayList<>();
        for (StockPriceData rec : recommendationData) {
            StockPriceData aft = afternoonMap.get(rec.getSymbol());
            if (aft != null && aft.getChangePercent() != null && aft.getChangePercent().compareTo(rec.getChangePercent()) < 0) {
                bad.add(aft);
            }
        }
        return bad;
    }

    private List<StockPriceData> getBestToInvest(List<StockPriceData> todayMorningData) {
        return todayMorningData.stream()
                .filter(e -> e.getChangePercent() != null)
                .filter(e -> e.getChangePercent().compareTo(BigDecimal.ZERO) < 0)
                .filter(e -> e.getTransactions() != null && e.getTransactions() >= minAmountOfTransactionsBestToInvest)
                .filter(e -> e.getChangePercent().compareTo(minPercentageChangeBestToInvest) >= 0)
                .filter(e -> e.getChangePercent().compareTo(maxPercentageChangeBestToInvest) <= 0)
                .toList();
    }

    private List<StockPriceData> getRiskyToInvest(List<StockPriceData> todayMorningData) {
        return todayMorningData.stream()
                .filter(e -> e.getChangePercent() != null)
                .filter(e -> e.getChangePercent().compareTo(BigDecimal.ZERO) < 0)
                .filter(e -> e.getTransactions() != null && e.getTransactions() < minAmountOfTransactionsGoodToInvest)
                .filter(e -> e.getTransactions() >= minAmountOfTransactionsRiskyToInvest)
                .filter(e -> e.getChangePercent().compareTo(minPercentageChangeRiskyToInvest) >= 0)
                .filter(e -> e.getChangePercent().compareTo(maxPercentageChangeRiskyToInvest) <= 0)
                .toList();
    }

    private List<StockPriceData> getGoodToInvest(
            List<StockPriceData> todayMorningData,
            List<StockPriceData> best,
            List<StockPriceData> risky) {

        Set<String> bestSymbols = best.stream()
                .map(StockPriceData::getSymbol)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> riskySymbols = risky.stream()
                .map(StockPriceData::getSymbol)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return todayMorningData.stream()
                .filter(e -> e.getChangePercent() != null)
                .filter(e -> e.getChangePercent().compareTo(BigDecimal.ZERO) < 0)
                .filter(e -> e.getTransactions() != null && e.getTransactions() < minAmountOfTransactionsBestToInvest)
                .filter(e -> e.getTransactions() >= minAmountOfTransactionsGoodToInvest)
                .filter(e -> e.getChangePercent().compareTo(minPercentageChangeGoodToInvest) >= 0)
                .filter(e -> e.getChangePercent().compareTo(maxPercentageChangeGoodToInvest) <= 0)
                .filter(e -> !bestSymbols.contains(e.getSymbol()))
                .filter(e -> !riskySymbols.contains(e.getSymbol()))
                .toList();
    }
}
