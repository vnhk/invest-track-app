package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.RealEstate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class RealEstateService extends BaseService<UUID, RealEstate> {

    protected RealEstateService(BaseRepository<RealEstate, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

    @Override
    public RealEstate save(RealEstate data) {
        if (data.getId() == null) {
            String name = data.getName();
            Set<RealEstate> loaded = getRealEstateByName(name);
            if (!loaded.isEmpty()) {
                throw new IllegalArgumentException("RealEstate with name " + name + " already exists");
            }

        }
        return super.save(data);
    }

    public Set<RealEstate> getRealEstateByName(String name) {
        SearchRequest request = new SearchRequest();
        request.addCriterion("RealEstate_NAME", RealEstate.class, "name", SearchOperation.EQUALS_OPERATION, name);
        return load(request, Pageable.ofSize(1));
    }
}
