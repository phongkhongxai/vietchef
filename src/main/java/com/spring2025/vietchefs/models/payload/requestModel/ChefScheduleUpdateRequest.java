package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Data
public class ChefScheduleUpdateRequest {
    @NotNull(message = "Schedule id is required")
    private Long id;

    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}