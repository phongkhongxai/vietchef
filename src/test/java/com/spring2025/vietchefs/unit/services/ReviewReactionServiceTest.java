package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReaction;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReactionRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReactionResponse;
import com.spring2025.vietchefs.repositories.ReviewReactionRepository;
import com.spring2025.vietchefs.repositories.ReviewRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.impl.ReviewReactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewReactionServiceTest {

    @Mock
    private ReviewReactionRepository reviewReactionRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewReactionServiceImpl reviewReactionService;

    private Review testReview;
    private User testUser;
    private ReviewReaction testReaction;
    private ReviewReactionRequest testRequest;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        
        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .build();

        testReview = Review.builder()
                .id(1L)
                .user(testUser)
                .build();

        testReaction = ReviewReaction.builder()
                .reactionId(1L)
                .review(testReview)
                .user(testUser)
                .reactionType("helpful")
                .createdAt(testTime)
                .build();

        testRequest = new ReviewReactionRequest("helpful");
    }

    // ----- addReaction Tests -----
    
    @Test
    void addReaction_WhenNewReaction_ShouldCreateAndReturnReaction() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.empty());
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(testReaction);

        // Act
        ReviewReactionResponse result = reviewReactionService.addReaction(reviewId, userId, testRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getReactionId());
        assertEquals(reviewId, result.getReviewId());
        assertEquals(userId, result.getUserId());
        assertEquals("helpful", result.getReactionType());
        
        verify(reviewReactionRepository).findByReviewAndUser(testReview, testUser);
        verify(reviewReactionRepository).save(any(ReviewReaction.class));
    }

    @Test
    void addReaction_WhenExistingReaction_ShouldUpdateAndReturnReaction() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 1L;
        ReviewReactionRequest updateRequest = new ReviewReactionRequest("not_helpful");
        
        ReviewReaction existingReaction = ReviewReaction.builder()
                .reactionId(1L)
                .review(testReview)
                .user(testUser)
                .reactionType("helpful")
                .createdAt(testTime)
                .build();
        
        ReviewReaction updatedReaction = ReviewReaction.builder()
                .reactionId(1L)
                .review(testReview)
                .user(testUser)
                .reactionType("not_helpful")
                .createdAt(testTime)
                .build();
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.of(existingReaction));
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(updatedReaction);

        // Act
        ReviewReactionResponse result = reviewReactionService.addReaction(reviewId, userId, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getReactionId());
        assertEquals("not_helpful", result.getReactionType());
        
        verify(reviewReactionRepository).findByReviewAndUser(testReview, testUser);
        verify(reviewReactionRepository).save(any(ReviewReaction.class));
    }

    @Test
    void addReaction_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 99L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.addReaction(reviewId, userId, testRequest);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewReactionRepository, never()).save(any(ReviewReaction.class));
    }

    @Test
    void addReaction_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 99L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.addReaction(reviewId, userId, testRequest);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(reviewReactionRepository, never()).save(any(ReviewReaction.class));
    }

    // ----- updateReaction Tests -----
    
    @Test
    void updateReaction_WhenReactionExists_ShouldUpdateAndReturnReaction() {
        // Arrange
        Long reactionId = 1L;
        ReviewReactionRequest updateRequest = new ReviewReactionRequest("not_helpful");
        
        ReviewReaction existingReaction = ReviewReaction.builder()
                .reactionId(reactionId)
                .review(testReview)
                .user(testUser)
                .reactionType("helpful")
                .createdAt(testTime)
                .build();
        
        ReviewReaction updatedReaction = ReviewReaction.builder()
                .reactionId(reactionId)
                .review(testReview)
                .user(testUser)
                .reactionType("not_helpful")
                .createdAt(testTime)
                .build();
        
        when(reviewReactionRepository.findById(reactionId)).thenReturn(Optional.of(existingReaction));
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(updatedReaction);

        // Act
        ReviewReactionResponse result = reviewReactionService.updateReaction(reactionId, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(reactionId, result.getReactionId());
        assertEquals("not_helpful", result.getReactionType());
        
        verify(reviewReactionRepository).findById(reactionId);
        verify(reviewReactionRepository).save(any(ReviewReaction.class));
    }

    @Test
    void updateReaction_WhenReactionNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reactionId = 99L;
        
        when(reviewReactionRepository.findById(reactionId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.updateReaction(reactionId, testRequest);
        });
        
        assertTrue(exception.getMessage().contains("Reaction not found"));
        verify(reviewReactionRepository, never()).save(any(ReviewReaction.class));
    }

    @Test
    void updateReaction_WhenSameReactionType_ShouldStillUpdateAndReturnReaction() {
        // Arrange
        Long reactionId = 1L;
        ReviewReactionRequest sameRequest = new ReviewReactionRequest("helpful");
        
        when(reviewReactionRepository.findById(reactionId)).thenReturn(Optional.of(testReaction));
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(testReaction);

        // Act
        ReviewReactionResponse result = reviewReactionService.updateReaction(reactionId, sameRequest);

        // Assert
        assertNotNull(result);
        assertEquals(reactionId, result.getReactionId());
        assertEquals("helpful", result.getReactionType());
        
        verify(reviewReactionRepository).findById(reactionId);
        verify(reviewReactionRepository).save(any(ReviewReaction.class));
    }

    @Test
    void updateReaction_WhenInvalidReactionType_ShouldStillProcessRequest() {
        // Arrange
        Long reactionId = 1L;
        ReviewReactionRequest invalidRequest = new ReviewReactionRequest("invalid_type");
        
        ReviewReaction existingReaction = ReviewReaction.builder()
                .reactionId(reactionId)
                .review(testReview)
                .user(testUser)
                .reactionType("helpful")
                .createdAt(testTime)
                .build();
        
        ReviewReaction updatedReaction = ReviewReaction.builder()
                .reactionId(reactionId)
                .review(testReview)
                .user(testUser)
                .reactionType("invalid_type")
                .createdAt(testTime)
                .build();
        
        when(reviewReactionRepository.findById(reactionId)).thenReturn(Optional.of(existingReaction));
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(updatedReaction);

        // Act
        ReviewReactionResponse result = reviewReactionService.updateReaction(reactionId, invalidRequest);

        // Assert
        assertNotNull(result);
        assertEquals("invalid_type", result.getReactionType());
        
        verify(reviewReactionRepository).save(any(ReviewReaction.class));
    }

    // ----- removeReaction Tests -----
    
    @Test
    void removeReaction_WhenReactionExists_ShouldDeleteReaction() {
        // Arrange
        Long reactionId = 1L;
        
        when(reviewReactionRepository.existsById(reactionId)).thenReturn(true);
        doNothing().when(reviewReactionRepository).deleteById(reactionId);

        // Act
        reviewReactionService.removeReaction(reactionId);

        // Assert
        verify(reviewReactionRepository).existsById(reactionId);
        verify(reviewReactionRepository).deleteById(reactionId);
    }

    @Test
    void removeReaction_WhenReactionNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reactionId = 99L;
        
        when(reviewReactionRepository.existsById(reactionId)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.removeReaction(reactionId);
        });
        
        assertTrue(exception.getMessage().contains("Reaction not found"));
        verify(reviewReactionRepository, never()).deleteById(anyLong());
    }

    @Test
    void removeReaction_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        Long reactionId = 1L;
        
        when(reviewReactionRepository.existsById(reactionId)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(reviewReactionRepository).deleteById(reactionId);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewReactionService.removeReaction(reactionId);
        });
        
        assertEquals("Database error", exception.getMessage());
    }

    @Test
    void removeReaction_WhenZeroIdProvided_ShouldAttemptToFindAndDelete() {
        // Arrange
        Long reactionId = 0L;
        
        when(reviewReactionRepository.existsById(reactionId)).thenReturn(true);
        doNothing().when(reviewReactionRepository).deleteById(reactionId);

        // Act
        reviewReactionService.removeReaction(reactionId);

        // Assert
        verify(reviewReactionRepository).existsById(reactionId);
        verify(reviewReactionRepository).deleteById(reactionId);
    }

    // ----- getReactionsByReview Tests -----
    
    @Test
    void getReactionsByReview_WhenReviewExists_ShouldReturnReactionsList() {
        // Arrange
        Long reviewId = 1L;
        List<ReviewReaction> reactions = Arrays.asList(
            ReviewReaction.builder()
                .reactionId(1L)
                .review(testReview)
                .user(testUser)
                .reactionType("helpful")
                .createdAt(testTime)
                .build(),
            ReviewReaction.builder()
                .reactionId(2L)
                .review(testReview)
                .user(User.builder().id(2L).fullName("Jane Doe").build())
                .reactionType("not_helpful")
                .createdAt(testTime)
                .build()
        );
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.findByReview(testReview)).thenReturn(reactions);

        // Act
        List<ReviewReactionResponse> result = reviewReactionService.getReactionsByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getReactionId());
        assertEquals("helpful", result.get(0).getReactionType());
        assertEquals(2L, result.get(1).getReactionId());
        assertEquals("not_helpful", result.get(1).getReactionType());
        
        verify(reviewRepository).findById(reviewId);
        verify(reviewReactionRepository).findByReview(testReview);
    }

    @Test
    void getReactionsByReview_WhenReviewExistsButNoReactions_ShouldReturnEmptyList() {
        // Arrange
        Long reviewId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.findByReview(testReview)).thenReturn(Collections.emptyList());

        // Act
        List<ReviewReactionResponse> result = reviewReactionService.getReactionsByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(reviewRepository).findById(reviewId);
        verify(reviewReactionRepository).findByReview(testReview);
    }

    @Test
    void getReactionsByReview_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 99L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.getReactionsByReview(reviewId);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewReactionRepository, never()).findByReview(any(Review.class));
    }

    @Test
    void getReactionsByReview_WhenMappingResponsesWithMultipleReactions_ShouldMapCorrectly() {
        // Arrange
        Long reviewId = 1L;
        User user1 = User.builder().id(1L).fullName("John Doe").build();
        User user2 = User.builder().id(2L).fullName("Jane Doe").build();
        
        List<ReviewReaction> reactions = Arrays.asList(
            ReviewReaction.builder()
                .reactionId(1L)
                .review(testReview)
                .user(user1)
                .reactionType("helpful")
                .createdAt(testTime)
                .build(),
            ReviewReaction.builder()
                .reactionId(2L)
                .review(testReview)
                .user(user2)
                .reactionType("not_helpful")
                .createdAt(testTime.plusHours(1))
                .build()
        );
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.findByReview(testReview)).thenReturn(reactions);

        // Act
        List<ReviewReactionResponse> result = reviewReactionService.getReactionsByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        assertEquals(1L, result.get(0).getReactionId());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals("John Doe", result.get(0).getUserName());
        assertEquals("helpful", result.get(0).getReactionType());
        assertEquals(testTime, result.get(0).getCreatedAt());
        
        assertEquals(2L, result.get(1).getReactionId());
        assertEquals(2L, result.get(1).getUserId());
        assertEquals("Jane Doe", result.get(1).getUserName());
        assertEquals("not_helpful", result.get(1).getReactionType());
        assertEquals(testTime.plusHours(1), result.get(1).getCreatedAt());
    }

    // ----- hasUserReacted Tests -----
    
    @Test
    void hasUserReacted_WhenUserHasReacted_ShouldReturnTrue() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.of(testReaction));

        // Act
        boolean result = reviewReactionService.hasUserReacted(reviewId, userId);

        // Assert
        assertTrue(result);
        
        verify(reviewRepository).findById(reviewId);
        verify(userRepository).findById(userId);
        verify(reviewReactionRepository).findByReviewAndUser(testReview, testUser);
    }

    @Test
    void hasUserReacted_WhenUserHasNotReacted_ShouldReturnFalse() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.empty());

        // Act
        boolean result = reviewReactionService.hasUserReacted(reviewId, userId);

        // Assert
        assertFalse(result);
        
        verify(reviewRepository).findById(reviewId);
        verify(userRepository).findById(userId);
        verify(reviewReactionRepository).findByReviewAndUser(testReview, testUser);
    }

    @Test
    void hasUserReacted_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 99L;
        Long userId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.hasUserReacted(reviewId, userId);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
        verify(userRepository, never()).findById(anyLong());
        verify(reviewReactionRepository, never()).findByReviewAndUser(any(), any());
    }

    @Test
    void hasUserReacted_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 1L;
        Long userId = 99L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.hasUserReacted(reviewId, userId);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(reviewReactionRepository, never()).findByReviewAndUser(any(), any());
    }

    // ----- getReactionCountsByReview Tests -----
    
    @Test
    void getReactionCountsByReview_WhenReviewExists_ShouldReturnCountMap() {
        // Arrange
        Long reviewId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "helpful")).thenReturn(3L);
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "not_helpful")).thenReturn(2L);

        // Act
        Map<String, Long> result = reviewReactionService.getReactionCountsByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(3L, result.get("helpful"));
        assertEquals(2L, result.get("not_helpful"));
        
        verify(reviewRepository).findById(reviewId);
        verify(reviewReactionRepository).countByReviewAndReactionType(testReview, "helpful");
        verify(reviewReactionRepository).countByReviewAndReactionType(testReview, "not_helpful");
    }

    @Test
    void getReactionCountsByReview_WhenNoReactions_ShouldReturnZeroCounts() {
        // Arrange
        Long reviewId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "helpful")).thenReturn(0L);
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "not_helpful")).thenReturn(0L);

        // Act
        Map<String, Long> result = reviewReactionService.getReactionCountsByReview(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(0L, result.get("helpful"));
        assertEquals(0L, result.get("not_helpful"));
    }

    @Test
    void getReactionCountsByReview_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long reviewId = 99L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.getReactionCountsByReview(reviewId);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewReactionRepository, never()).countByReviewAndReactionType(any(), anyString());
    }

    @Test
    void getReactionCountsByReview_WhenRepositoriesInteracted_ShouldMakeExactlyRequiredCalls() {
        // Arrange
        Long reviewId = 1L;
        
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "helpful")).thenReturn(5L);
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "not_helpful")).thenReturn(3L);

        // Act
        reviewReactionService.getReactionCountsByReview(reviewId);

        // Assert
        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewReactionRepository, times(1)).countByReviewAndReactionType(testReview, "helpful");
        verify(reviewReactionRepository, times(1)).countByReviewAndReactionType(testReview, "not_helpful");
        verifyNoMoreInteractions(reviewRepository, reviewReactionRepository);
    }
} 