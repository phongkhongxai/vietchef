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
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
import com.spring2025.vietchefs.services.ReviewReplyService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.RoleService;
import com.spring2025.vietchefs.services.UserService;

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
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewCriteriaService reviewCriteriaService;
    private final ReviewReplyService reviewReplyService;
    private final ReviewReactionService reviewReactionService;
    private final UserService userService;
    private final ChefService chefService;
    private final RoleService roleService;

    @Autowired
    public ReviewController(
            ReviewService reviewService,
            ReviewCriteriaService reviewCriteriaService,
            ReviewReplyService reviewReplyService,
            ReviewReactionService reviewReactionService,
            UserService userService,
            ChefService chefService,
            RoleService roleService) {
        this.reviewService = reviewService;
        this.reviewCriteriaService = reviewCriteriaService;
        this.reviewReplyService = reviewReplyService;
        this.reviewReactionService = reviewReactionService;
        this.userService = userService;
        this.chefService = chefService;
        this.roleService = roleService;
    }

    // Get current authenticated user
    private UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userService.getProfileUserByUsernameOrEmail(currentUsername, currentUsername);
    }

    // Get all review criteria
    @GetMapping("/review-criteria")
    public ResponseEntity<List<ReviewCriteriaResponse>> getAllCriteria() {
        return ResponseEntity.ok(reviewCriteriaService.getActiveCriteria());
    }

    // Admin: Create review criteria
    @PostMapping("/review-criteria")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ReviewCriteriaResponse> createCriteria(@RequestBody ReviewCriteriaRequest request) {
        return new ResponseEntity<>(reviewCriteriaService.createCriteria(request), HttpStatus.CREATED);
    }

    // Admin: Update review criteria
    @PutMapping("/review-criteria/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ReviewCriteriaResponse> updateCriteria(@PathVariable Long id, @RequestBody ReviewCriteriaRequest request) {
        return ResponseEntity.ok(reviewCriteriaService.updateCriteria(id, request));
    }

    // Get reviews for a chef
    @GetMapping("/reviews/chef/{chefId}")
    public ResponseEntity<Map<String, Object>> getReviewsByChef(
            @PathVariable Long chefId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ReviewResponse> reviewPage = reviewService.getReviewsByChef(
                chefId, PageRequest.of(page, size, Sort.by("createAt").descending()));
        
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviewPage.getContent());
        response.put("currentPage", reviewPage.getNumber());
        response.put("totalItems", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("averageRating", reviewService.getAverageRatingForChef(chefId));
        response.put("totalReviews", reviewService.getReviewCountForChef(chefId));
        response.put("ratingDistribution", reviewService.getRatingDistributionForChef(chefId));
        
        return ResponseEntity.ok(response);
    }

    // Get a specific review
    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        ReviewResponse review = reviewService.getReviewById(id);
        return ResponseEntity.ok(review);
    }

    // Create a new review
    @PostMapping("/reviews")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(@RequestBody ReviewCreateRequest request) {
        UserDto currentUser = getCurrentUser();
        ReviewResponse savedReview = reviewService.createReview(request, currentUser.getId());
        return new ResponseEntity<>(savedReview, HttpStatus.CREATED);
    }

    // Update a review
    @PutMapping("/reviews/{id}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id, @RequestBody ReviewUpdateRequest request) {
        UserDto currentUser = getCurrentUser();
        ReviewResponse updatedReview = reviewService.updateReview(id, request, currentUser.getId());
        return ResponseEntity.ok(updatedReview);
    }

    // Delete a review
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
        ReviewResponse updatedReview = reviewService.addChefResponse(id, response, currentUser.getId());
        
        return ResponseEntity.ok(updatedReview);
    }

    // Add a reply to a review
    @PostMapping("/reviews/{id}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewReplyResponse> addReply(@PathVariable Long id, @RequestBody ReviewReplyRequest request) {
        UserDto currentUser = getCurrentUser();
        ReviewReplyResponse reply = reviewReplyService.addReply(id, currentUser.getId(), request);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    // Add a reaction to a review
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