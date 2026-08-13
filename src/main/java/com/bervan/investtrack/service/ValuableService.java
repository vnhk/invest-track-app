package com.bervan.investtrack.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Valuable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ValuableService extends BaseService<UUID, Valuable> {

    protected ValuableService(BaseRepository<Valuable, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }
}
