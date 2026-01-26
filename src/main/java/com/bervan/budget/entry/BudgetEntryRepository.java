package com.bervan.budget.entry;

import com.bervan.history.model.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// Low-Code START
@Repository
public interface BudgetEntryRepository extends BaseRepository<BudgetEntry, UUID> {

}
// Low-Code END
