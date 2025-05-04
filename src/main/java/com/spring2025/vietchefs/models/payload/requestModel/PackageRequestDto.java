package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageRequestDto {
    @NotBlank(message = "Name must not be blank")
    private String name;

    @Min(value = 1, message = "Duration must be greater than 0")
    private int durationDays;

    @NotNull(message = "Discount must not be null")
    private BigDecimal discount;

    @Min(value = 1, message = "Max dishes per meal must be greater than 0")
    private int maxDishesPerMeal;

    @Min(value = 1, message = "Max guest count per meal must be greater than 0")
    private int maxGuestCountPerMeal;
}
