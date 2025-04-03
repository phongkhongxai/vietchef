package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ChefBlockedDateUpdateRequest {
    @NotNull(message = "Block id is required")
    private Long blockId;

    private LocalDate blockedDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
} 