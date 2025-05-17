package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
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
public class ReviewCreateRequest {
    @NotNull(message = "Chef ID is required")
    private Long chefId;
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
    
    @NotNull(message = "Overall experience description is required")
    @Size(min = 10, max = 2000, message = "Overall experience must be between 10 and 2000 characters")
    private String overallExperience;
    
    private MultipartFile mainImage;
    
    private List<MultipartFile> additionalImages;
    
    @NotEmpty(message = "At least one criteria rating is required")
    private Map<Long, BigDecimal> criteriaRatings;
} 