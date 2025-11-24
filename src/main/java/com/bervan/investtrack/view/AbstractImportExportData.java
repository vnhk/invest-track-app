package com.bervan.investtrack.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractDataIEView;
import com.bervan.core.model.BervanLogger;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.Wallet;
import com.bervan.investtrack.model.WalletSnapshot;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public abstract class AbstractImportExportData extends AbstractDataIEView<UUID, WalletSnapshot> {
    public static final String ROUTE_NAME = "/invest-track-app/import-export-data";

    public AbstractImportExportData(BaseService<UUID, WalletSnapshot> dataService, BervanViewConfig bervanViewConfig, BervanLogger logger) {
        super(dataService, new InvestTrackPageLayout(ROUTE_NAME, null), bervanViewConfig, logger, WalletSnapshot.class);
    }
}