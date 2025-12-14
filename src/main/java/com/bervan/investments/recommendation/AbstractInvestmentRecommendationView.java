package com.bervan.investments.recommendation;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.service.BaseService;
import com.bervan.common.user.UserRepository;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.investtrack.InvestTrackPageLayout;

import java.util.UUID;

public abstract class AbstractInvestmentRecommendationView extends AbstractBervanTableView<UUID, InvestmentRecommendation> {
    public static final String ROUTE_NAME = "/invest-track-app/recommendations-history";

    public AbstractInvestmentRecommendationView(BaseService<UUID, InvestmentRecommendation> service, BervanViewConfig bervanViewConfig, UserRepository userRepository) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, bervanViewConfig, InvestmentRecommendation.class);

        renderCommonComponents();
    }
}
