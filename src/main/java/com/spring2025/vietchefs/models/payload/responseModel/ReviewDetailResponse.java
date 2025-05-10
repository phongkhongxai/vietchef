package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.entity.ReviewReply;
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
public class ReviewDetailResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long chefId;
    private String chefName;
    private Long bookingId;
    private BigDecimal rating;
    private String overallExperience;
    private String photos;
    private String response;
    private LocalDateTime chefResponseAt;
    private LocalDateTime createAt;
    
    // Criteria-specific ratings and comments
    private Map<String, BigDecimal> criteriaRatings;
    
    // Replies and reactions
    private List<ReviewReply> replies;
    private Map<String, Long> reactionCounts;
} 