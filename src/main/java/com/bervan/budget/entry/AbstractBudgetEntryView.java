package com.bervan.budget.entry;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.service.BaseService;
import com.bervan.common.view.AbstractBervanTableView;
import com.bervan.logging.JsonLogger;
import java.util.UUID;


// Low-Code START
public abstract class AbstractBudgetEntryView extends AbstractBervanTableView<UUID, BudgetEntry> {
    public static final String ROUTE_NAME = "";

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "investments");
    protected AbstractBudgetEntryView(BaseService<UUID,BudgetEntry> service, BervanViewConfig bervanViewConfig) {
        super(null, service, bervanViewConfig, BudgetEntry.class);
    }

}
// Low-Code END
