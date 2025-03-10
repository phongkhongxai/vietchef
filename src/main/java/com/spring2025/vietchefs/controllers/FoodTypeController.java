package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.dto.FoodTypeDto;
import com.spring2025.vietchefs.models.payload.requestModel.DishRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.services.DishService;
import com.spring2025.vietchefs.services.FoodTypeService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/food-types")
public class FoodTypeController {
    @Autowired
    private FoodTypeService foodTypeService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> createFoodType(@Valid @RequestBody FoodTypeDto foodTypeDto) {
        FoodTypeDto pt = foodTypeService.createFoodType(foodTypeDto);
        return new ResponseEntity<>(pt, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<?> getAllFoodTypes(){
        return new ResponseEntity<>(foodTypeService.getAllFoodTypes(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        FoodTypeDto foodTypeDto = foodTypeService.getById(id);
        return new ResponseEntity<>(foodTypeDto, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateType(@PathVariable("id") Long id, @Valid @RequestBody FoodTypeDto foodTypeDto) {
        FoodTypeDto bt1 = foodTypeService.updateType(id, foodTypeDto);
        return new ResponseEntity<>(bt1, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteType(@PathVariable("id") Long id) {
        String bt1 = foodTypeService.deleteType(id);
        return new ResponseEntity<>(bt1, HttpStatus.NO_CONTENT);
    }
}
