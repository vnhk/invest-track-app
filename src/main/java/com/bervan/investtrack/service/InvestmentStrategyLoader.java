package com.bervan.investtrack.service;

import com.bervan.common.config.DynamicConfigLoader;
import com.bervan.investtrack.service.recommendations.ShortTermRecommendationStrategy;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InvestmentStrategyLoader implements DynamicConfigLoader {

    private final Map<String, ShortTermRecommendationStrategy> strategies;

    public InvestmentStrategyLoader(Map<String, ShortTermRecommendationStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public @NotNull String getSupportedClass() {
        return "InvestmentRecommendation";
    }

    @Override
    public @NotNull String getSupportedField() {
        return "strategy";
    }

    @Override
    public @NotNull List<String> getDynamicStrValuesMap() {
        return strategies.keySet().stream().toList();
    }
}
