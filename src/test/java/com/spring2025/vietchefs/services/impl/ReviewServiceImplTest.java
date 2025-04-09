package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCreateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewDetailRepository reviewDetailRepository;

    @Mock
    private ReviewCriteriaService reviewCriteriaService;

    @Mock
    private ReviewReactionService reviewReactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private ImageRepository imageRepository;
    
    @Mock
    private ImageService imageService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Chef testChef;
    private Booking testBooking;
    private Review testReview;
    private ReviewCriteriaResponse testCriteria;
    private ReviewCreateRequest createRequest;
    private ReviewUpdateRequest updateRequest;
    private Map<Long, BigDecimal> criteriaRatings;
    private Map<Long, String> criteriaComments;
    private Map<String, Long> reactionCounts;
    private MockMultipartFile testMainImage;
    private MockMultipartFile testAdditionalImage;
    private List<MultipartFile> additionalImages;
    private Image testImage;
    private List<Image> testImages;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");

        // Set up chef
        testChef = new Chef();
        testChef.setId(2L);
        User chefUser = new User();
        chefUser.setId(3L);
        testChef.setUser(chefUser);

        // Set up booking
        testBooking = new Booking();
        testBooking.setId(4L);
        testBooking.setStatus("completed");

        // Set up review
        testReview = new Review();
        testReview.setId(5L);
        testReview.setUser(testUser);
        testReview.setChef(testChef);
        testReview.setBooking(testBooking);
        testReview.setRating(new BigDecimal("4.5"));
        testReview.setDescription("Test review");
        testReview.setOverallExperience("Great experience");
        testReview.setImageUrl("http://example.com/images/main-review-image.jpg");
        testReview.setIsVerified(true);
        testReview.setCreateAt(LocalDateTime.now());
        testReview.setIsDeleted(false);

        // Set up criteria
        testCriteria = new ReviewCriteriaResponse(
                1L,
                "Food Taste",
                "Quality of the food taste",
                new BigDecimal("0.3"),
                true,
                1
        );

        // Set up request objects
        criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5"));

        criteriaComments = new HashMap<>();
        criteriaComments.put(1L, "Excellent taste");
        
        // Set up mock multipart files
        testMainImage = new MockMultipartFile(
                "mainImage",
                "main-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        
        testAdditionalImage = new MockMultipartFile(
                "additionalImage",
                "additional-image.jpg",
                "image/jpeg",
                "test additional image content".getBytes()
        );
        
        additionalImages = new ArrayList<>();
        additionalImages.add(testAdditionalImage);
        
        // Set up mock images
        testImage = new Image();
        testImage.setId(1L);
        testImage.setImageUrl("http://example.com/images/additional-image.jpg");
        testImage.setEntityType("REVIEW");
        testImage.setEntityId(5L);
        
        testImages = new ArrayList<>();
        testImages.add(testImage);

        createRequest = new ReviewCreateRequest();
        createRequest.setChefId(testChef.getId());
        createRequest.setBookingId(testBooking.getId());
        createRequest.setDescription("Test review");
        createRequest.setOverallExperience("Great experience");
        createRequest.setMainImage(testMainImage);
        createRequest.setAdditionalImages(additionalImages);
        createRequest.setCriteriaRatings(criteriaRatings);
        createRequest.setCriteriaComments(criteriaComments);

        updateRequest = new ReviewUpdateRequest();
        updateRequest.setDescription("Updated review");
        updateRequest.setOverallExperience("Updated experience");
        updateRequest.setMainImage(testMainImage);
        updateRequest.setAdditionalImages(additionalImages);
        updateRequest.setImagesToDelete(Collections.singletonList(1L));
        updateRequest.setCriteriaRatings(criteriaRatings);
        updateRequest.setCriteriaComments(criteriaComments);

        // Set up reaction counts
        reactionCounts = new HashMap<>();
        reactionCounts.put("helpful", 5L);
        reactionCounts.put("not_helpful", 1L);
    }

    @Test
    void getReviewById_ShouldReturnReview_WhenReviewExists() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(imageRepository.findByEntityTypeAndEntityId("REVIEW", testReview.getId())).thenReturn(testImages);
        when(reviewReactionService.getReactionCountsByReview(testReview.getId())).thenReturn(reactionCounts);

        // Act
        ReviewResponse result = reviewService.getReviewById(testReview.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testReview.getId(), result.getId());
        assertEquals(testReview.getDescription(), result.getDescription());
        assertEquals(testReview.getOverallExperience(), result.getOverallExperience());
        assertEquals(testReview.getImageUrl(), result.getMainImageUrl());
        assertNotNull(result.getAdditionalImageUrls());
        assertEquals(1, result.getAdditionalImageUrls().size());
        assertEquals(testReview.getIsVerified(), result.getVerified());
        assertEquals(reactionCounts, result.getReactionCounts());
    }

    @Test
    void getReviewById_ShouldThrowException_WhenReviewNotFound() {
        // Arrange
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewById(999L);
        });
    }

    @Test
    void createReview_ShouldReturnCreatedReview() throws IOException {
        // Arrange
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(bookingRepository.findById(testBooking.getId())).thenReturn(Optional.of(testBooking));
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(testCriteria);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(imageService.uploadImage(any(MultipartFile.class), anyLong(), eq("REVIEW")))
                .thenReturn("http://example.com/images/test-image.jpg");
        when(imageRepository.findByEntityTypeAndEntityId("REVIEW", testReview.getId())).thenReturn(testImages);
        when(reviewReactionService.getReactionCountsByReview(anyLong())).thenReturn(reactionCounts);

        // Act
        ReviewResponse result = reviewService.createReview(createRequest, testUser.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testReview.getId(), result.getId());
        verify(reviewRepository, times(2)).save(any(Review.class)); // Initial save and after image upload
        verify(imageService, times(2)).uploadImage(any(MultipartFile.class), anyLong(), eq("REVIEW"));
        verify(reviewDetailRepository, atLeastOnce()).save(any(ReviewDetail.class));
    }

    @Test
    void updateReview_ShouldReturnUpdatedReview() throws IOException {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(testCriteria);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(imageService.uploadImage(any(MultipartFile.class), anyLong(), eq("REVIEW")))
                .thenReturn("http://example.com/images/updated-image.jpg");
        when(imageRepository.findById(1L)).thenReturn(Optional.of(testImage));
        when(imageRepository.findByEntityTypeAndEntityId("REVIEW", testReview.getId())).thenReturn(testImages);
        when(reviewReactionService.getReactionCountsByReview(anyLong())).thenReturn(reactionCounts);

        // Act
        ReviewResponse result = reviewService.updateReview(testReview.getId(), updateRequest, testUser.getId());

        // Assert
        assertNotNull(result);
        verify(reviewRepository).save(any(Review.class));
        verify(imageRepository).delete(testImage);
        verify(imageService, times(2)).uploadImage(any(MultipartFile.class), anyLong(), eq("REVIEW"));
        verify(reviewDetailRepository).deleteAll(anyList());
        verify(reviewDetailRepository, atLeastOnce()).save(any(ReviewDetail.class));
    }

    @Test
    void deleteReview_ShouldMarkReviewAsDeleted() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        
        // Act
        reviewService.deleteReview(testReview.getId());
        
        // Assert
        assertTrue(testReview.getIsDeleted());
        verify(reviewRepository).save(testReview);
    }

    @Test
    void getAverageRatingForChef_ShouldReturnCorrectValue() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.of(new BigDecimal("4.5")));
        
        // Act
        BigDecimal result = reviewService.getAverageRatingForChef(testChef.getId());
        
        // Assert
        assertEquals(new BigDecimal("4.5"), result);
    }

    @Test
    void getReviewCountForChef_ShouldReturnCorrectCount() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(10L);
        
        // Act
        long result = reviewService.getReviewCountForChef(testChef.getId());
        
        // Assert
        assertEquals(10L, result);
    }

    @Test
    void addChefResponse_ShouldUpdateReviewWithResponse() {
        // Arrange
        when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
        when(userRepository.findById(testChef.getUser().getId())).thenReturn(Optional.of(testChef.getUser()));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(imageRepository.findByEntityTypeAndEntityId("REVIEW", testReview.getId())).thenReturn(testImages);
        when(reviewReactionService.getReactionCountsByReview(anyLong())).thenReturn(reactionCounts);
        String responseText = "Thank you for your review";
        
        // Act
        ReviewResponse result = reviewService.addChefResponse(testReview.getId(), responseText, testChef.getUser().getId());
        
        // Assert
        assertNotNull(result);
        assertEquals(responseText, testReview.getResponse());
        assertNotNull(testReview.getChefResponseAt());
        verify(reviewRepository).save(testReview);
    }
} 