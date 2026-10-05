package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.VehicleSnapshot;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VehicleSnapshotRepository extends BaseRepository<VehicleSnapshot, UUID> {
}
