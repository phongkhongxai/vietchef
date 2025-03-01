package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class DistanceFeeResponse {
    private BigDecimal distanceKm;
    private BigDecimal durationHours;
    private BigDecimal travelFee;
}
