package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.common.service.EmailService;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.service.scrap.ScrapStockPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class StockPriceAlertService extends BaseService<UUID, StockPriceAlert> {
    private final EmailService emailService;
    private final List<ScrapStockPriceService> scrapStockPriceServices;

    protected StockPriceAlertService(EmailService emailService, StockPriceAlertRepository repository, SearchService searchService, List<ScrapStockPriceService> scrapStockPriceServices) {
        super(repository, searchService);
        this.emailService = emailService;
        this.scrapStockPriceServices = scrapStockPriceServices;
    }


//    @Scheduled(cron = "0 0/5 * * * *")
    public void notifyAboutStockPrices() {
        log.info("notifyAboutStockPrices[scheduled] started");

        SearchRequest request = new SearchRequest();
        request.setAddOwnerCriterion(false);
        Set<StockPriceAlert> productAlerts = load(request, Pageable.ofSize(100000));

        notifyAboutStockPrices(productAlerts);
        log.info("notifyAboutStockPrices[scheduled] ended");
    }

    public void notifyAboutStockPrices(Collection<StockPriceAlert> alerts) {
        log.info("notifyAboutStockPrices started");
        for (StockPriceAlert alert : alerts) {
            try {
                notifyAboutStockPrices(alert);
            } catch (Exception e) {
                log.error("Could not notify about product prices: alert name = {}", alert.getName(), e);
            }
        }
        log.info("notifyAboutStockPrices ended");
    }

    private void notifyAboutStockPrices(StockPriceAlert alert) {
        if (alert.getEmails() == null || alert.getEmails().isEmpty()) {
            log.warn("Alert without emails. Skipping.");
            return;
        }

        for (ScrapStockPriceService scrapStockPriceService : scrapStockPriceServices) {
            if (scrapStockPriceService.supports(alert.getExchange())) {
                Optional<BigDecimal> stockPrice = scrapStockPriceService.getStockPrice(alert.getSymbol());

            }
        }

    }
}
