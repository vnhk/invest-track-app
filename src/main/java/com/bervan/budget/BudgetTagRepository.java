package com.bervan.budget;

import com.bervan.history.model.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// Low-Code START
@Repository
public interface BudgetTagRepository extends BaseRepository<BudgetEntryTag, UUID> {

}
// Low-Code END
