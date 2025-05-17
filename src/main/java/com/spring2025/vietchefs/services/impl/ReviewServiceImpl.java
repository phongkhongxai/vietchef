package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCreateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewDetailResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.ContentFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ImageRepository imageRepository;
    private final ImageService imageService;
    private final ContentFilterService contentFilterService;

    @Autowired
    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ReviewDetailRepository reviewDetailRepository,
            ReviewCriteriaService reviewCriteriaService,
            ReviewReactionService reviewReactionService,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ChefRepository chefRepository,
            ImageRepository imageRepository,
            ImageService imageService,
            ContentFilterService contentFilterService) {
        this.reviewRepository = reviewRepository;
        this.reviewDetailRepository = reviewDetailRepository;
        this.reviewCriteriaService = reviewCriteriaService;
        this.reviewReactionService = reviewReactionService;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.chefRepository = chefRepository;
        this.imageRepository = imageRepository;
        this.imageService = imageService;
        this.contentFilterService = contentFilterService;
    }

    @Override
    public ReviewResponse getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
                
        if (review.getIsDeleted()) {
            throw new ResourceNotFoundException("Review not found with id: " + id);
        }
        
        return mapToResponse(review);
    }

    // @Override
    // public List<ReviewResponse> getReviewsByChef(Long chefId) {
    //     Chef chef = chefRepository.findById(chefId)
    //             .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
        
    //     return reviewRepository.findByChefAndIsDeletedFalseOrderByCreateAtDesc(chef)
    //             .stream()
    //             .map(this::mapToResponse)
    //             .collect(Collectors.toList());
    // }

    @Override
    public Page<ReviewResponse> getReviewsByChef(Long chefId, Pageable pageable) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
        
        return reviewRepository.findByChefAndIsDeletedFalse(chef, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<ReviewResponse> getFilteredReviewsByChef(Map<String, Object> filters, Pageable pageable) {
        // Extract chef ID from filters map
        Long chefId = (Long) filters.get("chefId");
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
        
        // Extract rating filters
        BigDecimal minRating = filters.containsKey("minRating") ? (BigDecimal) filters.get("minRating") : null;
        BigDecimal maxRating = filters.containsKey("maxRating") ? (BigDecimal) filters.get("maxRating") : null;
        
        // Extract date filters
        LocalDateTime fromDate = filters.containsKey("fromDate") ? (LocalDateTime) filters.get("fromDate") : null;
        LocalDateTime toDate = filters.containsKey("toDate") ? (LocalDateTime) filters.get("toDate") : null;
        
        // Apply filters based on which ones are present
        if (minRating != null && maxRating != null && fromDate != null && toDate != null) {
            return reviewRepository.findByChefAndRatingBetweenAndCreateAtBetweenAndIsDeletedFalse(
                    chef, minRating, maxRating, fromDate, toDate, pageable)
                    .map(this::mapToResponse);
        } else if (minRating != null && maxRating != null) {
            return reviewRepository.findByChefAndRatingBetweenAndIsDeletedFalse(
                    chef, minRating, maxRating, pageable)
                    .map(this::mapToResponse);
        } else if (fromDate != null && toDate != null) {
            return reviewRepository.findByChefAndCreateAtBetweenAndIsDeletedFalse(
                    chef, fromDate, toDate, pageable)
                    .map(this::mapToResponse);
        } else if (minRating != null) {
            return reviewRepository.findByChefAndRatingGreaterThanEqualAndIsDeletedFalse(
                    chef, minRating, pageable)
                    .map(this::mapToResponse);
        } else if (maxRating != null) {
            return reviewRepository.findByChefAndRatingLessThanEqualAndIsDeletedFalse(
                    chef, maxRating, pageable)
                    .map(this::mapToResponse);
        } else if (fromDate != null) {
            return reviewRepository.findByChefAndCreateAtGreaterThanEqualAndIsDeletedFalse(
                    chef, fromDate, pageable)
                    .map(this::mapToResponse);
        } else if (toDate != null) {
            return reviewRepository.findByChefAndCreateAtLessThanEqualAndIsDeletedFalse(
                    chef, toDate, pageable)
                    .map(this::mapToResponse);
        } else {
            // Default to unfiltered results
            return reviewRepository.findByChefAndIsDeletedFalse(chef, pageable)
                    .map(this::mapToResponse);
        }
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
        
        // Phục vụ cho BR-46: Kiểm tra xem booking đã có review chưa 
        // Mỗi buổi đặt chỉ cho phép gửi một đánh giá duy nhất từ khách hàng
        Review review = reviewRepository.findByBookingAndIsDeletedFalse(booking)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for booking: " + bookingId));
                
        return mapToResponse(review);
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
            
            // BR-46: Kiểm tra xem buổi đặt này đã có đánh giá chưa
            Optional<Review> existingReview = reviewRepository.findByBookingAndIsDeletedFalse(booking);
            if (existingReview.isPresent()) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                    "BR-46: Mỗi buổi đặt chỉ cho phép gửi một đánh giá duy nhất từ khách hàng.");
            }
        } 
        
        // Filter the review description for profanity
        String filteredOverallExperience = contentFilterService.filterText(request.getOverallExperience());
        
        Review review = new Review();
        review.setUser(user);
        review.setChef(chef);
        review.setBooking(booking);
        review.setOverallExperience(filteredOverallExperience);
        review.setCreateAt(LocalDateTime.now());
        review.setIsDeleted(false);
        
        // Calculate weighted rating based on criteria ratings
        BigDecimal calculatedRating = calculateWeightedRating(request.getCriteriaRatings());
        review.setRating(calculatedRating);
        
        // Save the review first to get ID for image uploads
        Review savedReview = reviewRepository.save(review);
        
        // Handle main image upload
        try {
            if (request.getMainImage() != null && !request.getMainImage().isEmpty()) {
                String imageUrl = imageService.uploadImage(request.getMainImage(), savedReview.getId(), "REVIEW");
                savedReview.setImageUrl(imageUrl);
                reviewRepository.save(savedReview);
            }
            
            // Handle additional images
            if (request.getAdditionalImages() != null && !request.getAdditionalImages().isEmpty()) {
                for (MultipartFile imageFile : request.getAdditionalImages()) {
                    if (imageFile != null && !imageFile.isEmpty()) {
                        imageService.uploadImage(imageFile, savedReview.getId(), "REVIEW");
                    }
                }
            }
        } catch (IOException e) {
            throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload review images: " + e.getMessage());
        }
        
        // Create review details for each criterion
        for (Map.Entry<Long, BigDecimal> entry : request.getCriteriaRatings().entrySet()) {
            ReviewCriteria criteria = mapToCriteriaEntity(reviewCriteriaService.getCriteriaById(entry.getKey()));
            
            ReviewDetail detail = new ReviewDetail();
            detail.setReview(savedReview);
            detail.setCriteria(criteria);
            detail.setRating(entry.getValue());
            
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
        
        // BR-46: Ghi chú - Việc sửa đổi review không thể thay đổi booking
        // Booking được gán khi tạo review và không thể thay đổi sau đó
        
        // Filter the review description for profanity
        String filteredDescription = contentFilterService.filterText(request.getOverallExperience());
        
        // Update review details
        existingReview.setOverallExperience(filteredDescription);
        
        // Handle main image upload
        try {
            if (request.getMainImage() != null && !request.getMainImage().isEmpty()) {
                String imageUrl = imageService.uploadImage(request.getMainImage(), existingReview.getId(), "REVIEW");
                existingReview.setImageUrl(imageUrl);
            }
            
            // Handle additional images
            if (request.getAdditionalImages() != null && !request.getAdditionalImages().isEmpty()) {
                for (MultipartFile imageFile : request.getAdditionalImages()) {
                    if (imageFile != null && !imageFile.isEmpty()) {
                        imageService.uploadImage(imageFile, existingReview.getId(), "REVIEW");
                    }
                }
            }
            
            // Delete images if requested
            if (request.getImagesToDelete() != null && !request.getImagesToDelete().isEmpty()) {
                for (Long imageId : request.getImagesToDelete()) {
                    Image image = imageRepository.findById(imageId)
                            .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));
                    
                    // Verify that this image belongs to the review
                    if ("REVIEW".equals(image.getEntityType()) && existingReview.getId().equals(image.getEntityId())) {
                        imageRepository.delete(image);
                    } else {
                        throw new IllegalArgumentException("Cannot delete image that does not belong to this review");
                    }
                }
            }
        } catch (IOException e) {
            throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload review images: " + e.getMessage());
        }
        
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
        
        // Verify the chef is the one being reviewed
        if (!review.getChef().getUser().getId().equals(chefId)) {
            throw new IllegalArgumentException("Only the chef being reviewed can respond to the review");
        }
        
        // Filter the chef response for profanity
        String filteredResponse = contentFilterService.filterText(response);
        
        review.setResponse(filteredResponse);
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
            
            BigDecimal rating = entry.getValue();
            
            // Skip criteria with rating 0 (not rated by user)
            if (rating.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            
            BigDecimal weight = criteria.getWeight();
            
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
                
        BigDecimal avgRating = reviewRepository.findAverageRatingByChef(chef)
                .orElse(BigDecimal.ZERO);
        
        // Làm tròn đến 2 chữ số thập phân
        return avgRating.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public long getReviewCountForChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found with id: " + chefId));
                
        return reviewRepository.countByChef(chef);
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
        distribution.put("1-star", reviewRepository.countByChef(chef) - 
                                  reviewRepository.countByChefAndRatingGreaterThanEqual(chef, new BigDecimal("1.5")));
        
        return distribution;
    }
    
    private ReviewResponse mapToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUser().getId());
        response.setUserName(review.getUser().getFullName());
        response.setUserAvatar(review.getUser().getAvatarUrl());
        response.setChefId(review.getChef().getId());
        response.setBookingId(review.getBooking() != null ? review.getBooking().getId() : null);
        response.setRating(review.getRating());
        response.setOverallExperience(review.getOverallExperience());
        response.setMainImageUrl(review.getImageUrl());
        
        // Get all additional images for this review
        List<Image> reviewImages = imageRepository.findByEntityTypeAndEntityId("REVIEW", review.getId());
        List<String> additionalImageUrls = reviewImages.stream()
                .map(Image::getImageUrl)
                .collect(Collectors.toList());
        response.setAdditionalImageUrls(additionalImageUrls);
        
        response.setResponse(review.getResponse());
        response.setChefResponseAt(review.getChefResponseAt());
        response.setCreateAt(review.getCreateAt());
        
        // Add reaction counts
        Map<String, Long> reactionCounts = reviewReactionService.getReactionCountsByReview(review.getId());
        response.setReactionCounts(reactionCounts);
        
        return response;
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