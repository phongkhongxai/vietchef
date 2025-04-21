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
public class ReviewUpdateRequest {
    private String description;
    private String overallExperience;
    // Replace string-based photos with file uploads
    // private String photos;
    private MultipartFile mainImage;
    private List<MultipartFile> additionalImages;
    private List<Long> imagesToDelete;
    private Map<Long, BigDecimal> criteriaRatings;
} 