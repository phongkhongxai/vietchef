package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long chefId;
    private Long bookingId;
    private BigDecimal rating;
    private String overallExperience;
    private String mainImageUrl;
    private List<String> additionalImageUrls;
    private String response;
    private LocalDateTime chefResponseAt;
    private LocalDateTime createAt;
    private Map<String, Long> reactionCounts;
} 