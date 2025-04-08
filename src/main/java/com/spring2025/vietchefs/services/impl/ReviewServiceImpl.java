package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewDetailRepository reviewDetailRepository;
    private final ReviewCriteriaService reviewCriteriaService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ChefRepository chefRepository;

    @Autowired
    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ReviewDetailRepository reviewDetailRepository,
            ReviewCriteriaService reviewCriteriaService,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ChefRepository chefRepository) {
        this.reviewRepository = reviewRepository;
        this.reviewDetailRepository = reviewDetailRepository;
        this.reviewCriteriaService = reviewCriteriaService;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.chefRepository = chefRepository;
    }

    @Override
    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
    }

    @Override
    public List<Review> getReviewsByChef(Chef chef) {
        return reviewRepository.findByChefAndIsDeletedFalseOrderByCreateAtDesc(chef);
    }

    @Override
    public Page<Review> getReviewsByChef(Chef chef, Pageable pageable) {
        return reviewRepository.findByChefAndIsDeletedFalse(chef, pageable);
    }

    @Override
    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByUserAndIsDeletedFalseOrderByCreateAtDesc(user);
    }

    @Override
    public Review getReviewByBooking(Booking booking) {
        return reviewRepository.findByBookingAndIsDeletedFalse(booking)
                .orElse(null);
    }

    @Override
    @Transactional
    public Review createReview(Review review, Map<Long, BigDecimal> criteriaRatings, Map<Long, String> criteriaComments) {
        // Set creation timestamp
        review.setCreateAt(LocalDateTime.now());
        review.setIsDeleted(false);
        
        // Check if booking exists and is completed
        if (review.getBooking() != null) {
            Booking booking = bookingRepository.findById(review.getBooking().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + review.getBooking().getId()));
            
            if ("completed".equals(booking.getStatus())) {
                review.setIsVerified(true);
            }
        }
        
        // Calculate weighted rating based on criteria ratings
        BigDecimal calculatedRating = calculateWeightedRating(criteriaRatings);
        review.setRating(calculatedRating);
        
        // Save the review
        Review savedReview = reviewRepository.save(review);
        
        // Create review details for each criterion
        for (Map.Entry<Long, BigDecimal> entry : criteriaRatings.entrySet()) {
            ReviewCriteria criteria = reviewCriteriaService.getCriteriaById(entry.getKey());
            
            ReviewDetail detail = new ReviewDetail();
            detail.setReview(savedReview);
            detail.setCriteria(criteria);
            detail.setRating(entry.getValue());
            
            // Add comment if available
            if (criteriaComments != null && criteriaComments.containsKey(entry.getKey())) {
                detail.setComment(criteriaComments.get(entry.getKey()));
            }
            
            reviewDetailRepository.save(detail);
        }
        
        return savedReview;
    }

    @Override
    @Transactional
    public Review updateReview(Long id, Review updatedReview, Map<Long, BigDecimal> criteriaRatings, Map<Long, String> criteriaComments) {
        Review existingReview = getReviewById(id);
        
        // Update basic info
        existingReview.setDescription(updatedReview.getDescription());
        existingReview.setOverallExperience(updatedReview.getOverallExperience());
        existingReview.setPhotos(updatedReview.getPhotos());
        
        // Calculate and update weighted rating
        BigDecimal calculatedRating = calculateWeightedRating(criteriaRatings);
        existingReview.setRating(calculatedRating);
        
        // Delete existing review details
        List<ReviewDetail> existingDetails = reviewDetailRepository.findByReview(existingReview);
        reviewDetailRepository.deleteAll(existingDetails);
        
        // Create new review details
        for (Map.Entry<Long, BigDecimal> entry : criteriaRatings.entrySet()) {
            ReviewCriteria criteria = reviewCriteriaService.getCriteriaById(entry.getKey());
            
            ReviewDetail detail = new ReviewDetail();
            detail.setReview(existingReview);
            detail.setCriteria(criteria);
            detail.setRating(entry.getValue());
            
            // Add comment if available
            if (criteriaComments != null && criteriaComments.containsKey(entry.getKey())) {
                detail.setComment(criteriaComments.get(entry.getKey()));
            }
            
            reviewDetailRepository.save(detail);
        }
        
        return reviewRepository.save(existingReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Review review = getReviewById(id);
        review.setIsDeleted(true);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public Review addChefResponse(Long reviewId, String response, User chef) {
        Review review = getReviewById(reviewId);
        
        // Verify that the chef is the one being reviewed
        if (!chef.getId().equals(review.getChef().getUser().getId())) {
            throw new IllegalArgumentException("Only the chef who is being reviewed can respond to this review");
        }
        
        review.setResponse(response);
        review.setChefResponseAt(LocalDateTime.now());
        
        return reviewRepository.save(review);
    }

    @Override
    public BigDecimal calculateWeightedRating(Map<Long, BigDecimal> criteriaRatings) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (Map.Entry<Long, BigDecimal> entry : criteriaRatings.entrySet()) {
            ReviewCriteria criteria = reviewCriteriaService.getCriteriaById(entry.getKey());
            
            // Skip inactive criteria
            if (!criteria.getIsActive()) {
                continue;
            }
            
            BigDecimal weight = criteria.getWeight();
            BigDecimal rating = entry.getValue();
            
            weightedSum = weightedSum.add(rating.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        
        // Avoid division by zero
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getAverageRatingForChef(Chef chef) {
        return reviewRepository.findAverageRatingByChef(chef)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public long getReviewCountForChef(Chef chef) {
        return reviewRepository.countByChefAndVerified(chef);
    }

    @Override
    public Map<String, Long> getRatingDistributionForChef(Chef chef) {
        Map<String, Long> distribution = new HashMap<>();
        
        distribution.put("5-star", reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("4.5")));
        distribution.put("4-star", reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("3.5")) - 
                                  reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("4.5")));
        distribution.put("3-star", reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("2.5")) - 
                                  reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("3.5")));
        distribution.put("2-star", reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("1.5")) - 
                                  reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("2.5")));
        distribution.put("1-star", reviewRepository.countByChefAndVerified(chef) - 
                                  reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("1.5")));
        
        return distribution;
    }

    @Override
    public boolean isVerifiedReview(Review review) {
        return review.getIsVerified();
    }

    @Override
    @Transactional
    public void markReviewAsVerified(Long reviewId) {
        Review review = getReviewById(reviewId);
        review.setIsVerified(true);
        reviewRepository.save(review);
    }
} 