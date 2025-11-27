package com.bervan.investtrack.service;

import com.bervan.core.model.BervanLogger;
import com.bervan.filestorage.model.BervanMockMultiPartFile;
import com.bervan.filestorage.service.FileDiskStorageService;
import com.bervan.ieentities.BaseExcelExport;
import com.bervan.ieentities.BaseExcelImport;
import com.bervan.investtrack.model.StockPriceData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StockPriceReportService {
    public static Integer minAmountOfTransactionsBestToInvest = 50;
    public static Integer minAmountOfTransactionsGoodToInvest = 25;
    public static Integer minAmountOfTransactionsRiskyToInvest = 5;
    public static BigDecimal maxPercentageChangeBestToInvest = BigDecimal.valueOf(10);
    public static BigDecimal minPercentageChangeBestToInvest = BigDecimal.valueOf(1);
    public static BigDecimal maxPercentageChangeGoodToInvest = BigDecimal.valueOf(15);
    public static BigDecimal minPercentageChangeGoodToInvest = BigDecimal.valueOf(1);
    public static BigDecimal maxPercentageChangeRiskyToInvest = BigDecimal.valueOf(50);
    public static BigDecimal minPercentageChangeRiskyToInvest = BigDecimal.valueOf(5);
    private final BaseExcelExport baseExcelExport;
    private final BaseExcelImport baseExcelImport;
    private final FileDiskStorageService fileDiskStorageService;
    private final String URL = "https://www.bankier.pl/gielda/notowania/akcje";

    protected StockPriceReportService(BervanLogger logger, FileDiskStorageService fileDiskStorageService) {
        this.fileDiskStorageService = fileDiskStorageService;
        baseExcelExport = new BaseExcelExport();
        baseExcelImport = new BaseExcelImport(List.of(StockPriceData.class), logger);
    }


    @Scheduled(cron = "0 30 10 * * MON-FRI")
    public void loadStockPricesMorning() {
        log.info("loadStockPricesMorning started");
        try {
            loadStockPrices("10_30");
        } catch (Exception e) {
            log.error("Error loading morning stock prices", e);
        }
        log.info("loadStockPricesMorning finished");
    }

    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void loadStockPricesBeforeClose() {
        log.info("loadStockPricesBeforeClose started");
        try {
            loadStockPrices("15_30");
        } catch (Exception e) {
            log.error("Error loading evening stock prices", e);
        }
        log.info("loadStockPricesBeforeClose finished");
    }

    private void loadStockPrices(String x) throws IOException {
        Document doc = Jsoup.connect(URL).get();

        Element table = doc.select("table.sortTable").first();
        Elements rows = table.select("tbody tr");

        List<StockPriceData> results = new ArrayList<>();
        LocalDate now = LocalDate.now();

        long i = 0;
        for (Element row : rows) {
            Elements cols = row.select("td");

            StockPriceData item = new StockPriceData();
            try {
                item.setId(i++);
                item.setSymbol(getString(cols.select(".colWalor")));
                item.setPrice(getBigDecimal(cols.select(".colKurs")));
                item.setChange(getBigDecimal(cols.select(".colZmiana")));
                item.setChangePercent(getBigDecimal(cols.select(".colZmianaProcentowa")));
                item.setTransactions(getInteger(cols.select(".colLiczbaTransakcji")));
                item.setDate(getString(cols.select(".colAktualizacja")));
                String dateToCheck = now.getDayOfMonth() + "." + now.getMonthValue();
                String date = item.getDate();
                if (!date.contains(dateToCheck)) {
                    continue;
                }

            } catch (Exception e) {
                continue;
            }

            results.add(item);
        }

        log.info("Loaded " + results.size() + " stock prices");

        String filename = "STOCKS_PL_" + now.getDayOfMonth() + "_"
                + now.getMonthValue() + "_" + x + ".xlsx";

        try (Workbook workbook = baseExcelExport.exportExcel(results, null)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] byteArray = outputStream.toByteArray();
            fileDiskStorageService.storeTmp(new BervanMockMultiPartFile(filename, filename, Files.probeContentType(Path.of(filename)),
                    new ByteArrayInputStream(byteArray)), filename);

            log.info("Saved stock data excel file: " + filename);
        }
    }

    private String getString(Elements select) {
        return select.get(0).text();
    }

    private BigDecimal getBigDecimal(Elements select) {
        String text = select.get(0).text();
        return BigDecimal.valueOf(Double.parseDouble(text.replace(",", ".")
                .replace("%", "")
                .replace(" ", "")));
    }

    private Integer getInteger(Elements select) {
        String text = select.get(0).text();
        return Integer.valueOf(text.replace(" ", "").trim());
    }

    public ReportData loadReportData() {
        ReportData reportData = new ReportData();

        LocalDate now = LocalDate.now();
        LocalDate yesterday = now.minusDays(1);
        String today1030 = "STOCKS_PL_" + now.getDayOfMonth() + "_"
                + now.getMonthValue() + "_" + "10_30" + ".xlsx";
        String today1530 = "STOCKS_PL_" + now.getDayOfMonth() + "_"
                + now.getMonthValue() + "_" + "15_30" + ".xlsx";

        String yesterday1030 = "STOCKS_PL_" + yesterday.getDayOfMonth() + "_"
                + yesterday.getMonthValue() + "_" + "10_30" + ".xlsx";
        String yesterday1530 = "STOCKS_PL_" + yesterday.getDayOfMonth() + "_"
                + yesterday.getMonthValue() + "_" + "15_30" + ".xlsx";
        //check how many + stocks increased at 15_30 yesterday and show %

        boolean today1030loaded = false;
        if (fileDiskStorageService.isTmpFile(today1030)) {
            Path tmpFile = fileDiskStorageService.getTmpFile(today1030);
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

                today1030loaded = true;
            } catch (IOException e) {
                log.error("Error loading excel file: " + tmpFile.toAbsolutePath(), e);
            }
        }

        if (today1030loaded && fileDiskStorageService.isTmpFile(today1530)) {
            Path tmpFile = fileDiskStorageService.getTmpFile(today1530);
            try (Workbook workbook = baseExcelImport.load(tmpFile.toFile())) {
                List<StockPriceData> today1530data = (List<StockPriceData>) baseExcelImport.importExcel(workbook);

                reportData.setGoodInvestmentsBasedOnBestRecommendation(getGoodInvestmentsBasedRecommendation(reportData.getBestToInvest(), today1530data));
                reportData.setGoodInvestmentsBasedOnGoodRecommendation(getGoodInvestmentsBasedRecommendation(reportData.getGoodToInvest(), today1530data));
                reportData.setGoodInvestmentsBasedOnRiskyRecommendation(getGoodInvestmentsBasedRecommendation(reportData.getRiskyToInvest(), today1530data));
                reportData.setBadInvestmentsBasedOnBestRecommendation(getBadInvestmentsBasedRecommendation(reportData.getBestToInvest(), today1530data));
                reportData.setBadInvestmentsBasedOnGoodRecommendation(getBadInvestmentsBasedRecommendation(reportData.getGoodToInvest(), today1530data));
                reportData.setBadInvestmentsBasedOnRiskyRecommendation(getBadInvestmentsBasedRecommendation(reportData.getRiskyToInvest(), today1530data));

                reportData.setGoodInvestmentProbabilityBasedOnBestToday(calculateProbability(reportData.getGoodInvestmentsBasedOnBestRecommendation(), reportData.getBadInvestmentsBasedOnBestRecommendation()));
                reportData.setGoodInvestmentProbabilityBasedOnGoodToday(calculateProbability(reportData.getGoodInvestmentsBasedOnGoodRecommendation(), reportData.getBadInvestmentsBasedOnGoodRecommendation()));
                reportData.setGoodInvestmentProbabilityBasedOnRiskyToday(calculateProbability(reportData.getGoodInvestmentsBasedOnRiskyRecommendation(), reportData.getBadInvestmentsBasedOnRiskyRecommendation()));
                reportData.setGoodInvestmentTotalProbabilityBasedOnToday(calculateTotalProbability(reportData));
            } catch (IOException e) {
                log.error("Error loading excel file: " + tmpFile.toAbsolutePath(), e);
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

    private BigDecimal calculateTotalProbability(ReportData reportData) {
        int goodCount = safeSize(reportData.getGoodInvestmentsBasedOnBestRecommendation())
                + safeSize(reportData.getGoodInvestmentsBasedOnGoodRecommendation())
                + safeSize(reportData.getGoodInvestmentsBasedOnRiskyRecommendation());

        int badCount = safeSize(reportData.getBadInvestmentsBasedOnBestRecommendation())
                + safeSize(reportData.getBadInvestmentsBasedOnGoodRecommendation())
                + safeSize(reportData.getBadInvestmentsBasedOnRiskyRecommendation());

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

    private List<StockPriceData> getGoodInvestmentsBasedRecommendation(List<StockPriceData> recommendationData, List<StockPriceData> todays1530) {
        // English: create map for fast lookup of afternoon data by symbol
        Map<String, StockPriceData> afternoonMap = todays1530.stream()
                .filter(d -> d.getSymbol() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol, Function.identity(), (a, b) -> a));

        List<StockPriceData> good = new ArrayList<>();
        for (StockPriceData rec : recommendationData) {
            StockPriceData aft = afternoonMap.get(rec.getSymbol());
            if (aft != null && aft.getChangePercent() != null && aft.getChangePercent().compareTo(BigDecimal.ZERO) > 0) {
                good.add(aft);
            }
        }
        return good;
    }

    private List<StockPriceData> getBadInvestmentsBasedRecommendation(List<StockPriceData> recommendationData, List<StockPriceData> todays1530) {
        Map<String, StockPriceData> afternoonMap = todays1530.stream()
                .filter(d -> d.getSymbol() != null)
                .collect(Collectors.toMap(StockPriceData::getSymbol, Function.identity(), (a, b) -> a));

        List<StockPriceData> bad = new ArrayList<>();
        for (StockPriceData rec : recommendationData) {
            StockPriceData aft = afternoonMap.get(rec.getSymbol());
            if (aft != null && aft.getChangePercent() != null && aft.getChangePercent().compareTo(BigDecimal.ZERO) <= 0) {
                bad.add(aft);
            }
        }
        return bad;
    }

    private List<StockPriceData> getBestToInvest(List<StockPriceData> todayMorningData) {
        return todayMorningData.stream()
                .filter(e -> e.getChangePercent() != null && e.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .filter(e -> e.getTransactions() != null && e.getTransactions() >= minAmountOfTransactionsBestToInvest)
                .filter(e -> e.getChangePercent().compareTo(maxPercentageChangeBestToInvest) <= 0)
                .filter(e -> e.getChangePercent().compareTo(minPercentageChangeBestToInvest) >= 0)
                .toList();
    }

    private List<StockPriceData> getRiskyToInvest(List<StockPriceData> todayMorningData) {
        return todayMorningData.stream()
                .filter(e -> e.getChangePercent() != null && e.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .filter(e -> e.getTransactions() != null && e.getTransactions() < minAmountOfTransactionsGoodToInvest)
                .filter(e -> e.getTransactions() >= minAmountOfTransactionsRiskyToInvest)
                .filter(e -> e.getChangePercent().compareTo(minPercentageChangeRiskyToInvest) >= 0)
                .filter(e -> e.getChangePercent().compareTo(maxPercentageChangeRiskyToInvest) <= 0)
                .toList();
    }

    private List<StockPriceData> getGoodToInvest(List<StockPriceData> todayMorningData,
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
                .filter(e -> e.getChangePercent() != null && e.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .filter(e -> e.getTransactions() != null && e.getTransactions() < minAmountOfTransactionsBestToInvest)
                .filter(e -> e.getTransactions() >= minAmountOfTransactionsGoodToInvest)
                .filter(e -> e.getChangePercent().compareTo(minPercentageChangeGoodToInvest) >= 0)
                .filter(e -> !bestSymbols.contains(e.getSymbol()))
                .filter(e -> !riskySymbols.contains(e.getSymbol()))
                .toList();
    }
}
