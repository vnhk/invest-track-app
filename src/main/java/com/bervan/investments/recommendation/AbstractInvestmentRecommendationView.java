package com.bervan.investments.recommendation;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.service.BaseService;
import com.bervan.common.user.User;
import com.bervan.common.user.UserRepository;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.investtrack.InvestTrackPageLayout;

import java.util.UUID;

public abstract class AbstractInvestmentRecommendationView extends AbstractBervanTableView<UUID, InvestmentRecommendation> {
    public static final String ROUTE_NAME = "/invest-track-app/recommendations-history";
    private final UserRepository userRepository;

    public AbstractInvestmentRecommendationView(BaseService<UUID, InvestmentRecommendation> service, BervanViewConfig bervanViewConfig, UserRepository userRepository) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, bervanViewConfig, InvestmentRecommendation.class);
        this.userRepository = userRepository;

        renderCommonComponents();
    }

    @Override
    protected InvestmentRecommendation preSaveActions(InvestmentRecommendation newItem) {
        User user = userRepository.findByUsername("COMMON_USER").get();
        newItem.addOwner(user);
        return super.preSaveActions(newItem);
    }
}
