package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.ValuableSnapshot;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ValuableSnapshotRepository extends BaseRepository<ValuableSnapshot, UUID> {
}
