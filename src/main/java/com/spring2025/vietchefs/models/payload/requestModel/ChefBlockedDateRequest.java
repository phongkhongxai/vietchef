package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ChefBlockedDateRequest {
    @NotNull(message = "Blocked date is required")
    private LocalDate blockedDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private String reason;
} 