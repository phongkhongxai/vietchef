package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class ChefMultipleScheduleRequest {
    @NotNull(message = "Day of week is required")
    private Integer dayOfWeek; // 0-6

    @NotEmpty(message = "At least one schedule is required")
    @Valid
    private List<ScheduleTimeSlot> timeSlots;

    @Data
    public static class ScheduleTimeSlot {
        @NotNull(message = "Start time is required")
        private java.time.LocalTime startTime;

        @NotNull(message = "End time is required")
        private java.time.LocalTime endTime;
    }
} 