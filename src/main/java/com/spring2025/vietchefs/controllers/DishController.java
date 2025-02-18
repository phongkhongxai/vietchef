package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.DishRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.services.DishService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dishes")
public class DishController {
    @Autowired
    private DishService dishService;
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> createDish(@Valid @RequestBody DishDto dishDto) {
        DishDto pt = dishService.createDish(dishDto);
        return new ResponseEntity<>(pt, HttpStatus.CREATED);
    }
    @GetMapping
    public DishesResponse getAllDishes(
            @RequestParam(value = "chefId", required = false) Long chefId,
            @RequestParam(value = "foodTypeId", required = false) Long foodTypeId,
                                        @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                       @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                       @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
                                       @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        if (chefId != null) {
            return dishService.getDishesByChef(chefId, pageNo, pageSize, sortBy, sortDir);
        }
        if (foodTypeId != null) {
            return dishService.getDishesByFoodType(foodTypeId, pageNo, pageSize, sortBy, sortDir);
        }
        return dishService.getAllDishes(pageNo, pageSize, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDishById(@PathVariable("id") Long id) {
        DishDto dishDto = dishService.getDishById(id);
        return new ResponseEntity<>(dishDto, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDish(@PathVariable("id") Long id, @Valid @RequestBody DishRequest dishRequest) {
        DishDto bt1 = dishService.updateDish(id, dishRequest);
        return new ResponseEntity<>(bt1, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDish(@PathVariable("id") Long id) {
         String bt1 = dishService.deleteDish(id);
        return new ResponseEntity<>(bt1, HttpStatus.NO_CONTENT);
    }


}
