package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageResponseDto {
    private Long id;
    private String name;
    private int durationDays;
    private BigDecimal discount;
    private int maxDishesPerMeal;
    private int maxGuestCountPerMeal;
}
