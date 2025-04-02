package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class ChefTimeSettingsRequest {
    @Min(value = 15, message = "Standard preparation time should be at least 15 minutes")
    @Max(value = 120, message = "Standard preparation time should not exceed 120 minutes")
    private Integer standardPrepTime;
    
    @Min(value = 15, message = "Standard cleanup time should be at least 15 minutes")
    @Max(value = 120, message = "Standard cleanup time should not exceed 120 minutes")
    private Integer standardCleanupTime;
    
    @Min(value = 10, message = "Travel buffer percentage should be at least 10%")
    @Max(value = 100, message = "Travel buffer percentage should not exceed 100%")
    private Integer travelBufferPercentage;
    
    @DecimalMin(value = "0.5", message = "Cooking efficiency factor should be at least 0.5")
    @DecimalMax(value = "1.0", message = "Cooking efficiency factor should not exceed 1.0")
    private BigDecimal cookingEfficiencyFactor;
    
    @Min(value = 1, message = "Minimum booking notice hours should be at least 1 hour")
    @Max(value = 168, message = "Minimum booking notice hours should not exceed 168 hours (7 days)")
    private Integer minBookingNoticeHours;
    
    @Min(value = 7, message = "Maximum booking days ahead should be at least 7 days")
    @Max(value = 180, message = "Maximum booking days ahead should not exceed 180 days (6 months)")
    private Integer maxBookingDaysAhead;
    
    @Min(value = 1, message = "Maximum dishes per session should be at least 1")
    @Max(value = 15, message = "Maximum dishes per session should not exceed 15")
    private Integer maxDishesPerSession;
    
    @Min(value = 1, message = "Maximum guests per session should be at least 1")
    @Max(value = 30, message = "Maximum guests per session should not exceed 30")
    private Integer maxGuestsPerSession;
    
    @Min(value = 1, message = "Service radius should be at least 1 km")
    @Max(value = 100, message = "Service radius should not exceed 100 km")
    private Integer serviceRadiusKm;
    
    @Min(value = 1, message = "Maximum sessions per day should be at least 1")
    @Max(value = 5, message = "Maximum sessions per day should not exceed 5")
    private Integer maxSessionsPerDay;
} 