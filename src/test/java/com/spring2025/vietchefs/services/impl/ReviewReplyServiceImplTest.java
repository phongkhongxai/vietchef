package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReply;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReplyRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReplyResponse;
import com.spring2025.vietchefs.repositories.ReviewReplyRepository;
import com.spring2025.vietchefs.repositories.ReviewRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewReplyServiceImplTest {

    @Mock
    private ReviewReplyRepository reviewReplyRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewReplyServiceImpl reviewReplyService;

    private Review testReview;
    private User testUser;
    private ReviewReply testReply;
    private ReviewReplyRequest replyRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test User");
        testUser.setAvatarUrl("user.jpg");

        testReview = new Review();
        testReview.setId(2L);

        testReply = new ReviewReply();
        testReply.setReplyId(3L);
        testReply.setReview(testReview);
        testReply.setUser(testUser);
        testReply.setContent("This is a test reply");
        testReply.setCreatedAt(LocalDateTime.now());
        testReply.setIsDeleted(false);

        replyRequest = new ReviewReplyRequest();
        replyRequest.setContent("New reply content");
    }

    @Test
    void addReply_ShouldReturnCreatedReply() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(testReply);

        // Act
        ReviewReplyResponse result = reviewReplyService.addReply(testReview.getId(), testUser.getId(), replyRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testReply.getReplyId(), result.getReplyId());
        assertEquals(testReply.getContent(), result.getContent());
        assertEquals(testUser.getId(), result.getUserId());
        verify(reviewReplyRepository).save(any(ReviewReply.class));
    }

    @Test
    void updateReply_ShouldReturnUpdatedReply() {
        // Arrange
        when(reviewReplyRepository.findById(testReply.getReplyId())).thenReturn(Optional.of(testReply));
        when(reviewReplyRepository.save(any(ReviewReply.class))).thenReturn(testReply);

        // Act
        ReviewReplyResponse result = reviewReplyService.updateReply(testReply.getReplyId(), replyRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testReply.getReplyId(), result.getReplyId());
        assertEquals(replyRequest.getContent(), testReply.getContent());
        verify(reviewReplyRepository).save(testReply);
    }

    @Test
    void deleteReply_ShouldMarkReplyAsDeleted() {
        // Arrange
        when(reviewReplyRepository.findById(testReply.getReplyId())).thenReturn(Optional.of(testReply));

        // Act
        reviewReplyService.deleteReply(testReply.getReplyId());

        // Assert
        assertTrue(testReply.getIsDeleted());
        verify(reviewReplyRepository).save(testReply);
    }

    @Test
    void getRepliesByReview_ShouldReturnListOfReplies() {
        // Arrange
        List<ReviewReply> replies = Arrays.asList(testReply);
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(reviewReplyRepository.findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(testReview))
                .thenReturn(replies);

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByReview(testReview.getId());

        // Assert
        assertEquals(1, result.size());
        assertEquals(testReply.getReplyId(), result.get(0).getReplyId());
        assertEquals(testReply.getContent(), result.get(0).getContent());
    }

    @Test
    void getRepliesByUser_ShouldReturnListOfReplies() {
        // Arrange
        List<ReviewReply> replies = Arrays.asList(testReply);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reviewReplyRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(testUser))
                .thenReturn(replies);

        // Act
        List<ReviewReplyResponse> result = reviewReplyService.getRepliesByUser(testUser.getId());

        // Assert
        assertEquals(1, result.size());
        assertEquals(testReply.getReplyId(), result.get(0).getReplyId());
        assertEquals(testReply.getContent(), result.get(0).getContent());
    }

    @Test
    void addReply_ShouldThrowException_WhenReviewNotFound() {
        // Arrange
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.addReply(999L, testUser.getId(), replyRequest);
        });
    }

    @Test
    void addReply_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.addReply(testReview.getId(), 999L, replyRequest);
        });
    }

    @Test
    void updateReply_ShouldThrowException_WhenReplyNotFound() {
        // Arrange
        when(reviewReplyRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.updateReply(999L, replyRequest);
        });
    }

    @Test
    void deleteReply_ShouldThrowException_WhenReplyNotFound() {
        // Arrange
        when(reviewReplyRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewReplyService.deleteReply(999L);
        });
    }
} 