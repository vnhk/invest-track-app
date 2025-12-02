package com.bervan.investtrack.service.recommendations;

import com.bervan.investtrack.service.ReportData;
import com.bervan.logging.BaseProcessContext;

import java.time.LocalDate;

public interface RecommendationStrategy {

    ReportData loadReportData(LocalDate day, BaseProcessContext recommendationContext);
}
