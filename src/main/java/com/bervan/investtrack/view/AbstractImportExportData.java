package com.bervan.investtrack.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractDataIEView;
import com.bervan.investtrack.InvestTrackPageLayout;
import com.bervan.investtrack.model.WalletSnapshot;
import com.bervan.logging.JsonLogger;

import java.util.UUID;

public abstract class AbstractImportExportData extends AbstractDataIEView<UUID, WalletSnapshot> {
    public static final String ROUTE_NAME = "/invest-track-app/import-export-data";
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");

    public AbstractImportExportData(BaseService<UUID, WalletSnapshot> dataService, BervanViewConfig bervanViewConfig) {
        super(dataService, new InvestTrackPageLayout(ROUTE_NAME, null), bervanViewConfig, WalletSnapshot.class);
    }
}