package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCreateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewDetailResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
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
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewDetailRepository reviewDetailRepository;
    private final ReviewCriteriaService reviewCriteriaService;
    private final ReviewReactionService reviewReactionService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ChefRepository chefRepository;

    @Autowired
    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ReviewDetailRepository reviewDetailRepository,
            ReviewCriteriaService reviewCriteriaService,
            ReviewReactionService reviewReactionService,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ChefRepository chefRepository) {
        this.reviewRepository = reviewRepository;
        this.reviewDetailRepository = reviewDetailRepository;
        this.reviewCriteriaService = reviewCriteriaService;
        this.reviewReactionService = reviewReactionService;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.chefRepository = chefRepository;
    }

    @Override
    public ReviewResponse getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        return mapToResponse(review);
    }

    @Override
    public List<ReviewResponse> getReviewsByChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
        
        return reviewRepository.findByChefAndIsDeletedFalseOrderByCreateAtDesc(chef)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ReviewResponse> getReviewsByChef(Long chefId, Pageable pageable) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
        
        return reviewRepository.findByChefAndIsDeletedFalse(chef, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<ReviewResponse> getReviewsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return reviewRepository.findByUserAndIsDeletedFalseOrderByCreateAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse getReviewByBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        
        Review review = reviewRepository.findByBookingAndIsDeletedFalse(booking)
                .orElse(null);
                
        return review != null ? mapToResponse(review) : null;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Chef chef = chefRepository.findById(request.getChefId())
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + request.getChefId()));
        
        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));
        }
        
        Review review = new Review();
        review.setUser(user);
        review.setChef(chef);
        review.setBooking(booking);
        review.setDescription(request.getDescription());
        review.setOverallExperience(request.getOverallExperience());
        review.setPhotos(request.getPhotos());
        review.setCreateAt(LocalDateTime.now());
        review.setIsDeleted(false);
        review.setIsVerified(false);
        
        // Check if booking exists and is completed
        if (booking != null && "completed".equals(booking.getStatus())) {
            review.setIsVerified(true);
        }
        
        // Calculate weighted rating based on criteria ratings
        BigDecimal calculatedRating = calculateWeightedRating(request.getCriteriaRatings());
        review.setRating(calculatedRating);
        
        // Save the review
        Review savedReview = reviewRepository.save(review);
        
        // Create review details for each criterion
        for (Map.Entry<Long, BigDecimal> entry : request.getCriteriaRatings().entrySet()) {
            ReviewCriteria criteria = mapToCriteriaEntity(reviewCriteriaService.getCriteriaById(entry.getKey()));
            
            ReviewDetail detail = new ReviewDetail();
            detail.setReview(savedReview);
            detail.setCriteria(criteria);
            detail.setRating(entry.getValue());
            
            // Add comment if available
            if (request.getCriteriaComments() != null && request.getCriteriaComments().containsKey(entry.getKey())) {
                detail.setComment(request.getCriteriaComments().get(entry.getKey()));
            }
            
            reviewDetailRepository.save(detail);
        }
        
        return mapToResponse(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long id, ReviewUpdateRequest request, Long userId) {
        Review existingReview = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        // Verify that the user is the one who created the review
        if (!existingReview.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the user who created the review can update it");
        }
        
        // Update basic info
        existingReview.setDescription(request.getDescription());
        existingReview.setOverallExperience(request.getOverallExperience());
        existingReview.setPhotos(request.getPhotos());
        
        // Calculate and update weighted rating
        BigDecimal calculatedRating = calculateWeightedRating(request.getCriteriaRatings());
        existingReview.setRating(calculatedRating);
        
        // Delete existing review details
        List<ReviewDetail> existingDetails = reviewDetailRepository.findByReview(existingReview);
        reviewDetailRepository.deleteAll(existingDetails);
        
        // Create new review details
        for (Map.Entry<Long, BigDecimal> entry : request.getCriteriaRatings().entrySet()) {
            ReviewCriteria criteria = mapToCriteriaEntity(reviewCriteriaService.getCriteriaById(entry.getKey()));
            
            ReviewDetail detail = new ReviewDetail();
            detail.setReview(existingReview);
            detail.setCriteria(criteria);
            detail.setRating(entry.getValue());
            
            // Add comment if available
            if (request.getCriteriaComments() != null && request.getCriteriaComments().containsKey(entry.getKey())) {
                detail.setComment(request.getCriteriaComments().get(entry.getKey()));
            }
            
            reviewDetailRepository.save(detail);
        }
        
        Review updatedReview = reviewRepository.save(existingReview);
        return mapToResponse(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        review.setIsDeleted(true);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public ReviewResponse addChefResponse(Long reviewId, String response, Long chefId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        User chefUser = userRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef user not found with id: " + chefId));
        
        // Verify that the chef is the one being reviewed
        if (!chefUser.getId().equals(review.getChef().getUser().getId())) {
            throw new IllegalArgumentException("Only the chef who is being reviewed can respond to this review");
        }
        
        review.setResponse(response);
        review.setChefResponseAt(LocalDateTime.now());
        
        Review updatedReview = reviewRepository.save(review);
        return mapToResponse(updatedReview);
    }

    @Override
    public BigDecimal calculateWeightedRating(Map<Long, BigDecimal> criteriaRatings) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        
        for (Map.Entry<Long, BigDecimal> entry : criteriaRatings.entrySet()) {
            ReviewCriteria criteria = mapToCriteriaEntity(reviewCriteriaService.getCriteriaById(entry.getKey()));
            
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
    public BigDecimal getAverageRatingForChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
                
        return reviewRepository.findAverageRatingByChef(chef)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public long getReviewCountForChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
                
        return reviewRepository.countByChefAndVerified(chef);
    }

    @Override
    public Map<String, Long> getRatingDistributionForChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
                
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
    public boolean isVerifiedReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        return review.getIsVerified();
    }

    @Override
    @Transactional
    public void markReviewAsVerified(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        review.setIsVerified(true);
        reviewRepository.save(review);
    }
    
    private ReviewResponse mapToResponse(Review review) {
        Map<String, Long> reactionCounts = reviewReactionService.getReactionCountsByReview(review.getId());
        
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getChef().getId(),
                review.getBooking() != null ? review.getBooking().getId() : null,
                review.getRating(),
                review.getDescription(),
                review.getOverallExperience(),
                review.getPhotos(),
                review.getIsVerified(),
                review.getResponse(),
                review.getChefResponseAt(),
                review.getCreateAt(),
                reactionCounts
        );
    }
    
    private ReviewCriteria mapToCriteriaEntity(ReviewCriteriaResponse response) {
        ReviewCriteria criteria = new ReviewCriteria();
        criteria.setCriteriaId(response.getCriteriaId());
        criteria.setName(response.getName());
        criteria.setDescription(response.getDescription());
        criteria.setWeight(response.getWeight());
        criteria.setIsActive(response.getIsActive());
        criteria.setDisplayOrder(response.getDisplayOrder());
        return criteria;
    }
} 