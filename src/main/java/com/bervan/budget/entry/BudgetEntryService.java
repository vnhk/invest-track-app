package com.bervan.budget.entry;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import org.springframework.stereotype.Service;
import com.bervan.logging.JsonLogger;

import java.util.List;
import java.util.UUID;

// Low-Code START
@Service
public class BudgetEntryService extends BaseService<UUID, BudgetEntry> {

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    public BudgetEntryService(BaseRepository<BudgetEntry, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

}
// Low-Code END
