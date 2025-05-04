package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReplyRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReplyResponse;
import com.spring2025.vietchefs.services.ContentFilterService;
import com.spring2025.vietchefs.services.ReviewReplyService;
import com.spring2025.vietchefs.services.RoleService;
import com.spring2025.vietchefs.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-replies")
public class ReviewReplyController {

    private final ReviewReplyService reviewReplyService;
    private final UserService userService;
    private final ContentFilterService contentFilterService;
    private final RoleService roleService;

    @Autowired
    public ReviewReplyController(
            ReviewReplyService reviewReplyService,
            UserService userService,
            ContentFilterService contentFilterService,
            RoleService roleService) {
        this.reviewReplyService = reviewReplyService;
        this.userService = userService;
        this.contentFilterService = contentFilterService;
        this.roleService = roleService;
    }

    // Get current authenticated user
    private UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return userService.getProfileUserByUsernameOrEmail(currentUsername, currentUsername);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Thêm phản hồi vào đánh giá",
            description = "Người dùng đã xác thực có thể thêm phản hồi vào đánh giá"
    )
    @PostMapping("/{reviewId}/replies")
    public ResponseEntity<ReviewReplyResponse> addReplyToReview(
            @PathVariable Long reviewId, 
            @Valid @RequestBody ReviewReplyRequest request) {
        UserDto currentUser = getCurrentUser();
        
        // Lọc nội dung phản hồi
        String filteredContent = contentFilterService.filterText(request.getContent());
        
        // Tạo request mới với nội dung đã lọc
        ReviewReplyRequest filteredRequest = new ReviewReplyRequest();
        filteredRequest.setContent(filteredContent);
        
        ReviewReplyResponse reply = reviewReplyService.addReply(reviewId, currentUser.getId(), filteredRequest);
        return new ResponseEntity<>(reply, HttpStatus.CREATED);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Xóa phản hồi",
            description = "Người dùng chỉ có thể xóa phản hồi của chính họ hoặc admin có thể xóa bất kỳ phản hồi nào"
    )
    @DeleteMapping("/{replyId}")
    public ResponseEntity<Void> deleteReply(@PathVariable Long replyId) {
        UserDto currentUser = getCurrentUser();
        
        // Lấy danh sách phản hồi của người dùng
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
} 