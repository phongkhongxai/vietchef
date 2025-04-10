package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DishResponseDto {
    private Long id;
    private ChefDto chef;
    private Long foodTypeId;
    private String name;
    private String description;
    private String cuisineType;
    private String serviceType;
    private BigDecimal cookTime;
    private BigDecimal basePrice;
    private Integer estimatedCookGroup;
    private String imageUrl;
}
