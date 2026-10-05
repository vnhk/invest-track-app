package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.RealEstateSnapshot;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RealEstateSnapshotRepository extends BaseRepository<RealEstateSnapshot, UUID> {
}
