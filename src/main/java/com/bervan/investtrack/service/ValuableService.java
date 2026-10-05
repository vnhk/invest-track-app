package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Valuable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class ValuableService extends BaseService<UUID, Valuable> {

    private final ValuableSnapshotService snapshotService;

    protected ValuableService(BaseRepository<Valuable, UUID> repository, SearchService searchService, ValuableSnapshotService snapshotService) {
        super(repository, searchService);
        this.snapshotService = snapshotService;
    }

    @Override
    public Valuable save(Valuable data) {
        if (data.getId() == null && data.getName() != null) {
            String name = data.getName();
            Set<Valuable> loaded = getValuableByName(name);
            if (!loaded.isEmpty()) {
                throw new IllegalArgumentException("Valuable with name " + name + " already exists");
            }
        }
        return super.save(data);
    }

    public Set<Valuable> getValuableByName(String name) {
        SearchRequest request = new SearchRequest();
        request.addCriterion("VALUABLE_NAME", Valuable.class, "name", SearchOperation.EQUALS_OPERATION, name);
        return load(request, Pageable.ofSize(1));
    }

    @Transactional
    public void deleteValuable(UUID valuableId) {
        Valuable valuable = findById(valuableId);
        valuable.getSnapshots().forEach(e -> snapshotService.deleteById(e.getId()));
        super.delete(valuable);
    }

    @Transactional
    public void deleteSnapshot(UUID snapshotId) {
        snapshotService.deleteById(snapshotId);
    }

    @Override
    public Valuable findById(UUID valuableId) {
        return repository.findById(valuableId).orElseThrow(() -> new IllegalArgumentException("Valuable with id " + valuableId + " not found"));
    }
}
