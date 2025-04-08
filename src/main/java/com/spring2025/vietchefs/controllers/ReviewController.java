package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCreateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReactionRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReplyRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewDetailResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import com.spring2025.vietchefs.repositories.BookingRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
import com.spring2025.vietchefs.services.ReviewReplyService;
import com.spring2025.vietchefs.services.ReviewService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewCriteriaService reviewCriteriaService;
    private final ReviewReplyService reviewReplyService;
    private final ReviewReactionService reviewReactionService;
    private final UserRepository userRepository;
    private final ChefRepository chefRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ReviewController(
            ReviewService reviewService,
            ReviewCriteriaService reviewCriteriaService,
            ReviewReplyService reviewReplyService,
            ReviewReactionService reviewReactionService,
            UserRepository userRepository,
            ChefRepository chefRepository,
            BookingRepository bookingRepository,
            ModelMapper modelMapper) {
        this.reviewService = reviewService;
        this.reviewCriteriaService = reviewCriteriaService;
        this.reviewReplyService = reviewReplyService;
        this.reviewReactionService = reviewReactionService;
        this.userRepository = userRepository;
        this.chefRepository = chefRepository;
        this.bookingRepository = bookingRepository;
        this.modelMapper = modelMapper;
    }

    // Get current authenticated user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Get all review criteria
    @GetMapping("/review-criteria")
    public ResponseEntity<List<ReviewCriteria>> getAllCriteria() {
        return ResponseEntity.ok(reviewCriteriaService.getActiveCriteria());
    }

    // Admin: Create review criteria
    @PostMapping("/review-criteria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewCriteria> createCriteria(@RequestBody ReviewCriteria criteria) {
        return new ResponseEntity<>(reviewCriteriaService.createCriteria(criteria), HttpStatus.CREATED);
    }

    // Admin: Update review criteria
    @PutMapping("/review-criteria/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewCriteria> updateCriteria(@PathVariable Long id, @RequestBody ReviewCriteria criteria) {
        return ResponseEntity.ok(reviewCriteriaService.updateCriteria(id, criteria));
    }

    // Get reviews for a chef
    @GetMapping("/reviews/chef/{chefId}")
    public ResponseEntity<Map<String, Object>> getReviewsByChef(
            @PathVariable Long chefId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found with id: " + chefId));
        
        Page<Review> reviewPage = reviewService.getReviewsByChef(
                chef, PageRequest.of(page, size, Sort.by("createAt").descending()));
        
        List<ReviewResponse> reviews = reviewPage.getContent().stream()
                .map(this::convertToReviewResponse)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviews);
        response.put("currentPage", reviewPage.getNumber());
        response.put("totalItems", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("averageRating", reviewService.getAverageRatingForChef(chef));
        response.put("totalReviews", reviewService.getReviewCountForChef(chef));
        response.put("ratingDistribution", reviewService.getRatingDistributionForChef(chef));
        
        return ResponseEntity.ok(response);
    }

    // Get a specific review
    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewDetailResponse> getReviewById(@PathVariable Long id) {
        Review review = reviewService.getReviewById(id);
        return ResponseEntity.ok(convertToReviewDetailResponse(review));
    }

    // Create a new review
    @PostMapping("/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(@RequestBody ReviewCreateRequest request) {
        User currentUser = getCurrentUser();
        
        Chef chef = chefRepository.findById(request.getChefId())
                .orElseThrow(() -> new RuntimeException("Chef not found with id: " + request.getChefId()));
        
        Review review = new Review();
        review.setUser(currentUser);
        review.setChef(chef);
        review.setDescription(request.getDescription());
        review.setOverallExperience(request.getOverallExperience());
        review.setPhotos(request.getPhotos());
        
        // Link to booking if provided
        if (request.getBookingId() != null) {
            Booking booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + request.getBookingId()));
            review.setBooking(booking);
        }
        
        Review savedReview = reviewService.createReview(review, request.getCriteriaRatings(), request.getCriteriaComments());
        return new ResponseEntity<>(convertToReviewResponse(savedReview), HttpStatus.CREATED);
    }

    // Update a review
    @PutMapping("/reviews/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id, @RequestBody ReviewUpdateRequest request) {
        User currentUser = getCurrentUser();
        Review existingReview = reviewService.getReviewById(id);
        
        // Verify the current user is the author of the review
        if (!existingReview.getUser().getId().equals(currentUser.getId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        Review reviewToUpdate = new Review();
        reviewToUpdate.setDescription(request.getDescription());
        reviewToUpdate.setOverallExperience(request.getOverallExperience());
        reviewToUpdate.setPhotos(request.getPhotos());
        
        Review updatedReview = reviewService.updateReview(id, reviewToUpdate, request.getCriteriaRatings(), request.getCriteriaComments());
        return ResponseEntity.ok(convertToReviewResponse(updatedReview));
    }

    // Delete a review
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Review review = reviewService.getReviewById(id);
        
        // Allow deletion if user is the author or an admin
        if (review.getUser().getId().equals(currentUser.getId()) || 
                "ROLE_ADMIN".equals(currentUser.getRole().getRoleName())) {
            reviewService.deleteReview(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    // Chef: Respond to a review
    @PostMapping("/reviews/{id}/response")
    @PreAuthorize("hasRole('CHEF')")
    public ResponseEntity<ReviewResponse> respondToReview(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User currentUser = getCurrentUser();
        Review review = reviewService.getReviewById(id);
        
        // Verify the chef being reviewed is the current user
        if (!review.getChef().getUser().getId().equals(currentUser.getId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        String response = request.get("response");
        Review updatedReview = reviewService.addChefResponse(id, response, currentUser);
        
        return ResponseEntity.ok(convertToReviewResponse(updatedReview));
    }

    // Add a reply to a review
    @PostMapping("/reviews/{id}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addReply(@PathVariable Long id, @RequestBody ReviewReplyRequest request) {
        User currentUser = getCurrentUser();
        Review review = reviewService.getReviewById(id);
        
        ReviewReply reply = reviewReplyService.addReply(review, currentUser, request.getContent());
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    // Add a reaction to a review
    @PostMapping("/reviews/{id}/reaction")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addReaction(@PathVariable Long id, @RequestBody ReviewReactionRequest request) {
        User currentUser = getCurrentUser();
        Review review = reviewService.getReviewById(id);
        
        ReviewReaction reaction = reviewReactionService.addReaction(review, currentUser, request.getReactionType());
        
        Map<String, Object> response = new HashMap<>();
        response.put("reaction", reaction);
        response.put("counts", reviewReactionService.getReactionCountsByReview(review));
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Convert Review to ReviewResponse
    private ReviewResponse convertToReviewResponse(Review review) {
        ReviewResponse response = modelMapper.map(review, ReviewResponse.class);
        
        // Add additional information
        response.setVerified(review.getIsVerified());
        response.setUserName(review.getUser().getUsername());
        
        // Add reaction counts
        Map<String, Long> reactionCounts = reviewReactionService.getReactionCountsByReview(review);
        response.setReactionCounts(reactionCounts);
        
        return response;
    }

    // Convert Review to DetailedReviewResponse
    private ReviewDetailResponse convertToReviewDetailResponse(Review review) {
        ReviewDetailResponse response = modelMapper.map(review, ReviewDetailResponse.class);
        
        // Add criteria details
        response.setCriteriaRatings(review.getReviewDetails().stream()
                .collect(Collectors.toMap(
                        detail -> detail.getCriteria().getName(),
                        ReviewDetail::getRating
                )));
        
        response.setCriteriaComments(review.getReviewDetails().stream()
                .filter(detail -> detail.getComment() != null && !detail.getComment().isEmpty())
                .collect(Collectors.toMap(
                        detail -> detail.getCriteria().getName(),
                        ReviewDetail::getComment
                )));
        
        // Add replies
        response.setReplies(reviewReplyService.getRepliesByReview(review));
        
        // Add reaction counts
        Map<String, Long> reactionCounts = reviewReactionService.getReactionCountsByReview(review);
        response.setReactionCounts(reactionCounts);
        
        return response;
    }
} 