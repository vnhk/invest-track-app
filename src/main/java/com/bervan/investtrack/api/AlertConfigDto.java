package com.bervan.investtrack.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigDto {
    private UUID id;
    private BigDecimal price;
    private String operator;
    private Integer amountOfNotifications;
    private Integer checkIntervalMinutes;
    private Integer anotherNotificationEachPercentage;
    private LocalDateTime previouslyNotifiedDate;
    private BigDecimal previouslyNotifiedPrice;
}
