package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponseDto {
    private Long dishId;
    private String dishName;
    private String dishImageUrl;
}

