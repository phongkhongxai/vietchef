package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
public class AvailableTimeSlotRequest {
    private LocalDate sessionDate;
    private Long menuId;
    private List<Long> dishIds;
}
