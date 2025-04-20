package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.DishRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DishService {
    DishDto createDish (DishDto dishDto, MultipartFile imageFile);
    DishResponseDto getDishById(Long id);
    DishDto updateDish (Long id, DishRequest dishRequest);
    String deleteDish(Long id);
    DishesResponse getAllDishes(int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesNearBy(double customerLat, double customerLng, double distance,int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesByChef(Long chefId,int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesNotInMenu(Long menuId,int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesByFoodType(List<Long> foodTypeIds, int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse getDishesByFoodTypeNearBy(List<Long> foodTypeIds,double customerLat, double customerLng,double distance, int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse searchDishByName(String keyword,int pageNo, int pageSize, String sortBy, String sortDir);
    DishesResponse searchDishByNameNearBy(double customerLat, double customerLng, double distance,String keyword,int pageNo, int pageSize, String sortBy, String sortDir);



}
