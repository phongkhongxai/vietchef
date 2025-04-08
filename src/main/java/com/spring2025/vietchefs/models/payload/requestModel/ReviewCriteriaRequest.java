package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCriteriaRequest {
    private String name;
    private String description;
    private BigDecimal weight;
    private Boolean isActive;
    private Integer displayOrder;
} 