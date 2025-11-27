package com.bervan.investments.recommendation;

import com.bervan.history.model.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// Low-Code START
@Repository
public interface InvestmentRecommendationRepository extends BaseRepository<InvestmentRecommendation, UUID> {

}
// Low-Code END
