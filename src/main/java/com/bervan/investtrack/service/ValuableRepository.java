package com.bervan.investtrack.service;

import com.bervan.history.model.BaseRepository;
import com.bervan.investtrack.model.Valuable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ValuableRepository extends BaseRepository<Valuable, UUID> {

}
