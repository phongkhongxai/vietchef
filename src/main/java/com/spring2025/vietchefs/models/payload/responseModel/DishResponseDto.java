package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.dto.FoodTypeDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DishResponseDto {
    private Long id;
    private ChefResponseDto chef;
    private List<FoodTypeDto> foodTypes;
    private String name;
    private String description;
    private String cuisineType;
    private String serviceType;
    private BigDecimal cookTime;
    private BigDecimal basePrice;
    private Integer estimatedCookGroup;
    private String imageUrl;
}
