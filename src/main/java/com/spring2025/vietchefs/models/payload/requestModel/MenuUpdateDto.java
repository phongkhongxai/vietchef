package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuUpdateDto {
    private String name;
    private String description;
    private Boolean hasDiscount;
    private Double discountPercentage;
    private List<MenuItemRequestDto> menuItems;
}

