package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ReviewCreateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ReviewService {
    ReviewResponse getReviewById(Long id);
    // List<ReviewResponse> getReviewsByChef(Long chefId);
    Page<ReviewResponse> getReviewsByChef(Long chefId, Pageable pageable);
    List<ReviewResponse> getReviewsByUser(Long userId);
    ReviewResponse getReviewByBooking(Long bookingId);
    
    ReviewResponse createReview(ReviewCreateRequest request, Long userId);
    ReviewResponse updateReview(Long id, ReviewUpdateRequest request, Long userId);
    void deleteReview(Long id);
    ReviewResponse addChefResponse(Long reviewId, String response, Long chefId);
    
    BigDecimal calculateWeightedRating(Map<Long, BigDecimal> criteriaRatings);
    BigDecimal getAverageRatingForChef(Long chefId);
    long getReviewCountForChef(Long chefId);
    Map<String, Long> getRatingDistributionForChef(Long chefId);
} 