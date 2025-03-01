package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingDetailPriceRequestDto {
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private int guestCount;
    private Long menuId;
    private List<Long> extraDishIds;
}
