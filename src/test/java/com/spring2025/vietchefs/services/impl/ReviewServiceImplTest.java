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
import java.math.RoundingMode;
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
    private Map<String, Long> reactionCounts;
    private MockMultipartFile testMainImage;
    private MockMultipartFile testAdditionalImage;
    private List<MultipartFile> additionalImages;
    private Image testImage;
    private List<Image> testImages;
    private List<Review> reviewList;

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

        updateRequest = new ReviewUpdateRequest();
        updateRequest.setDescription("Updated review");
        updateRequest.setOverallExperience("Updated experience");
        updateRequest.setMainImage(testMainImage);
        updateRequest.setAdditionalImages(additionalImages);
        updateRequest.setImagesToDelete(Collections.singletonList(1L));
        updateRequest.setCriteriaRatings(criteriaRatings);

        // Set up reaction counts
        reactionCounts = new HashMap<>();
        reactionCounts.put("helpful", 5L);
        reactionCounts.put("not_helpful", 1L);

        // Create test reviews
        reviewList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Review review = new Review();
            review.setId((long) i);
            review.setChef(testChef);
            review.setUser(testUser);
            review.setRating(new BigDecimal(i));
            review.setDescription("Test review " + i);
            review.setCreateAt(LocalDateTime.now());
            review.setIsDeleted(false);
            reviewList.add(review);
        }
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
        BigDecimal expectedAverage = new BigDecimal("3.0");
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.of(expectedAverage));

        // Act
        BigDecimal result = reviewService.getAverageRatingForChef(testChef.getId());

        // Assert
        assertEquals(expectedAverage, result);
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).findAverageRatingByChef(testChef);
    }

    @Test
    void getAverageRatingForChef_WhenNoReviews_ShouldReturnZero() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.empty());

        // Act
        BigDecimal result = reviewService.getAverageRatingForChef(testChef.getId());

        // Assert
        assertEquals(BigDecimal.ZERO, result);
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).findAverageRatingByChef(testChef);
    }

    @Test
    void getAverageRatingForChef_WhenChefNotFound_ShouldThrowException() {
        // Arrange
        Long nonExistentChefId = 999L;
        when(chefRepository.findById(nonExistentChefId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getAverageRatingForChef(nonExistentChefId);
        });
        verify(chefRepository).findById(nonExistentChefId);
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void getReviewCountForChef_ShouldReturnCorrectCount() {
        // Arrange
        long expectedCount = 5L;
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChef(testChef)).thenReturn(expectedCount);

        // Act
        long result = reviewService.getReviewCountForChef(testChef.getId());

        // Assert
        assertEquals(expectedCount, result);
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).countByChef(testChef);
    }

    @Test
    void getReviewCountForChef_WhenNoReviews_ShouldReturnZero() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChef(testChef)).thenReturn(0L);

        // Act
        long result = reviewService.getReviewCountForChef(testChef.getId());

        // Assert
        assertEquals(0L, result);
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).countByChef(testChef);
    }

    @Test
    void getRatingDistributionForChef_ShouldReturnCorrectDistribution() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChef(testChef)).thenReturn(10L);
        
        // Mock star distribution counts
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("4.5")))).thenReturn(2L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("3.5")))).thenReturn(5L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("2.5")))).thenReturn(7L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("1.5")))).thenReturn(9L);

        // Act
        Map<String, Long> distribution = reviewService.getRatingDistributionForChef(testChef.getId());

        // Assert
        assertEquals(2L, distribution.get("5-star"));
        assertEquals(3L, distribution.get("4-star"));  // 5 - 2 = 3
        assertEquals(2L, distribution.get("3-star"));  // 7 - 5 = 2
        assertEquals(2L, distribution.get("2-star"));  // 9 - 7 = 2
        assertEquals(1L, distribution.get("1-star"));  // 10 - 9 = 1
        
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).countByChef(testChef);
    }

    @Test
    void getRatingDistributionForChef_WhenNoReviews_ShouldReturnZerosForAllStars() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChef(testChef)).thenReturn(0L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(any(Chef.class), any(BigDecimal.class))).thenReturn(0L);

        // Act
        Map<String, Long> distribution = reviewService.getRatingDistributionForChef(testChef.getId());

        // Assert
        assertEquals(0L, distribution.get("5-star"));
        assertEquals(0L, distribution.get("4-star"));
        assertEquals(0L, distribution.get("3-star"));
        assertEquals(0L, distribution.get("2-star"));
        assertEquals(0L, distribution.get("1-star"));
        
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).countByChef(testChef);
    }

    @Test
    void getReviewsByChef_ShouldReturnPageOfReviews() {
        // Arrange
        Page<Review> reviewPage = new PageImpl<>(reviewList);
        Pageable pageable = Pageable.unpaged();
        
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.findByChefAndIsDeletedFalse(eq(testChef), any(Pageable.class))).thenReturn(reviewPage);
        when(reviewReactionService.getReactionCountsByReview(anyLong())).thenReturn(Collections.emptyMap());

        // Act
        Page<ReviewResponse> result = reviewService.getReviewsByChef(testChef.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(reviewList.size(), result.getContent().size());
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).findByChefAndIsDeletedFalse(eq(testChef), any(Pageable.class));
        verify(reviewReactionService, times(reviewList.size())).getReactionCountsByReview(anyLong());
    }

    @Test
    void calculateWeightedRating_ShouldReturnCorrectValue() {
        // Arrange
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5"));
        criteriaRatings.put(2L, new BigDecimal("3.0"));
        criteriaRatings.put(3L, new BigDecimal("5.0"));
        
        ReviewCriteriaResponse criteria1 = new ReviewCriteriaResponse();
        criteria1.setCriteriaId(1L);
        criteria1.setWeight(new BigDecimal("0.4"));
        criteria1.setIsActive(true);
        
        ReviewCriteriaResponse criteria2 = new ReviewCriteriaResponse();
        criteria2.setCriteriaId(2L);
        criteria2.setWeight(new BigDecimal("0.3"));
        criteria2.setIsActive(true);
        
        ReviewCriteriaResponse criteria3 = new ReviewCriteriaResponse();
        criteria3.setCriteriaId(3L);
        criteria3.setWeight(new BigDecimal("0.3"));
        criteria3.setIsActive(true);
        
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(criteria1);
        when(reviewCriteriaService.getCriteriaById(2L)).thenReturn(criteria2);
        when(reviewCriteriaService.getCriteriaById(3L)).thenReturn(criteria3);
        
        // Expected: (4.5 * 0.4) + (3.0 * 0.3) + (5.0 * 0.3) = 1.8 + 0.9 + 1.5 = 4.2
        BigDecimal expected = new BigDecimal("4.20");
        
        // Act
        BigDecimal result = reviewService.calculateWeightedRating(criteriaRatings);
        
        // Assert
        assertEquals(expected, result);
        verify(reviewCriteriaService, times(3)).getCriteriaById(anyLong());
    }

    @Test
    void calculateWeightedRating_ShouldSkipZeroRatings() {
        // Arrange
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5"));
        criteriaRatings.put(2L, BigDecimal.ZERO);  // This should be skipped
        criteriaRatings.put(3L, new BigDecimal("5.0"));
        
        ReviewCriteriaResponse criteria1 = new ReviewCriteriaResponse();
        criteria1.setCriteriaId(1L);
        criteria1.setWeight(new BigDecimal("0.4"));
        criteria1.setIsActive(true);
        
        ReviewCriteriaResponse criteria2 = new ReviewCriteriaResponse();
        criteria2.setCriteriaId(2L);
        criteria2.setWeight(new BigDecimal("0.3"));
        criteria2.setIsActive(true);
        
        ReviewCriteriaResponse criteria3 = new ReviewCriteriaResponse();
        criteria3.setCriteriaId(3L);
        criteria3.setWeight(new BigDecimal("0.3"));
        criteria3.setIsActive(true);
        
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(criteria1);
        when(reviewCriteriaService.getCriteriaById(2L)).thenReturn(criteria2);
        when(reviewCriteriaService.getCriteriaById(3L)).thenReturn(criteria3);
        
        // Expected: (4.5 * 0.4) + (5.0 * 0.3) / (0.4 + 0.3) = (1.8 + 1.5) / 0.7 = 3.3 / 0.7 = 4.71 (rounded to 4.71)
        BigDecimal expected = new BigDecimal("4.71");
        
        // Act
        BigDecimal result = reviewService.calculateWeightedRating(criteriaRatings);
        
        // Assert
        assertEquals(expected, result);
        verify(reviewCriteriaService, times(3)).getCriteriaById(anyLong());
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

    @Test
    void whenNoReviews_ReturnZeroInAllRatingData() {
        // Arrange - simulate the situation in the screenshot where all data is zero/empty
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.empty());
        when(reviewRepository.countByChef(testChef)).thenReturn(0L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(any(Chef.class), any(BigDecimal.class))).thenReturn(0L);
        
        // Mock an empty page of reviews
        Page<Review> emptyPage = new PageImpl<>(Collections.emptyList());
        Pageable pageable = Pageable.unpaged();
        when(reviewRepository.findByChefAndIsDeletedFalse(eq(testChef), any(Pageable.class))).thenReturn(emptyPage);
        
        // Act - simulate the controller's response
        BigDecimal averageRating = reviewService.getAverageRatingForChef(testChef.getId());
        long totalReviews = reviewService.getReviewCountForChef(testChef.getId());
        Map<String, Long> distribution = reviewService.getRatingDistributionForChef(testChef.getId());
        Page<ReviewResponse> reviews = reviewService.getReviewsByChef(testChef.getId(), pageable);
        
        // Assert - verify we get zeros for everything, matching the screenshot
        assertEquals(BigDecimal.ZERO, averageRating);
        assertEquals(0L, totalReviews);
        assertEquals(0L, distribution.get("5-star"));
        assertEquals(0L, distribution.get("4-star"));
        assertEquals(0L, distribution.get("3-star"));
        assertEquals(0L, distribution.get("2-star"));
        assertEquals(0L, distribution.get("1-star"));
        assertTrue(reviews.getContent().isEmpty());
    }

    @Test
    void getReviewCountForChef_ShouldMatchTotalReviewsFromPageResponse() {
        // Arrange
        long expectedCount = 5L;
        Page<Review> reviewPage = new PageImpl<>(reviewList);
        Pageable pageable = Pageable.unpaged();
        
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.findByChefAndIsDeletedFalse(eq(testChef), any(Pageable.class))).thenReturn(reviewPage);
        when(reviewRepository.countByChef(testChef)).thenReturn(expectedCount);
        when(reviewReactionService.getReactionCountsByReview(anyLong())).thenReturn(Collections.emptyMap());

        // Act
        long countResult = reviewService.getReviewCountForChef(testChef.getId());
        Page<ReviewResponse> pageResult = reviewService.getReviewsByChef(testChef.getId(), pageable);

        // Assert
        assertEquals(expectedCount, countResult);
        assertEquals(reviewList.size(), pageResult.getTotalElements());
        
        // The two values should be equal (or at least consistent in their logic)
        // This might fail if your application logic for these values is different
        // If this test fails, it indicates there's an inconsistency between totalReviews and totalItems
        assertEquals(expectedCount, pageResult.getTotalElements());
        
        verify(chefRepository, times(2)).findById(testChef.getId());
        verify(reviewRepository).countByChef(testChef);
        verify(reviewRepository).findByChefAndIsDeletedFalse(eq(testChef), any(Pageable.class));
    }
} 