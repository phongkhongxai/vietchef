package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReaction;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReactionRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReactionResponse;
import com.spring2025.vietchefs.repositories.ReviewReactionRepository;
import com.spring2025.vietchefs.repositories.ReviewRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
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
public class ReviewReactionServiceImplTest {

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
    private ReviewReactionRequest reactionRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test User");

        testReview = new Review();
        testReview.setId(2L);

        testReaction = new ReviewReaction();
        testReaction.setReactionId(3L);
        testReaction.setReview(testReview);
        testReaction.setUser(testUser);
        testReaction.setReactionType("helpful");
        testReaction.setCreatedAt(LocalDateTime.now());

        reactionRequest = new ReviewReactionRequest();
        reactionRequest.setReactionType("helpful");
    }

    @Test
    void addReaction_ShouldCreateNewReaction_WhenUserHasNotReacted() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.empty());
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(testReaction);

        // Act
        ReviewReactionResponse result = reviewReactionService.addReaction(testReview.getId(), testUser.getId(), reactionRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testReaction.getReactionId(), result.getReactionId());
        assertEquals(testReaction.getReactionType(), result.getReactionType());
        assertEquals(testUser.getId(), result.getUserId());
        verify(reviewReactionRepository).save(any(ReviewReaction.class));
    }

    @Test
    void addReaction_ShouldUpdateExistingReaction_WhenUserHasAlreadyReacted() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.of(testReaction));
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(testReaction);

        // Create a different reaction type for the request
        reactionRequest.setReactionType("not_helpful");

        // Act
        ReviewReactionResponse result = reviewReactionService.addReaction(testReview.getId(), testUser.getId(), reactionRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testReaction.getReactionId(), result.getReactionId());
        assertEquals("not_helpful", testReaction.getReactionType()); // Should be updated
        verify(reviewReactionRepository).save(testReaction);
    }

    @Test
    void updateReaction_ShouldReturnUpdatedReaction() {
        // Arrange
        when(reviewReactionRepository.findById(testReaction.getReactionId())).thenReturn(Optional.of(testReaction));
        when(reviewReactionRepository.save(any(ReviewReaction.class))).thenReturn(testReaction);

        // Create a different reaction type for the request
        reactionRequest.setReactionType("not_helpful");

        // Act
        ReviewReactionResponse result = reviewReactionService.updateReaction(testReaction.getReactionId(), reactionRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testReaction.getReactionId(), result.getReactionId());
        assertEquals("not_helpful", testReaction.getReactionType());
        verify(reviewReactionRepository).save(testReaction);
    }

    @Test
    void removeReaction_ShouldDeleteReaction() {
        // Arrange
        when(reviewReactionRepository.existsById(testReaction.getReactionId())).thenReturn(true);

        // Act
        reviewReactionService.removeReaction(testReaction.getReactionId());

        // Assert
        verify(reviewReactionRepository).deleteById(testReaction.getReactionId());
    }

    @Test
    void getReactionsByReview_ShouldReturnListOfReactions() {
        // Arrange
        List<ReviewReaction> reactions = Arrays.asList(testReaction);
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.findByReview(testReview)).thenReturn(reactions);

        // Act
        List<ReviewReactionResponse> result = reviewReactionService.getReactionsByReview(testReview.getId());

        // Assert
        assertEquals(1, result.size());
        assertEquals(testReaction.getReactionId(), result.get(0).getReactionId());
        assertEquals(testReaction.getReactionType(), result.get(0).getReactionType());
    }

    @Test
    void hasUserReacted_ShouldReturnTrue_WhenUserHasReacted() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.of(testReaction));

        // Act
        boolean result = reviewReactionService.hasUserReacted(testReview.getId(), testUser.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void hasUserReacted_ShouldReturnFalse_WhenUserHasNotReacted() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reviewReactionRepository.findByReviewAndUser(testReview, testUser)).thenReturn(Optional.empty());

        // Act
        boolean result = reviewReactionService.hasUserReacted(testReview.getId(), testUser.getId());

        // Assert
        assertFalse(result);
    }

    @Test
    void getReactionCountsByReview_ShouldReturnCounts() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "helpful")).thenReturn(5L);
        when(reviewReactionRepository.countByReviewAndReactionType(testReview, "not_helpful")).thenReturn(2L);

        // Act
        Map<String, Long> result = reviewReactionService.getReactionCountsByReview(testReview.getId());

        // Assert
        assertEquals(2, result.size());
        assertEquals(5L, result.get("helpful"));
        assertEquals(2L, result.get("not_helpful"));
    }

    @Test
    void addReaction_ShouldThrowException_WhenReviewNotFound() {
        // Arrange
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.addReaction(999L, testUser.getId(), reactionRequest);
        });
    }

    @Test
    void addReaction_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.addReaction(testReview.getId(), 999L, reactionRequest);
        });
    }

    @Test
    void updateReaction_ShouldThrowException_WhenReactionNotFound() {
        // Arrange
        when(reviewReactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.updateReaction(999L, reactionRequest);
        });
    }

    @Test
    void removeReaction_ShouldThrowException_WhenReactionNotFound() {
        // Arrange
        when(reviewReactionRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReactionService.removeReaction(999L);
        });
    }
} 