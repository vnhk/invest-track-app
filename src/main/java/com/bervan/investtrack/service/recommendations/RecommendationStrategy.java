package com.bervan.investtrack.service.recommendations;

import com.bervan.investtrack.model.StockPriceData;
import com.bervan.investtrack.service.ReportData;
import com.bervan.logging.BaseProcessContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RecommendationStrategy {

    ReportData loadReportData(LocalDate day, BaseProcessContext recommendationContext);

    default List<StockPriceData> getGood(List<StockPriceData> rec,
                                         List<StockPriceData> evening) {

        Map<String, StockPriceData> map = evening.stream()
                .filter(e -> e.getSymbol() != null)
                .collect(Collectors.toMap(
                        StockPriceData::getSymbol,
                        Function.identity(),
                        (a, b) -> a));

        return rec.stream()
                .map(r -> map.get(r.getSymbol()))
                .filter(Objects::nonNull)
                .filter(e -> e.getChangePercent() != null
                        && e.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    default List<StockPriceData> getBad(List<StockPriceData> rec,
                                        List<StockPriceData> evening) {

        Map<String, StockPriceData> map = evening.stream()
                .filter(e -> e.getSymbol() != null)
                .collect(Collectors.toMap(
                        StockPriceData::getSymbol,
                        Function.identity(),
                        (a, b) -> a));

        return rec.stream()
                .map(r -> map.get(r.getSymbol()))
                .filter(Objects::nonNull)
                .filter(e -> e.getChangePercent() != null
                        && e.getChangePercent().compareTo(BigDecimal.ZERO) < 0)
                .toList();
    }

    default String fileName(LocalDate day, String part) {
        // Format day and month with leading zero if needed (01–09)
        String dayOfMonth = String.format("%02d", day.getDayOfMonth());
        String month = String.format("%02d", day.getMonthValue());

        return "STOCKS_PL_" + dayOfMonth + "_" + month + "_" + part + ".xlsx";
    }


    default BigDecimal probability(List<?> good, List<?> bad) {
        int g = good == null ? 0 : good.size();
        int b = bad == null ? 0 : bad.size();
        int t = g + b;

        return t == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(g)
                .divide(BigDecimal.valueOf(t), 2, RoundingMode.HALF_UP);
    }

    default List<?> concatGood(ReportData r) {
        return Stream.of(
                        r.getGoodInvestmentsBasedOnBestRecommendation(),
                        r.getGoodInvestmentsBasedOnGoodRecommendation(),
                        r.getGoodInvestmentsBasedOnRiskyRecommendation())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    default List<?> concatBad(ReportData r) {
        return Stream.of(
                        r.getBadInvestmentsBasedOnBestRecommendation(),
                        r.getBadInvestmentsBasedOnGoodRecommendation(),
                        r.getBadInvestmentsBasedOnRiskyRecommendation())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

}
