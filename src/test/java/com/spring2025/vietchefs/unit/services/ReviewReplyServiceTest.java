package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReply;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReplyRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReplyResponse;
import com.spring2025.vietchefs.repositories.ReviewReplyRepository;
import com.spring2025.vietchefs.repositories.ReviewRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.impl.ReviewReplyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewReplyServiceTest {

    @Mock
    private ReviewReplyRepository reviewReplyRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewReplyServiceImpl reviewReplyService;

    @Captor
    private ArgumentCaptor<ReviewReply> replyCaptor;

    private Review testReview;
    private User testUser;
    private ReviewReply testReply;
    private ReviewReplyRequest testRequest;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        
        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .avatarUrl("avatar.jpg")
                .build();

        testReview = Review.builder()
                .id(1L)
                .user(testUser)
                .build();

        testReply = ReviewReply.builder()
                .replyId(1L)
                .review(testReview)
                .user(testUser)
                .content("This is a test reply")
                .createdAt(testTime)
                .isDeleted(false)
                .build();

        testRequest = new ReviewReplyRequest("This is a test reply");
    }

    // ==================== addReply Tests ====================

    @Test
    @DisplayName("Test 1: addReply when all parameters are valid should create and return reply")
    void addReply_WhenAllParametersValid_ShouldCreateAndReturnReply() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(testReply);

        // Act
        ReviewReplyResponse result = reviewReplyService.addReply(reviewId, userId, testRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getReplyId());
        assertEquals(reviewId, result.getReviewId());
        assertEquals(userId, result.getUserId());
        assertEquals("John Doe", result.getUserName());
        assertEquals("avatar.jpg", result.getUserAvatar());
        assertEquals("This is a test reply", result.getContent());
        
        verify(reviewRepository).findById(reviewId);
        verify(userRepository).findById(userId);
        verify(reviewReplyRepository).save(any(ReviewReply.class));
    }

    @Test
    @DisplayName("Test 2: addReply when review not found should throw ResourceNotFoundException")
    void addReply_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 99L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.addReply(reviewId, userId, testRequest);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewReplyRepository, never()).save(any(ReviewReply.class));
    }

    @Test
    @DisplayName("Test 3: addReply when user not found should throw ResourceNotFoundException")
    void addReply_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 99L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.addReply(reviewId, userId, testRequest);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(reviewReplyRepository, never()).save(any(ReviewReply.class));
    }

    @Test
    @DisplayName("Test 4: addReply should set correct values in the saved entity")
    void addReply_ShouldSetCorrectValuesInSavedEntity() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(testReply);

        // Act
        reviewReplyService.addReply(reviewId, userId, testRequest);

        // Assert
        verify(reviewReplyRepository).save(replyCaptor.capture());
        ReviewReply capturedReply = replyCaptor.getValue();
        
        assertEquals(testReview, capturedReply.getReview());
        assertEquals(testUser, capturedReply.getUser());
        assertEquals("This is a test reply", capturedReply.getContent());
        assertNotNull(capturedReply.getCreatedAt());
        assertFalse(capturedReply.getIsDeleted());
    }

    // ==================== updateReply Tests ====================

    @Test
    @DisplayName("Test 1: updateReply when reply exists should update and return reply")
    void updateReply_WhenReplyExists_ShouldUpdateAndReturnReply() {
        // Arrange
        Long replyId = 1L;
        ReviewReplyRequest updateRequest = new ReviewReplyRequest("Updated reply content");
        
        ReviewReply existingReply = testReply;
        ReviewReply updatedReply = ReviewReply.builder()
                .replyId(1L)
                .review(testReview)
                .user(testUser)
                .content("Updated reply content")
                .createdAt(testTime)
                .isDeleted(false)
                .build();
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.of(existingReply));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(updatedReply);

        // Act
        ReviewReplyResponse result = reviewReplyService.updateReply(replyId, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(replyId, result.getReplyId());
        assertEquals("Updated reply content", result.getContent());
        
        verify(reviewReplyRepository).findById(replyId);
        verify(reviewReplyRepository).save(any(ReviewReply.class));
    }

    @Test
    @DisplayName("Test 2: updateReply when reply not found should throw ResourceNotFoundException")
    void updateReply_WhenReplyNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long replyId = 99L;
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.updateReply(replyId, testRequest);
        });
        
        assertTrue(exception.getMessage().contains("Reply not found"));
        verify(reviewReplyRepository, never()).save(any(ReviewReply.class));
    }

    @Test
    @DisplayName("Test 3: updateReply should only update content field")
    void updateReply_ShouldOnlyUpdateContentField() {
        // Arrange
        Long replyId = 1L;
        ReviewReplyRequest updateRequest = new ReviewReplyRequest("Updated reply content");
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.of(testReply));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(testReply);

        // Act
        reviewReplyService.updateReply(replyId, updateRequest);

        // Assert
        verify(reviewReplyRepository).save(replyCaptor.capture());
        ReviewReply capturedReply = replyCaptor.getValue();
        
        assertEquals("Updated reply content", capturedReply.getContent());
    }

    @Test
    @DisplayName("Test 4: updateReply with empty content should still update")
    void updateReply_WithEmptyContent_ShouldStillUpdate() {
        // Arrange
        Long replyId = 1L;
        ReviewReplyRequest emptyRequest = new ReviewReplyRequest("");
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.of(testReply));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(testReply);

        // Act
        ReviewReplyResponse result = reviewReplyService.updateReply(replyId, emptyRequest);

        // Assert
        assertNotNull(result);
        verify(reviewReplyRepository).save(replyCaptor.capture());
        ReviewReply capturedReply = replyCaptor.getValue();
        
        assertEquals("", capturedReply.getContent());
    }

    // ==================== deleteReply Tests ====================

    @Test
    @DisplayName("Test 1: deleteReply when reply exists should soft delete the reply")
    void deleteReply_WhenReplyExists_ShouldSoftDeleteReply() {
        // Arrange
        Long replyId = 1L;
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.of(testReply));

        // Act
        reviewReplyService.deleteReply(replyId);

        // Assert
        verify(reviewReplyRepository).findById(replyId);
        verify(reviewReplyRepository).save(replyCaptor.capture());
        
        ReviewReply capturedReply = replyCaptor.getValue();
        assertTrue(capturedReply.getIsDeleted());
    }

    @Test
    @DisplayName("Test 2: deleteReply when reply not found should throw ResourceNotFoundException")
    void deleteReply_WhenReplyNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long replyId = 99L;
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.deleteReply(replyId);
        });
        
        assertTrue(exception.getMessage().contains("Reply not found"));
        verify(reviewReplyRepository, never()).save(any(ReviewReply.class));
    }

    @Test
    @DisplayName("Test 3: deleteReply should not change other properties")
    void deleteReply_ShouldNotChangeOtherProperties() {
        // Arrange
        Long replyId = 1L;
        String originalContent = testReply.getContent();
        Review originalReview = testReply.getReview();
        User originalUser = testReply.getUser();
        LocalDateTime originalCreatedAt = testReply.getCreatedAt();
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.of(testReply));

        // Act
        reviewReplyService.deleteReply(replyId);

        // Assert
        verify(reviewReplyRepository).save(replyCaptor.capture());
        ReviewReply capturedReply = replyCaptor.getValue();
        
        assertEquals(originalContent, capturedReply.getContent());
        assertEquals(originalReview, capturedReply.getReview());
        assertEquals(originalUser, capturedReply.getUser());
        assertEquals(originalCreatedAt, capturedReply.getCreatedAt());
        assertTrue(capturedReply.getIsDeleted());
    }

    @Test
    @DisplayName("Test 4: deleteReply when already deleted should still set isDeleted to true")
    void deleteReply_WhenAlreadyDeleted_ShouldStillSetIsDeletedToTrue() {
        // Arrange
        Long replyId = 1L;
        
        ReviewReply alreadyDeletedReply = ReviewReply.builder()
                .replyId(1L)
                .review(testReview)
                .user(testUser)
                .content("This is a test reply")
                .createdAt(testTime)
                .isDeleted(true)
                .build();
        
        when(reviewReplyRepository.findById(replyId)).thenReturn(Optional.of(alreadyDeletedReply));

        // Act
        reviewReplyService.deleteReply(replyId);

        // Assert
        verify(reviewReplyRepository).save(replyCaptor.capture());
        ReviewReply capturedReply = replyCaptor.getValue();
        
        assertTrue(capturedReply.getIsDeleted());
    }

    // ==================== getRepliesByReview Tests ====================

    @Test
    @DisplayName("Test 1: getRepliesByReview when review exists should return list of replies")
    void getRepliesByReview_WhenReviewExists_ShouldReturnListOfReplies() {
        // Arrange
        Long reviewId = 1L;
        
        ReviewReply reply1 = ReviewReply.builder()
                .replyId(1L)
                .review(testReview)
                .user(testUser)
                .content("First reply")
                .createdAt(testTime)
                .isDeleted(false)
                .build();
        
        ReviewReply reply2 = ReviewReply.builder()
                .replyId(2L)
                .review(testReview)
                .user(testUser)
                .content("Second reply")
                .createdAt(testTime.plusHours(1))
                .isDeleted(false)
                .build();
        
        List<ReviewReply> replies = Arrays.asList(reply2, reply1); // Descending order by createdAt
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReplyRepository.findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(testReview)).thenReturn(replies);

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getReplyId()); // First in list should be reply2
        assertEquals(1L, result.get(1).getReplyId()); // Second in list should be reply1
        assertEquals("Second reply", result.get(0).getContent());
        assertEquals("First reply", result.get(1).getContent());
        
        verify(reviewRepository).findById(reviewId);
        verify(reviewReplyRepository).findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(testReview);
    }

    @Test
    @DisplayName("Test 2: getRepliesByReview when review not found should throw ResourceNotFoundException")
    void getRepliesByReview_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 99L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.getRepliesByReview(reviewId);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewReplyRepository, never()).findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(any(Review.class));
    }

    @Test
    @DisplayName("Test 3: getRepliesByReview when no replies exist should return empty list")
    void getRepliesByReview_WhenNoRepliesExist_ShouldReturnEmptyList() {
        // Arrange
        Long reviewId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReplyRepository.findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(testReview)).thenReturn(new ArrayList<>());

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(reviewRepository).findById(reviewId);
        verify(reviewReplyRepository).findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(testReview);
    }

    @Test
    @DisplayName("Test 4: getRepliesByReview should correctly map entity fields to response")
    void getRepliesByReview_ShouldCorrectlyMapEntityFieldsToResponse() {
        // Arrange
        Long reviewId = 1L;
        
        User anotherUser = User.builder()
                .id(2L)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .avatarUrl("jane-avatar.jpg")
                .build();
        
        ReviewReply reply = ReviewReply.builder()
                .replyId(1L)
                .review(testReview)
                .user(anotherUser)
                .content("Test reply content")
                .createdAt(testTime)
                .isDeleted(false)
                .build();
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReplyRepository.findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(testReview)).thenReturn(List.of(reply));

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        ReviewReplyResponse response = result.get(0);
        assertEquals(reply.getReplyId(), response.getReplyId());
        assertEquals(reply.getReview().getId(), response.getReviewId());
        assertEquals(reply.getUser().getId(), response.getUserId());
        assertEquals(reply.getUser().getFullName(), response.getUserName());
        assertEquals(reply.getUser().getAvatarUrl(), response.getUserAvatar());
        assertEquals(reply.getContent(), response.getContent());
        assertEquals(reply.getCreatedAt(), response.getCreatedAt());
    }

    // ==================== getRepliesByUser Tests ====================

    @Test
    @DisplayName("Test 1: getRepliesByUser when user exists should return list of replies")
    void getRepliesByUser_WhenUserExists_ShouldReturnListOfReplies() {
        // Arrange
        Long userId = 1L;
        
        Review anotherReview = Review.builder()
                .id(2L)
                .user(testUser)
                .build();
        
        ReviewReply reply1 = ReviewReply.builder()
                .replyId(1L)
                .review(testReview)
                .user(testUser)
                .content("Reply to first review")
                .createdAt(testTime)
                .isDeleted(false)
                .build();
        
        ReviewReply reply2 = ReviewReply.builder()
                .replyId(2L)
                .review(anotherReview)
                .user(testUser)
                .content("Reply to second review")
                .createdAt(testTime.plusHours(1))
                .isDeleted(false)
                .build();
        
        List<ReviewReply> replies = Arrays.asList(reply2, reply1); // Descending order by createdAt
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReplyRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(testUser)).thenReturn(replies);

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getReplyId()); // First in list should be reply2
        assertEquals(1L, result.get(1).getReplyId()); // Second in list should be reply1
        assertEquals(2L, result.get(0).getReviewId());
        assertEquals(1L, result.get(1).getReviewId());
        
        verify(userRepository).findById(userId);
        verify(reviewReplyRepository).findByUserAndIsDeletedFalseOrderByCreatedAtDesc(testUser);
    }

    @Test
    @DisplayName("Test 2: getRepliesByUser when user not found should throw ResourceNotFoundException")
    void getRepliesByUser_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long userId = 99L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.getRepliesByUser(userId);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(reviewReplyRepository, never()).findByUserAndIsDeletedFalseOrderByCreatedAtDesc(any(User.class));
    }

    @Test
    @DisplayName("Test 3: getRepliesByUser when no replies exist should return empty list")
    void getRepliesByUser_WhenNoRepliesExist_ShouldReturnEmptyList() {
        // Arrange
        Long userId = 1L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReplyRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(testUser)).thenReturn(new ArrayList<>());

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByUser(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(userRepository).findById(userId);
        verify(reviewReplyRepository).findByUserAndIsDeletedFalseOrderByCreatedAtDesc(testUser);
    }

    @Test
    @DisplayName("Test 4: getRepliesByUser should correctly map entity fields to response")
    void getRepliesByUser_ShouldCorrectlyMapEntityFieldsToResponse() {
        // Arrange
        Long userId = 1L;
        
        ReviewReply reply = ReviewReply.builder()
                .replyId(1L)
                .review(testReview)
                .user(testUser)
                .content("Test reply content")
                .createdAt(testTime)
                .isDeleted(false)
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReplyRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(testUser)).thenReturn(List.of(reply));

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        ReviewReplyResponse response = result.get(0);
        assertEquals(reply.getReplyId(), response.getReplyId());
        assertEquals(reply.getReview().getId(), response.getReviewId());
        assertEquals(reply.getUser().getId(), response.getUserId());
        assertEquals(reply.getUser().getFullName(), response.getUserName());
        assertEquals(reply.getUser().getAvatarUrl(), response.getUserAvatar());
        assertEquals(reply.getContent(), response.getContent());
        assertEquals(reply.getCreatedAt(), response.getCreatedAt());
    }
} 