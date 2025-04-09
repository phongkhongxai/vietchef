package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {
    private Long chefId;
    private Long bookingId;
    private String description;
    private String overallExperience;
    private MultipartFile mainImage;
    private List<MultipartFile> additionalImages;
    private Map<Long, BigDecimal> criteriaRatings;
    private Map<Long, String> criteriaComments;
} 