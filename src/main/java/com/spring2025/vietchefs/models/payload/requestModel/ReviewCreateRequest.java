package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {
    private Long chefId;
    private Long bookingId;
    private String description;
    private String overallExperience;
    private String photos;
    private Map<Long, BigDecimal> criteriaRatings;
    private Map<Long, String> criteriaComments;
} 