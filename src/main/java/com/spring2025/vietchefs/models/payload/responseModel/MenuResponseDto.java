package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponseDto {
    private Long id;
    private String name;
    private ChefResponseDto chef;
    private String description;
    private BigDecimal beforePrice;
    private Boolean hasDiscount;
    private Double discountPercentage;
    private BigDecimal afterPrice;
    private List<MenuItemResponseDto> menuItems;
    private String imageUrl;
}

