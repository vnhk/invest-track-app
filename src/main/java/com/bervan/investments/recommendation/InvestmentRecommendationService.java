package com.bervan.investments.recommendation;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// Low-Code START
@Service
public class InvestmentRecommendationService extends BaseService<UUID, InvestmentRecommendation> {

    public InvestmentRecommendationService(BaseRepository<InvestmentRecommendation, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

}
// Low-Code END
