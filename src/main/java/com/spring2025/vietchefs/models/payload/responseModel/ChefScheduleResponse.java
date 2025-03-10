package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Data;
import java.time.LocalTime;

@Data
public class ChefScheduleResponse {
    private Long id;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
