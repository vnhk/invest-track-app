package com.bervan.investtrack.service;

import com.bervan.asynctask.AsyncTaskService;
import com.bervan.common.service.PlaywrightService;
import com.bervan.filestorage.model.BervanMockMultiPartFile;
import com.bervan.filestorage.service.FileDiskStorageService;
import com.bervan.ieentities.BaseExcelExport;
import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.recommendations.RecommendationStrategy;
import com.bervan.logging.BaseProcessContext;
import com.bervan.logging.JsonLogger;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StockPriceReportService {
    private final JsonLogger log = JsonLogger.getLogger(getClass());
    private final BaseExcelExport baseExcelExport;
    private final PlaywrightService playwrightService;
    private final FileDiskStorageService fileDiskStorageService;
    private final AsyncTaskService asyncTaskService;
    private final String URL = "https://www.bankier.pl/gielda/notowania/akcje";
    private final BaseProcessContext loadStockPricesContext = BaseProcessContext.builder()
            .processName("loadStockPrices").build();
    private final Map<String, RecommendationStrategy> strategies;

    protected StockPriceReportService(PlaywrightService playwrightService,
                                      FileDiskStorageService fileDiskStorageService,
                                      AsyncTaskService asyncTaskService,
                                      Map<String, RecommendationStrategy> strategies) {
        this.playwrightService = playwrightService;
        this.fileDiskStorageService = fileDiskStorageService;
        this.asyncTaskService = asyncTaskService;
        this.strategies = strategies;
        baseExcelExport = new BaseExcelExport();
    }


    public List<String> getStrategyNames() {
        return new ArrayList<>(strategies.keySet());
    }

    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "Europe/Warsaw")
    public void loadStockPricesMorning() {
        log.info(loadStockPricesContext.map(), "loadStockPricesMorning started");
        try {
            loadStockPrices("Morning");
        } catch (Exception e) {
            log.error(loadStockPricesContext.map(), "Error loading morning stock prices", e);
        }
        log.info(loadStockPricesContext.map(), "loadStockPricesMorning finished");
    }

    @Scheduled(cron = "0 30 17 * * MON-FRI", zone = "Europe/Warsaw")
    public void loadStockPricesEvening() {
        log.info(loadStockPricesContext.map(), "loadStockPricesEvening started");
        try {
            loadStockPrices("Evening");
        } catch (Exception e) {
            log.error(loadStockPricesContext.map(), "Error loading evening stock prices", e);
        }
        log.info(loadStockPricesContext.map(), "loadStockPricesEvening finished");
    }

    private void loadStockPrices(String x) {
        LocalDate now = LocalDate.now();

        String dayOfMonth = String.valueOf(now.getDayOfMonth());
        String month = String.valueOf(now.getMonthValue());
        if (dayOfMonth.length() == 1) dayOfMonth = "0" + dayOfMonth;
        if (month.length() == 1) month = "0" + month;
        String dateToCheck = dayOfMonth + "." + month;

        log.debug(loadStockPricesContext.map(), "Loading stock prices for date: " + dateToCheck);

        try (Playwright playwright = Playwright.create()) {
            Page page = playwrightService.getPage(playwright, true);
            page.navigate(URL);

            List<Locator> rows = page.locator("table.sortTable tbody tr").all();

            List<StockPriceData> results = new ArrayList<>();
            long i = 0;

            for (Locator row : rows) {
                try {
                    StockPriceData item = new StockPriceData();
                    item.setId(i++);

                    // Extract values from the row
                    item.setSymbol(row.locator(".colWalor").innerText().trim());
                    item.setPrice(getBigDecimal(row.locator(".colKurs").innerText().trim()));
                    item.setChange(getBigDecimal(row.locator(".colZmiana").innerText().trim()));
                    item.setChangePercent(getBigDecimal(row.locator(".colZmianaProcentowa").innerText().trim()));
                    item.setTransactions(getInteger(row.locator(".colLiczbaTransakcji").innerText().trim()));
                    item.setDate(row.locator(".colAktualizacja").innerText().trim());

                    // Filter by today

                    if (!item.getDate().contains(dateToCheck)) {
                        continue;
                    }

                    results.add(item);

                } catch (Exception e) {
                    log.debug(loadStockPricesContext.map(), "Error loading stock price data", e);
                }
            }

            log.info(loadStockPricesContext.map(), "Loaded " + results.size() + " stock prices");

            if (results.size() < 200) {
                log.warn(loadStockPricesContext.map(), "Not enough stock prices loaded!");
            }

            String filename = "STOCKS_PL_" + now.getDayOfMonth() + "_"
                    + now.getMonthValue() + "_" + x + ".xlsx";

            try (Workbook workbook = baseExcelExport.exportExcel(results, null)) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                byte[] byteArray = outputStream.toByteArray();

                fileDiskStorageService.storeTmp(
                        new BervanMockMultiPartFile(
                                filename,
                                filename,
                                Files.probeContentType(Path.of(filename)),
                                new ByteArrayInputStream(byteArray)
                        ),
                        filename
                );

                log.info(loadStockPricesContext.map(), "Saved stock data excel file: " + filename);
            }

        } catch (Exception e) {
            log.error(loadStockPricesContext.map(), "Failed to load stock prices", e);
        }
    }


    private BigDecimal getBigDecimal(String text) {
        return BigDecimal.valueOf(Double.parseDouble(text.replace(",", ".")
                .replace(" ", "")
                .replace("%", "")
                .replace(" ", "")));
    }

    private Integer getInteger(String text) {
        return Integer.valueOf(text.replace(" ", "").replace(" ", "").trim());
    }

    public ReportData loadReportData(LocalDate date, BaseProcessContext recommendationContext, String strategyName) {
        return strategies.get(strategyName).loadReportData(date, recommendationContext);
    }
}
