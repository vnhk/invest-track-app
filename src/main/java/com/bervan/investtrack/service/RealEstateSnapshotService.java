package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.RealEstateSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class RealEstateSnapshotService extends BaseService<UUID, RealEstateSnapshot> {
    protected RealEstateSnapshotService(BaseRepository<RealEstateSnapshot, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

    public List<RealEstateSnapshot> findByRealEstateId(UUID id) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.addCriterion("REAL_ESTATE_ID", RealEstateSnapshot.class, "realEstate.id", SearchOperation.EQUALS_OPERATION, id);
        return load(searchRequest, Pageable.ofSize(1000000), "snapshotDate", SortDirection.DESC, null).stream().toList();
    }

    public String exportSnapshotsToCsv(UUID realEstateId) {
        List<RealEstateSnapshot> snapshots = findByRealEstateId(realEstateId);
        StringBuilder csv = new StringBuilder();

        csv.append("Snapshot Date,Asset Value,Notes\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (RealEstateSnapshot snapshot : snapshots) {
            csv.append(snapshot.getSnapshotDate().format(dateFormatter)).append(",");
            csv.append(snapshot.getAssetValue()).append(",");
            csv.append("\"").append(snapshot.getNotes() != null ? snapshot.getNotes().replace("\"", "\"\"") : "").append("\"");
            csv.append("\n");
        }

        return csv.toString();
    }
}
