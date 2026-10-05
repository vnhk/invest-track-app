package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.search.model.SortDirection;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.VehicleSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class VehicleSnapshotService extends BaseService<UUID, VehicleSnapshot> {
    protected VehicleSnapshotService(BaseRepository<VehicleSnapshot, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }

    public List<VehicleSnapshot> findByVehicleId(UUID id) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.addCriterion("VEHICLE_ID", VehicleSnapshot.class, "vehicle.id", SearchOperation.EQUALS_OPERATION, id);
        return load(searchRequest, Pageable.ofSize(1000000), "snapshotDate", SortDirection.DESC, null).stream().toList();
    }

    public String exportSnapshotsToCsv(UUID vehicleId) {
        List<VehicleSnapshot> snapshots = findByVehicleId(vehicleId);
        StringBuilder csv = new StringBuilder();

        csv.append("Snapshot Date,Asset Value,Notes\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (VehicleSnapshot snapshot : snapshots) {
            csv.append(snapshot.getSnapshotDate().format(dateFormatter)).append(",");
            csv.append(snapshot.getAssetValue()).append(",");
            csv.append("\"").append(snapshot.getNotes() != null ? snapshot.getNotes().replace("\"", "\"\"") : "").append("\"");
            csv.append("\n");
        }

        return csv.toString();
    }
}
