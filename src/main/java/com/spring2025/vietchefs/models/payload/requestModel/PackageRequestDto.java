package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageRequestDto {
    private String name;
    private int durationDays;
    private BigDecimal discount;
    private int maxDishesPerMeal;
    private int maxGuestCountPerMeal;
}
