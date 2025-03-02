package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.DishRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;

public interface DishService {
    DishDto createDish (DishDto dishDto);
    DishDto getDishById(Long id);
    DishDto updateDish (Long id, DishRequest dishRequest);
    String deleteDish(Long id);
    DishesResponse getAllDishes(int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesByChef(Long chefId,int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesNotInMenu(Long menuId,int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesByFoodType(Long foodTypeId,int pageNo, int pageSize, String sortBy, String sortDir);


}
