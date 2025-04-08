package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCriteriaResponse {
    private Long criteriaId;
    private String name;
    private String description;
    private BigDecimal weight;
    private Boolean isActive;
    private Integer displayOrder;
} 