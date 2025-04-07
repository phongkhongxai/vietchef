package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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
    private Integer estimatedCookGroup;
    private BigDecimal cookTime;
    private BigDecimal basePrice;
    private MultipartFile file;
}
