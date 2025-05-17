package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
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
    @Size(min = 10, max = 2000, message = "Overall experience must be between 10 and 2000 characters")
    private String overallExperience;
    // Replace string-based photos with file uploads
    // private String photos;
    private MultipartFile mainImage;
    private List<MultipartFile> additionalImages;
    private List<Long> imagesToDelete;
    
    @NotEmpty(message = "At least one criteria rating is required")
    private Map<Long, BigDecimal> criteriaRatings;
} 