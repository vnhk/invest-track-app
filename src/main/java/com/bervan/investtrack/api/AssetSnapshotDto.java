package com.bervan.investtrack.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetSnapshotDto {
    private UUID id;
    private UUID assetId;
    private LocalDate snapshotDate;
    private BigDecimal assetValue;
    private String notes;
}
