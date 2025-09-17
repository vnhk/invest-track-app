package com.bervan.investtrack.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.SearchService;
import com.bervan.common.search.model.SearchOperation;
import com.bervan.common.service.BaseService;
import com.bervan.common.service.EmailService;
import com.bervan.investtrack.model.StockPriceAlert;
import com.bervan.investtrack.model.StockPriceAlertConfig;
import com.bervan.investtrack.service.scrap.ScrapStockPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Scheduled(cron = "0 0/5 9-17 * * MON-FRI")
    public void notifyAboutStockPrices() {
        log.info("notifyAboutStockPrices[scheduled] started");

        SearchRequest request = new SearchRequest();
        request.setAddOwnerCriterion(false);
        request.addCriterion("NOTIFICATION_LEFT_MORE_THAN_0", StockPriceAlert.class, "stockPriceAlertConfig.amountOfNotifications", SearchOperation.GREATER_EQUAL_OPERATION, 1);
        //todo store it in cache
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
        //todo refactor validation logic
        if (alert.getEmails() == null || alert.getEmails().isEmpty()) {
            log.warn("Alert without emails. Skipping.");
            return;
        }

        if (alert.getStockPriceAlertConfig() == null) {
            log.warn("Alert without config. Skipping.");
            return;
        }

        if (alert.getStockPriceAlertConfig().getOperator() == null) {
            log.warn("Alert without operator. Skipping.");
            return;
        }

        if (alert.getStockPriceAlertConfig().getPrice() == null) {
            log.warn("Alert without price. Skipping.");
            return;
        }

        if (alert.getStockPriceAlertConfig().getAmountOfNotifications() == null) {
            log.warn("Alert without amount of notifications. Skipping.");
            return;
        }

        if (alert.getSymbol() == null) {
            log.warn("Alert without symbol. Skipping.");
            return;
        }

        if (alert.getExchange() == null) {
            log.warn("Alert without exchange. Skipping.");
            return;
        }

        if (alert.getStockPriceAlertConfig().getAmountOfNotifications() <= 0) {
            log.error("Alert with 0 or less notifications! Search did not work!");
            return;
        }

        if (alert.getStockPriceAlertConfig().getPreviouslyCheckedDate() != null) {
            LocalDateTime previousCheckedDate = alert.getStockPriceAlertConfig().getPreviouslyCheckedDate();
            LocalDateTime now = LocalDateTime.now();
            Integer checkIntervalMinutes = alert.getStockPriceAlertConfig().getCheckIntervalMinutes();
            if (now.minusMinutes(checkIntervalMinutes).isAfter(previousCheckedDate)) {
                log.debug("Checking alert: {}", alert.getName());
            } else {
                log.debug("Alert was checked recently. Skipping.");
                return;
            }
        }


        for (ScrapStockPriceService scrapStockPriceService : scrapStockPriceServices) {
            if (scrapStockPriceService.supports(alert.getExchange())) {
                Optional<BigDecimal> stockPrice = scrapStockPriceService.getStockPrice(alert.getSymbol());
                alert.getStockPriceAlertConfig().setPreviouslyCheckedDate(LocalDateTime.now());
                if (stockPrice.isPresent()) {
                    BigDecimal actualPrice = stockPrice.get();
                    StockPriceAlertConfig alertConfig = alert.getStockPriceAlertConfig();
                    boolean shouldAlert = false;
                    if (alertConfig.getOperator().equals(">=")) {
                        shouldAlert = actualPrice.compareTo(alertConfig.getPrice()) >= 0;
                    } else if (alertConfig.getOperator().equals("<=")) {
                        shouldAlert = actualPrice.compareTo(alertConfig.getPrice()) <= 0;
                    }

                    if (!shouldAlert) {
                        break;
                    }

                    if (alertConfig.getPreviouslyNotifiedPrice() != null) {
                        //not first notification
                        Integer changeInPercentageToNotifyAgain = alertConfig.getAnotherNotificationEachPercentage();
                        if (changeInPercentageToNotifyAgain != null) {
                            BigDecimal changeInPercentage = actualPrice.subtract(alertConfig.getPreviouslyNotifiedPrice()).divide(alertConfig.getPreviouslyNotifiedPrice(), 2, BigDecimal.ROUND_HALF_UP);
                            if (changeInPercentage.compareTo(BigDecimal.valueOf(changeInPercentageToNotifyAgain)) >= 0) {
                                log.debug("Change in percentage is greater than {}. Sending notification again.", changeInPercentageToNotifyAgain);
                                shouldAlert = true;
                            } else {
                                log.debug("Change in percentage is less than {}. Skipping notification.", changeInPercentageToNotifyAgain);
                                shouldAlert = false;
                            }
                        } else {
                            log.error("No change in percentage to notify again set. Skipping notification.");
                            shouldAlert = false;
                        }
                    }

                    if (!shouldAlert) {
                        break;
                    }

                    for (String email : alert.getEmails()) {
                        String subject = "\uD83D\uDE45\u200D♂\uFE0F\uD83D\uDCB0 Stock Alert: " + alert.getName() + "!";
                        emailService.sendEmail(email, subject, """
                                        Hello!
                                        <br>
                                        Stock alert for: %s.
                                        <br>
                                        <br>
                                        Current price: %s
                                        Threshold: %s
                                        <br>
                                        Please review the situation and take action if necessary.
                                
                                """.formatted(alert.getSymbol(), actualPrice, alert.getStockPriceAlertConfig().getPrice()), "Stock Alert");
                    }

                    alertConfig.setPreviouslyNotifiedDate(LocalDateTime.now());
                    alertConfig.setPreviouslyNotifiedPrice(actualPrice);
                    alertConfig.setAmountOfNotifications(alertConfig.getAmountOfNotifications() - 1);
                    alert.setStockPriceAlertConfig(alertConfig);
                } else {
                    log.warn("Could not get stock price for alert: {}", alert.getName());
                }

            }
        }

        repository.save(alert); //update changes
    }

    public List<String> loadEmails(StockPriceAlert stockPriceAlert) {
        return ((StockPriceAlertRepository) repository).loadAllEmails(stockPriceAlert);
    }

    public StockPriceAlertConfig loadStockPriceAlertConfig(StockPriceAlert stockPriceAlert) {
        return ((StockPriceAlertRepository) repository).loadStockPriceAlertConfig(stockPriceAlert);
    }
}
