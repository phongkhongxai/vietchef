package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Data
public class ChefScheduleRequest {
    @NotNull(message = "Day of week is required")
    private Integer dayOfWeek; // 0-6

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;
}