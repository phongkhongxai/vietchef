package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ReviewService {
    Review getReviewById(Long id);
    List<Review> getReviewsByChef(Chef chef);
    Page<Review> getReviewsByChef(Chef chef, Pageable pageable);
    List<Review> getReviewsByUser(User user);
    Review getReviewByBooking(Booking booking);
    
    Review createReview(Review review, Map<Long, BigDecimal> criteriaRatings, Map<Long, String> criteriaComments);
    Review updateReview(Long id, Review updatedReview, Map<Long, BigDecimal> criteriaRatings, Map<Long, String> criteriaComments);
    void deleteReview(Long id);
    
    Review addChefResponse(Long reviewId, String response, User chef);
    
    BigDecimal calculateWeightedRating(Map<Long, BigDecimal> criteriaRatings);
    BigDecimal getAverageRatingForChef(Chef chef);
    long getReviewCountForChef(Chef chef);
    Map<String, Long> getRatingDistributionForChef(Chef chef);
    
    boolean isVerifiedReview(Review review);
    void markReviewAsVerified(Long reviewId);
} 