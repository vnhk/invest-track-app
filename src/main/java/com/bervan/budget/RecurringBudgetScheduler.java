package com.bervan.budget;

import com.bervan.logging.JsonLogger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled job to automatically add recurring budget entries at the start of each month.
 * Also provides manual trigger via addRecurringForMonth().
 */
@Component
public class RecurringBudgetScheduler {

    private final JsonLogger log = JsonLogger.getLogger(getClass(), "budget");
    private final BudgetGridService budgetGridService;

    public RecurringBudgetScheduler(BudgetGridService budgetGridService) {
        this.budgetGridService = budgetGridService;
    }

    /**
     * Runs at 00:01 on the 1st day of every month
     * Copies all recurring entries to the new month
     */
    @Scheduled(cron = "0 1 0 1 * *")
    public void addRecurringEntriesForNewMonth() {
        LocalDate today = LocalDate.now();
        log.info("Running scheduled recurring entries copy for month: {}", today.getMonth());

        try {
            budgetGridService.copyRecurringToAnotherDate(today);
            log.info("Successfully copied recurring entries for: {}", today.getMonth());
        } catch (Exception e) {
            log.error("Failed to copy recurring entries: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger to add recurring entries for a specific month
     *
     * @param targetDate the date to copy recurring entries to
     */
    public void addRecurringForMonth(LocalDate targetDate) {
        log.info("Manually adding recurring entries for: {}", targetDate);
        budgetGridService.copyRecurringToAnotherDate(targetDate);
    }
}
