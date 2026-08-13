package com.bervan.investtrack.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Vehicle;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VehicleService extends BaseService<UUID, Vehicle> {

    protected VehicleService(BaseRepository<Vehicle, UUID> repository, SearchService searchService) {
        super(repository, searchService);
    }
}
