package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DishRequest {
    private Long chefId;
    private Long foodTypeId;
    private String name;
    private String description;
    private String cuisineType;
    private String serviceType;
    private LocalTime cookTime;
    private Integer servingSize;
    private String imageUrl;
    private LocalTime preparationTime;
}
