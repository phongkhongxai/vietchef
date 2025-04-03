package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChefTimeSettingsResponse {
    private Long settingId;
    private Integer standardPrepTime;
    private Integer standardCleanupTime;
    private Integer travelBufferPercentage;
    private BigDecimal cookingEfficiencyFactor;
    private Integer minBookingNoticeHours;
    private Integer maxBookingDaysAhead;
    private Integer maxDishesPerSession;
    private Integer maxGuestsPerSession;
    private Integer serviceRadiusKm;
    private Integer maxSessionsPerDay;
} 