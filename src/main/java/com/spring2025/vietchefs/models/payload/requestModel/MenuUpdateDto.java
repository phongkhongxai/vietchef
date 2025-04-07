package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuUpdateDto {
    private String name;
    private String description;
    private Boolean hasDiscount;
    private Double discountPercentage;
    private BigDecimal totalCookTime;
    private List<MenuItemRequestDto> menuItems;
}

