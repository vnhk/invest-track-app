package com.bervan.investments.recommendation;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.investtrack.InvestTrackPageLayout;

import java.util.UUID;


// Low-Code START
public abstract class AbstractInvestmentRecommendationView extends AbstractBervanTableView<UUID, InvestmentRecommendation> {
    public static final String ROUTE_NAME = "/investment-recommendations-data";

    protected AbstractInvestmentRecommendationView(BaseService<UUID,InvestmentRecommendation> service, BervanViewConfig bervanViewConfig) {
        super(new InvestTrackPageLayout(ROUTE_NAME, null), service, bervanViewConfig, InvestmentRecommendation.class);
    }

}
// Low-Code END
