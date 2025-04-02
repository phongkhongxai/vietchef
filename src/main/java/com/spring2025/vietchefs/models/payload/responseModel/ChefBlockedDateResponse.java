package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ChefBlockedDateResponse {
    private Long blockId;
    private LocalDate blockedDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
} 