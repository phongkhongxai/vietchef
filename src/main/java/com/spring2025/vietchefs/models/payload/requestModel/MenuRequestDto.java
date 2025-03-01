package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuRequestDto {
    private Long chefId;
    private String name;
    private String description;
    private Boolean hasDiscount;
    private Double discountPercentage;
    private List<MenuItemRequestDto> menuItems;
}

