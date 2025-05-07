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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        
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
        
        Page<ReviewResponse> reviewPage = reviewService.getReviewsByChef(chefId, pageable);
        
        long reviewCount = reviewService.getReviewCountForChef(chefId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviewPage.getContent());
        response.put("currentPage", reviewPage.getNumber());
        response.put("totalReviews", reviewCount);
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("averageRating", reviewService.getAverageRatingForChef(chefId));
        response.put("ratingDistribution", reviewService.getRatingDistributionForChef(chefId));
        
        return ResponseEntity.ok(response);
    }

    // Get a specific review
    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        ReviewResponse review = reviewService.getReviewById(id);
        return ResponseEntity.ok(review);
    }

    // Get a specific review for a booking
    @GetMapping("/reviews/booking/{bookingId}")
    public ResponseEntity<?> getReviewByBooking(@PathVariable Long bookingId) {
        ReviewResponse review = reviewService.getReviewByBooking(bookingId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasReview", review != null);
        response.put("review", review);
        response.put("message", review != null ? 
            "BR-46: Buổi đặt này đã có đánh giá." : 
            "Buổi đặt này chưa có đánh giá.");
        
        return ResponseEntity.ok(response);
    }

    // Create a new review
    @PostMapping("/reviews")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(@RequestBody ReviewCreateRequest request) {
        UserDto currentUser = getCurrentUser();
        
        // Lọc nội dung đánh giá
        String filteredDescription = contentFilterService.filterText(request.getDescription());
        request.setDescription(filteredDescription);
        
        // Lọc nội dung trải nghiệm tổng thể
        if (request.getOverallExperience() != null) {
            String filteredExperience = contentFilterService.filterText(request.getOverallExperience());
            request.setOverallExperience(filteredExperience);
        }
        
        ReviewResponse savedReview = reviewService.createReview(request, currentUser.getId());
        return new ResponseEntity<>(savedReview, HttpStatus.CREATED);
    }

    // Update a review
    @PutMapping("/reviews/{id}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id, @RequestBody ReviewUpdateRequest request) {
        UserDto currentUser = getCurrentUser();
        
        // Lọc nội dung đánh giá
        String filteredDescription = contentFilterService.filterText(request.getDescription());
        request.setDescription(filteredDescription);
        
        // Lọc nội dung trải nghiệm tổng thể
        if (request.getOverallExperience() != null) {
            String filteredExperience = contentFilterService.filterText(request.getOverallExperience());
            request.setOverallExperience(filteredExperience);
        }
        
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
        
        // Lọc nội dung phản hồi của chef
        String filteredResponse = contentFilterService.filterText(response);
        
        ReviewResponse updatedReview = reviewService.addChefResponse(id, filteredResponse, currentUser.getId());
        
        return ResponseEntity.ok(updatedReview);
    }

    // Add a reply to a review
    @PostMapping("/reviews/{id}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewReplyResponse> addReply(@PathVariable Long id, @RequestBody ReviewReplyRequest request) {
        UserDto currentUser = getCurrentUser();
        
        // Lọc nội dung phản hồi
        String filteredContent = contentFilterService.filterText(request.getContent());
        
        // Tạo request mới với nội dung đã lọc
        ReviewReplyRequest filteredRequest = new ReviewReplyRequest();
        filteredRequest.setContent(filteredContent);
        
        ReviewReplyResponse reply = reviewReplyService.addReply(id, currentUser.getId(), filteredRequest);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    // Get replies by user
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Lấy danh sách phản hồi của người dùng",
            description = "Trả về danh sách các phản hồi của người dùng hiện tại"
    )
    @GetMapping("/reviews/replies/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReviewReplyResponse>> getRepliesByUser() {
        UserDto currentUser = getCurrentUser();
        List<ReviewReplyResponse> replies = reviewReplyService.getRepliesByUser(currentUser.getId());
        return ResponseEntity.ok(replies);
    }

    // Delete a reply
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Xóa phản hồi",
            description = "Người dùng chỉ có thể xóa phản hồi của chính họ hoặc admin có thể xóa bất kỳ phản hồi nào"
    )
    @DeleteMapping("/reviews/replies/{replyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReply(@PathVariable Long replyId) {
        UserDto currentUser = getCurrentUser();
        
        boolean isAdmin = "ROLE_ADMIN".equals(roleService.getRoleNameById(currentUser.getRoleId()));
        
        // Kiểm tra quyền xóa từ các phản hồi của người dùng
        boolean isOwner = reviewReplyService.getRepliesByUser(currentUser.getId()).stream()
                .anyMatch(reply -> reply.getReplyId().equals(replyId));
                
        if (isOwner || isAdmin) {
            reviewReplyService.deleteReply(replyId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
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