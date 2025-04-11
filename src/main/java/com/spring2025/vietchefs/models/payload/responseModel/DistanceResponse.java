package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistanceResponse {
    private BigDecimal distanceKm; // Khoảng cách (km)
    private BigDecimal durationHours; // Thời gian di chuyển (giờ)
}
