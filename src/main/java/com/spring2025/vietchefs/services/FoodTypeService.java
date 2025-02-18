package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.FoodTypeDto;

import java.util.List;

public interface FoodTypeService {
    FoodTypeDto createFoodType(FoodTypeDto foodTypeDto);
    FoodTypeDto updateType(Long id, FoodTypeDto foodTypeDto);
    String deleteType(Long id);
    List<FoodTypeDto> getAllFoodTypes();
    FoodTypeDto getById(Long id);
}
