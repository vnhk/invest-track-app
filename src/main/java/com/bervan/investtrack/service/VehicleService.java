package com.bervan.investtrack.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Vehicle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VehicleService extends BaseService<UUID, Vehicle> {

    private final VehicleSnapshotService snapshotService;

    protected VehicleService(BaseRepository<Vehicle, UUID> repository, SearchService searchService, VehicleSnapshotService snapshotService) {
        super(repository, searchService);
        this.snapshotService = snapshotService;
    }

    @Transactional
    public void deleteVehicle(UUID vehicleId) {
        Vehicle vehicle = findById(vehicleId);
        vehicle.getSnapshots().forEach(e -> snapshotService.deleteById(e.getId()));
        super.delete(vehicle);
    }

    @Transactional
    public void deleteSnapshot(UUID snapshotId) {
        snapshotService.deleteById(snapshotId);
    }

    @Override
    public Vehicle findById(UUID vehicleId) {
        return repository.findById(vehicleId).orElseThrow(() -> new IllegalArgumentException("Vehicle with id " + vehicleId + " not found"));
    }
}
