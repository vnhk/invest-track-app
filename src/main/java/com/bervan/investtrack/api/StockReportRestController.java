package com.bervan.investtrack.api;

import com.bervan.asynctask.AsyncTask;
import com.bervan.asynctask.AsyncTaskService;
import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.ReportData;
import com.bervan.investtrack.service.StockPriceReportService;
import com.bervan.logging.BaseProcessContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invest-track/stock-report")
public class StockReportRestController {

    private final StockPriceReportService reportService;
    private final AsyncTaskService asyncTaskService;
    private final BaseProcessContext ctx = BaseProcessContext.builder()
            .processName("stockReport").build();

    public StockReportRestController(StockPriceReportService reportService, AsyncTaskService asyncTaskService) {
        this.reportService = reportService;
        this.asyncTaskService = asyncTaskService;
    }

    record StockDto(String symbol, String date, BigDecimal price, BigDecimal change, BigDecimal changePercent, Integer transactions) {}

    record ReportDto(
            List<StockDto> bestToInvest,
            List<StockDto> goodToInvest,
            List<StockDto> riskyToInvest,
            List<StockDto> goodInvestmentsBasedOnBestRecommendation,
            List<StockDto> goodInvestmentsBasedOnGoodRecommendation,
            List<StockDto> goodInvestmentsBasedOnRiskyRecommendation,
            List<StockDto> badInvestmentsBasedOnBestRecommendation,
            List<StockDto> badInvestmentsBasedOnGoodRecommendation,
            List<StockDto> badInvestmentsBasedOnRiskyRecommendation,
            BigDecimal goodInvestmentProbabilityBasedOnBestToday,
            BigDecimal goodInvestmentProbabilityBasedOnGoodToday,
            BigDecimal goodInvestmentProbabilityBasedOnRiskyToday,
            BigDecimal goodInvestmentTotalProbabilityBasedOnToday
    ) {}

    private StockDto toDto(StockPriceData s) {
        return new StockDto(s.getSymbol(), s.getDate(), s.getPrice(), s.getChange(), s.getChangePercent(), s.getTransactions());
    }

    private List<StockDto> toDtos(List<StockPriceData> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/strategies")
    public ResponseEntity<List<String>> strategies() {
        return ResponseEntity.ok(reportService.getStrategyNames());
    }

    @GetMapping
    public ResponseEntity<ReportDto> report(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String strategy
    ) {

        LocalDate reportDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<String> strategies = reportService.getStrategyNames();
        String strategyName = (strategy != null && strategies.contains(strategy)) ? strategy : strategies.get(0);

        ReportData data = reportService.loadReportData(reportDate, ctx, strategyName);

        return ResponseEntity.ok(new ReportDto(
                toDtos(data.getBestToInvest()),
                toDtos(data.getGoodToInvest()),
                toDtos(data.getRiskyToInvest()),
                toDtos(data.getGoodInvestmentsBasedOnBestRecommendation()),
                toDtos(data.getGoodInvestmentsBasedOnGoodRecommendation()),
                toDtos(data.getGoodInvestmentsBasedOnRiskyRecommendation()),
                toDtos(data.getBadInvestmentsBasedOnBestRecommendation()),
                toDtos(data.getBadInvestmentsBasedOnGoodRecommendation()),
                toDtos(data.getBadInvestmentsBasedOnRiskyRecommendation()),
                data.getGoodInvestmentProbabilityBasedOnBestToday(),
                data.getGoodInvestmentProbabilityBasedOnGoodToday(),
                data.getGoodInvestmentProbabilityBasedOnRiskyToday(),
                data.getGoodInvestmentTotalProbabilityBasedOnToday()
        ));
    }

    @PostMapping("/trigger/morning")
    public ResponseEntity<Map<String, String>> triggerMorning() {
        SecurityContext context = SecurityContextHolder.getContext();
        AsyncTask task = asyncTaskService.createAndStoreAsyncTask();
        new Thread(() -> {
            SecurityContextHolder.setContext(context);
            AsyncTask running = asyncTaskService.setInProgress(task, "Morning report is being generated.");
            try {
                reportService.loadStockPricesMorning();
                asyncTaskService.setFinished(running, "Morning report generated successfully.");
            } catch (Exception e) {
                asyncTaskService.setFailed(running, e.getMessage());
            }
        }).start();
        return ResponseEntity.accepted().body(Map.of("message", "Morning report generation started."));
    }

    @PostMapping("/trigger/evening")
    public ResponseEntity<Map<String, String>> triggerEvening() {
        SecurityContext context = SecurityContextHolder.getContext();
        AsyncTask task = asyncTaskService.createAndStoreAsyncTask();
        new Thread(() -> {
            SecurityContextHolder.setContext(context);
            AsyncTask running = asyncTaskService.setInProgress(task, "Evening report is being generated.");
            try {
                reportService.loadStockPricesEvening();
                asyncTaskService.setFinished(running, "Evening report generated successfully.");
            } catch (Exception e) {
                asyncTaskService.setFailed(running, e.getMessage());
            }
        }).start();
        return ResponseEntity.accepted().body(Map.of("message", "Evening report generation started."));
    }
}
