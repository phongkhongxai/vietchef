package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DistanceResponse {
    private BigDecimal distanceKm; // Khoảng cách (km)
    private BigDecimal durationHours; // Thời gian di chuyển (giờ)
}
