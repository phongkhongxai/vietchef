package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCreateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCriteriaRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReactionRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReplyRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewDetailResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReactionResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReplyResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import com.spring2025.vietchefs.repositories.BookingRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.ContentFilterService;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
import com.spring2025.vietchefs.services.ReviewReplyService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.RoleService;
import com.spring2025.vietchefs.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewCriteriaService reviewCriteriaService;
    private final ReviewReplyService reviewReplyService;
    private final ReviewReactionService reviewReactionService;
    private final UserService userService;
    private final ChefService chefService;
    private final RoleService roleService;
    private final ContentFilterService contentFilterService;

    @Autowired
    public ReviewController(
            ReviewService reviewService,
            ReviewCriteriaService reviewCriteriaService,
            ReviewReplyService reviewReplyService,
            ReviewReactionService reviewReactionService,
            UserService userService,
            ChefService chefService,
            RoleService roleService,
            ContentFilterService contentFilterService) {
        this.reviewService = reviewService;
        this.reviewCriteriaService = reviewCriteriaService;
        this.reviewReplyService = reviewReplyService;
        this.reviewReactionService = reviewReactionService;
        this.userService = userService;
        this.chefService = chefService;
        this.roleService = roleService;
        this.contentFilterService = contentFilterService;
    }

    // Get current authenticated user
    private UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userService.getProfileUserByUsernameOrEmail(currentUsername, currentUsername);
    }
 
    // Get all review criteria
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get all review criteria",
            description = "Returns a list of all active review criteria"
    )
    @GetMapping("/review-criteria")
    public ResponseEntity<List<ReviewCriteriaResponse>> getAllCriteria() {
        return ResponseEntity.ok(reviewCriteriaService.getActiveCriteria());
    }

    // Admin: Create review criteria
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Create review criteria",
            description = "Creates a new review criteria. Only admin can use this API."
    )
    @PostMapping("/review-criteria")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ReviewCriteriaResponse> createCriteria(@RequestBody ReviewCriteriaRequest request) {
        return new ResponseEntity<>(reviewCriteriaService.createCriteria(request), HttpStatus.CREATED);
    }

    // Admin: Update review criteria
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Update review criteria",
            description = "Updates an existing review criteria. Only admin can use this API."
    )
    @PutMapping("/review-criteria/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ReviewCriteriaResponse> updateCriteria(@PathVariable Long id, @RequestBody ReviewCriteriaRequest request) {
        return ResponseEntity.ok(reviewCriteriaService.updateCriteria(id, request));
    }

    // Get reviews for a chef
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get chef reviews",
            description = "Returns a paginated list of reviews for a specific chef with filtering options"
    )
    @GetMapping("/reviews/chef/{chefId}")
    public ResponseEntity<Map<String, Object>> getReviewsByChef(
            @PathVariable Long chefId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) BigDecimal maxRating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        // Determine the sort specification based on the sort parameter
        Pageable pageable;
        switch (sort.toLowerCase()) {
            case "oldest":
                pageable = PageRequest.of(page, size, Sort.by("createAt").ascending());
                break;
            case "highest-rating":
                pageable = PageRequest.of(page, size, Sort.by("rating").descending());
                break;
            case "lowest-rating":
                pageable = PageRequest.of(page, size, Sort.by("rating").ascending());
                break;
            case "newest":
            default:
                pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
                break;
        }
        
        // Create filter map
        Map<String, Object> filters = new HashMap<>();
        filters.put("chefId", chefId);
        
        if (minRating != null) {
            filters.put("minRating", minRating);
        }
        
        if (maxRating != null) {
            filters.put("maxRating", maxRating);
        }
        
        if (fromDate != null) {
            filters.put("fromDate", fromDate.atStartOfDay());
        }
        
        if (toDate != null) {
            filters.put("toDate", toDate.plusDays(1).atStartOfDay());
        }
        
        // Get filtered reviews
        Page<ReviewResponse> reviewPage = reviewService.getFilteredReviewsByChef(filters, pageable);
        
        // Add replies to each review
        List<ReviewResponse> reviewsWithReplies = reviewPage.getContent();
        for (ReviewResponse review : reviewsWithReplies) {
            List<ReviewReplyResponse> replies = reviewReplyService.getRepliesByReview(review.getId());
            review.setReplies(replies);
        }
        
        long reviewCount = reviewService.getReviewCountForChef(chefId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviewsWithReplies);
        response.put("currentPage", reviewPage.getNumber());
        response.put("totalReviews", reviewCount);
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("averageRating", reviewService.getAverageRatingForChef(chefId));
        response.put("ratingDistribution", reviewService.getRatingDistributionForChef(chefId));
        response.put("filters", filters);
        
        return ResponseEntity.ok(response);
    }

    // Get a specific review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get review by ID",
            description = "Returns a specific review by its ID, including replies"
    )
    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        ReviewResponse review = reviewService.getReviewById(id);
        
        // Get replies for this review
        List<ReviewReplyResponse> replies = reviewReplyService.getRepliesByReview(id);
        review.setReplies(replies);
        
        return ResponseEntity.ok(review);
    }

    // Get a specific review for a booking
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get review by booking ID",
            description = "Returns a review for a specific booking, if it exists"
    )
    @GetMapping("/reviews/booking/{bookingId}")
    public ResponseEntity<?> getReviewByBooking(@PathVariable Long bookingId) {
        ReviewResponse review = reviewService.getReviewByBooking(bookingId);
        
        if (review != null) {
            // Add replies to the review
            List<ReviewReplyResponse> replies = reviewReplyService.getRepliesByReview(review.getId());
            review.setReplies(replies);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasReview", review != null);
        response.put("review", review);
        response.put("message", review != null ? 
            "BR-46: This booking already has a review." : 
            "This booking doesn't have a review yet.");
        
        return ResponseEntity.ok(response);
    }

    // Get all replies for a specific review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get all replies for a review",
            description = "Returns a paginated list of all replies for the specified review"
    )
    @GetMapping("/reviews/{id}/replies")
    public ResponseEntity<Map<String, Object>> getRepliesByReview(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        
        // Determine sort specification
        Sort.Direction direction;
        String property;
        
        switch (sort.toLowerCase()) {
            case "oldest":
                direction = Sort.Direction.ASC;
                property = "createdAt";
                break;
            case "newest":
            default:
                direction = Sort.Direction.DESC;
                property = "createdAt";
                break;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        
        // Get paginated replies
        Page<ReviewReplyResponse> repliesPage = reviewReplyService.getRepliesByReviewPaginated(id, pageable);
        
        // Create response with pagination metadata
        Map<String, Object> response = new HashMap<>();
        response.put("replies", repliesPage.getContent());
        response.put("currentPage", repliesPage.getNumber());
        response.put("totalReplies", repliesPage.getTotalElements());
        response.put("totalPages", repliesPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    // Create a new review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Create review",
            description = "Creates a new review for a booking"
    )
    @PostMapping("/reviews")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        UserDto currentUser = getCurrentUser();
        ReviewResponse savedReview = reviewService.createReview(request, currentUser.getId());
        return new ResponseEntity<>(savedReview, HttpStatus.CREATED);
    }

    // Update a review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Update review",
            description = "Updates an existing review"
    )
    @PutMapping("/reviews/{id}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id, @Valid @RequestBody ReviewUpdateRequest request) {
        UserDto currentUser = getCurrentUser();
        ReviewResponse updatedReview = reviewService.updateReview(id, request, currentUser.getId());
        return ResponseEntity.ok(updatedReview);
    }

    // Delete a review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Delete review",
            description = "Deletes a review. Users can only delete their own reviews, while admins can delete any review."
    )
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        UserDto currentUser = getCurrentUser();
        ReviewResponse review = reviewService.getReviewById(id);
        
        // Allow deletion if user is the author or an admin
        if (review.getUserId().equals(currentUser.getId()) || 
                "ROLE_ADMIN".equals(roleService.getRoleNameById(currentUser.getRoleId()))) {
            reviewService.deleteReview(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    // Chef: Respond to a review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Chef's response to a review",
            description = "Allows a chef to respond to a review on their service"
    )
    @PostMapping("/reviews/{id}/response")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    public ResponseEntity<ReviewResponse> respondToReview(@PathVariable Long id, @RequestBody Map<String, String> request) {
        UserDto currentUser = getCurrentUser();
        ReviewResponse review = reviewService.getReviewById(id);
        
        // Verify the chef being reviewed is the current user
        ChefResponseDto chef = chefService.getChefById(review.getChefId());
        
        // Check if the chef's user ID matches the current user's ID
        if (chef.getUser().getId() != currentUser.getId()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        String response = request.get("response");
        
        // Filter chef's response content
        String filteredResponse = contentFilterService.filterText(response);
        
        ReviewResponse updatedReview = reviewService.addChefResponse(id, filteredResponse, currentUser.getId());
        
        return ResponseEntity.ok(updatedReview);
    }

    // Add a reply to a review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Add a reply to a review",
            description = "Adds a new reply to the specified review"
    )
    @PostMapping("/reviews/{id}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewReplyResponse> addReply(@PathVariable Long id, @Valid @RequestBody ReviewReplyRequest request) {
        UserDto currentUser = getCurrentUser();
        
        // Filter reply content
        String filteredContent = contentFilterService.filterText(request.getContent());
        
        // Create new request with filtered content
        ReviewReplyRequest filteredRequest = new ReviewReplyRequest();
        filteredRequest.setContent(filteredContent);
        
        ReviewReplyResponse reply = reviewReplyService.addReply(id, currentUser.getId(), filteredRequest);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    // Delete a reply
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Delete a reply",
            description = "Users can only delete their own replies, while admins can delete any reply"
    )
    @DeleteMapping("/reviews/replies/{replyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReply(@PathVariable Long replyId) {
        UserDto currentUser = getCurrentUser();
        
        boolean isAdmin = "ROLE_ADMIN".equals(roleService.getRoleNameById(currentUser.getRoleId()));
        
        // Check if user owns the reply
        boolean isOwner = reviewReplyService.getRepliesByUser(currentUser.getId()).stream()
                .anyMatch(reply -> reply.getReplyId().equals(replyId));
                
        if (isOwner || isAdmin) {
            reviewReplyService.deleteReply(replyId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    // Get reviews by the current authenticated user
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Get user's reviews",
            description = "Returns a list of reviews made by the current user"
    )
    @GetMapping("/reviews/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentUserReviews() {
        UserDto currentUser = getCurrentUser();
        
        // Get user reviews from service
        List<ReviewResponse> userReviews = reviewService.getReviewsByUser(currentUser.getId());
        
        // Add replies to each review
        for (ReviewResponse review : userReviews) {
            List<ReviewReplyResponse> replies = reviewReplyService.getRepliesByReview(review.getId());
            review.setReplies(replies);
        }
        
        // Calculate total helpful reactions across all reviews
        long totalHelpful = userReviews.stream()
                .mapToLong(review -> {
                    Map<String, Long> reactionCounts = reviewReactionService.getReactionCountsByReview(review.getId());
                    return reactionCounts.getOrDefault("helpful", 0L);
                })
                .sum();
        
        // Create response object
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", userReviews);
        response.put("totalReviews", userReviews.size());
        response.put("totalLikes", totalHelpful);
        
        return ResponseEntity.ok(response);
    }
    
    // Admin: Get reviews by user ID
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Admin: Get user reviews by ID",
            description = "Returns a list of reviews made by a specific user. Only admin can use this API."
    )
    @GetMapping("/reviews/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getReviewsByUserId(@PathVariable Long userId) {
        // No need to verify user exists as reviewService will throw exception if user is not found
        
        // Get user reviews from service
        List<ReviewResponse> userReviews = reviewService.getReviewsByUser(userId);
        
        // Add replies to each review
        for (ReviewResponse review : userReviews) {
            List<ReviewReplyResponse> replies = reviewReplyService.getRepliesByReview(review.getId());
            review.setReplies(replies);
        }
        
        // Calculate total helpful reactions across all reviews
        long totalHelpful = userReviews.stream()
                .mapToLong(review -> {
                    Map<String, Long> reactionCounts = reviewReactionService.getReactionCountsByReview(review.getId());
                    return reactionCounts.getOrDefault("helpful", 0L);
                })
                .sum();
        
        // Create response object
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", userReviews);
        response.put("totalReviews", userReviews.size());
        response.put("totalLikes", totalHelpful);
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }

    // Add a reaction to a review
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Add reaction to review",
            description = "Adds a reaction to a specific review"
    )
    @PostMapping("/reviews/{id}/reaction")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> addReaction(@PathVariable Long id, @RequestBody ReviewReactionRequest request) {
        UserDto currentUser = getCurrentUser();
        ReviewReactionResponse reaction = reviewReactionService.addReaction(id, currentUser.getId(), request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("reaction", reaction);
        response.put("counts", reviewReactionService.getReactionCountsByReview(id));
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
} 