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
    private Map<Long, String> criteriaComments;
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
            review.setIsVerified(true);
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
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(expectedCount);

        // Act
        long result = reviewService.getReviewCountForChef(testChef.getId());

        // Assert
        assertEquals(expectedCount, result);
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).countByChefAndVerified(testChef);
    }

    @Test
    void getReviewCountForChef_WhenNoReviews_ShouldReturnZero() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(0L);

        // Act
        long result = reviewService.getReviewCountForChef(testChef.getId());

        // Assert
        assertEquals(0L, result);
        verify(chefRepository).findById(testChef.getId());
        verify(reviewRepository).countByChefAndVerified(testChef);
    }

    @Test
    void getRatingDistributionForChef_ShouldReturnCorrectDistribution() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(10L);
        
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
        verify(reviewRepository).countByChefAndVerified(testChef);
    }

    @Test
    void getRatingDistributionForChef_WhenNoReviews_ShouldReturnZerosForAllStars() {
        // Arrange
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(0L);
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
        verify(reviewRepository).countByChefAndVerified(testChef);
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
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(0L);
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
    void debugEmptyReviewDataTest() {
        // Arrange - Simulate the situation in the screenshot where all data is zero/empty
        System.out.println("\n\n===== DEBUG: STARTING EMPTY REVIEW TEST =====");
        
        // Mock chef repository
        Long testChefId = 99L;
        System.out.println("DEBUG: Setting up chef with ID: " + testChefId);
        when(chefRepository.findById(testChefId)).thenReturn(Optional.of(testChef));
        
        // Mock empty average rating
        System.out.println("DEBUG: Mocking empty average rating");
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.empty());
        
        // Mock zero review count
        System.out.println("DEBUG: Mocking zero review count");
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(0L);
        
        // Mock zero ratings distribution
        System.out.println("DEBUG: Mocking zero rating distribution");
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(any(Chef.class), any(BigDecimal.class))).thenReturn(0L);
        
        // Mock empty page of reviews
        System.out.println("DEBUG: Mocking empty page of reviews");
        Page<Review> emptyPage = new PageImpl<>(Collections.emptyList());
        Pageable pageable = Pageable.unpaged();
        when(reviewRepository.findByChefAndIsDeletedFalse(eq(testChef), any(Pageable.class))).thenReturn(emptyPage);
        
        // Simulate controller's response building
        System.out.println("\nDEBUG: Building response data similar to controller...");
        Map<String, Object> response = new HashMap<>();
        
        // Get reviews
        System.out.println("DEBUG: Calling getReviewsByChef()");
        Page<ReviewResponse> reviewPage = reviewService.getReviewsByChef(testChefId, pageable);
        
        // Build response map like the controller does
        System.out.println("DEBUG: Adding reviews to response");
        response.put("reviews", reviewPage.getContent());
        System.out.println("DEBUG: reviews = " + reviewPage.getContent());
        
        System.out.println("DEBUG: Adding pagination data");
        response.put("currentPage", reviewPage.getNumber());
        System.out.println("DEBUG: currentPage = " + reviewPage.getNumber());
        
        response.put("totalItems", reviewPage.getTotalElements());
        System.out.println("DEBUG: totalItems = " + reviewPage.getTotalElements());
        
        response.put("totalPages", reviewPage.getTotalPages());
        System.out.println("DEBUG: totalPages = " + reviewPage.getTotalPages());
        
        System.out.println("DEBUG: Getting average rating");
        BigDecimal averageRating = reviewService.getAverageRatingForChef(testChefId);
        response.put("averageRating", averageRating);
        System.out.println("DEBUG: averageRating = " + averageRating);
        
        System.out.println("DEBUG: Getting total reviews");
        long totalReviews = reviewService.getReviewCountForChef(testChefId);
        response.put("totalReviews", totalReviews);
        System.out.println("DEBUG: totalReviews = " + totalReviews);
        
        System.out.println("DEBUG: Getting rating distribution");
        Map<String, Long> distribution = reviewService.getRatingDistributionForChef(testChefId);
        response.put("ratingDistribution", distribution);
        System.out.println("DEBUG: ratingDistribution = " + distribution);
        
        // Print final response JSON-like format for visual inspection
        System.out.println("\nDEBUG: Final response object:");
        System.out.println("{\n" +
                "  \"totalItems\": " + response.get("totalItems") + ",\n" +
                "  \"totalReviews\": " + response.get("totalReviews") + ",\n" +
                "  \"ratingDistribution\": " + response.get("ratingDistribution") + ",\n" +
                "  \"reviews\": [");
        
        // Format each review with proper indentation and line breaks
        @SuppressWarnings("unchecked")
        List<ReviewResponse> emptyReviews = (List<ReviewResponse>) response.get("reviews");
        for (int i = 0; i < emptyReviews.size(); i++) {
            ReviewResponse review = emptyReviews.get(i);
            System.out.println("    {\n" +
                    "      \"id\": " + review.getId() + ",\n" +
                    "      \"userId\": " + review.getUserId() + ",\n" +
                    "      \"userName\": \"" + review.getUserName() + "\",\n" +
                    "      \"chefId\": " + review.getChefId() + ",\n" +
                    "      \"bookingId\": " + review.getBookingId() + ",\n" +
                    "      \"rating\": " + review.getRating() + ",\n" +
                    "      \"description\": \"" + review.getDescription() + "\",\n" +
                    "      \"overallExperience\": \"" + review.getOverallExperience() + "\",\n" +
                    "      \"mainImageUrl\": \"" + review.getMainImageUrl() + "\",\n" +
                    "      \"additionalImageUrls\": " + review.getAdditionalImageUrls() + ",\n" +
                    "      \"verified\": " + review.getVerified() + ",\n" +
                    "      \"response\": \"" + review.getResponse() + "\",\n" +
                    "      \"chefResponseAt\": \"" + review.getChefResponseAt() + "\",\n" +
                    "      \"createAt\": \"" + review.getCreateAt() + "\",\n" +
                    "      \"reactionCounts\": " + review.getReactionCounts() + "\n" +
                    "    }" + (i < emptyReviews.size() - 1 ? "," : ""));
        }
        
        System.out.println("  ],\n" +
                "  \"averageRating\": " + response.get("averageRating") + ",\n" +
                "  \"currentPage\": " + response.get("currentPage") + ",\n" +
                "  \"totalPages\": " + response.get("totalPages") + "\n" +
                "}");
        
        System.out.println("===== DEBUG: END OF TEST =====\n\n");
        
        // Assert all values are zero/empty as expected
        assertEquals(BigDecimal.ZERO, averageRating);
        assertEquals(0L, totalReviews);
        assertEquals(0L, distribution.get("5-star"));
        assertEquals(0L, distribution.get("4-star"));
        assertEquals(0L, distribution.get("3-star"));
        assertEquals(0L, distribution.get("2-star"));
        assertEquals(0L, distribution.get("1-star"));
        assertTrue(reviewPage.getContent().isEmpty());
        
        // No need to verify exact call counts since methods may be called internally
    }

    @Test
    void debugReviewDataWithValues() {
        // Arrange - Simulate the situation where we have reviews 
        System.out.println("\n\n===== DEBUG: STARTING REVIEW TEST WITH VALUES =====");
        
        // Chuẩn bị dữ liệu đầy đủ cho các review
        List<Review> detailedReviews = new ArrayList<>();
        
        // Tạo review 1 - đầy đủ thông tin
        Review review1 = new Review();
        review1.setId(1L);
        review1.setChef(testChef);
        review1.setUser(testUser);
        review1.setBooking(testBooking);
        review1.setRating(new BigDecimal("5.0"));
        review1.setDescription("Bữa ăn tuyệt vời và dịch vụ chuyên nghiệp!");
        review1.setOverallExperience("Một trong những trải nghiệm ẩm thực tốt nhất tôi từng có");
        review1.setImageUrl("http://example.com/images/review1.jpg");
        review1.setResponse("Cảm ơn bạn đã đánh giá tích cực. Chúng tôi rất vui khi bạn đã có trải nghiệm tuyệt vời!");
        review1.setChefResponseAt(LocalDateTime.now().minusHours(2));
        review1.setCreateAt(LocalDateTime.now().minusDays(1));
        review1.setIsDeleted(false);
        review1.setIsVerified(true);
        detailedReviews.add(review1);
        
        // Tạo review 2
        Review review2 = new Review();
        review2.setId(2L);
        review2.setChef(testChef);
        review2.setUser(testUser);
        review2.setBooking(testBooking);
        review2.setRating(new BigDecimal("4.0"));
        review2.setDescription("Món ăn ngon, trình bày đẹp mắt");
        review2.setOverallExperience("Trải nghiệm thú vị, sẽ giới thiệu cho bạn bè");
        review2.setImageUrl("http://example.com/images/review2.jpg");
        review2.setResponse("Chúng tôi đánh giá cao phản hồi của bạn và hy vọng sẽ gặp lại bạn sớm!");
        review2.setChefResponseAt(LocalDateTime.now().minusHours(5));
        review2.setCreateAt(LocalDateTime.now().minusDays(3));
        review2.setIsDeleted(false);
        review2.setIsVerified(true);
        detailedReviews.add(review2);
        
        // Chuẩn bị các hình ảnh bổ sung cho mỗi review
        List<Image> review1Images = new ArrayList<>();
        Image image1 = new Image();
        image1.setId(1L);
        image1.setEntityId(1L);
        image1.setEntityType("REVIEW");
        image1.setImageUrl("http://example.com/images/review1-additional1.jpg");
        
        Image image2 = new Image();
        image2.setId(2L);
        image2.setEntityId(1L);
        image2.setEntityType("REVIEW");
        image2.setImageUrl("http://example.com/images/review1-additional2.jpg");
        
        review1Images.add(image1);
        review1Images.add(image2);
        
        // Chuẩn bị các phản ứng
        Map<String, Long> review1Reactions = new HashMap<>();
        review1Reactions.put("helpful", 15L);
        review1Reactions.put("not_helpful", 2L);
        
        Map<String, Long> review2Reactions = new HashMap<>();
        review2Reactions.put("helpful", 8L);
        review2Reactions.put("not_helpful", 1L);
        
        // Mock chef repository
        Long testChefId = 99L;
        System.out.println("DEBUG: Setting up chef with ID: " + testChefId);
        when(chefRepository.findById(testChefId)).thenReturn(Optional.of(testChef));
        
        // Mock average rating with a real value
        BigDecimal expectedAverage = new BigDecimal("4.5");
        System.out.println("DEBUG: Mocking average rating: " + expectedAverage);
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.of(expectedAverage));
        
        // Mock review count
        long expectedCount = 10L;
        System.out.println("DEBUG: Mocking review count: " + expectedCount);
        when(reviewRepository.countByChefAndVerified(testChef)).thenReturn(expectedCount);
        
        // Mock ratings distribution
        System.out.println("DEBUG: Mocking rating distribution");
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("4.5")))).thenReturn(6L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("3.5")))).thenReturn(8L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("2.5")))).thenReturn(9L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("1.5")))).thenReturn(10L);
        
        // Mock page of reviews - sử dụng danh sách reviews đầy đủ thông tin
        System.out.println("DEBUG: Mocking page of reviews");
        Pageable pageable = Pageable.unpaged();
        when(reviewRepository.findByChefAndIsDeletedFalse(eq(testChef), any(Pageable.class)))
            .thenReturn(new PageImpl<>(detailedReviews));
            
        // Mock thông tin hình ảnh bổ sung
        when(imageRepository.findByEntityTypeAndEntityId(eq("REVIEW"), eq(1L))).thenReturn(review1Images);
        when(imageRepository.findByEntityTypeAndEntityId(eq("REVIEW"), eq(2L))).thenReturn(Collections.emptyList());
        
        // Mock thông tin reactions
        when(reviewReactionService.getReactionCountsByReview(eq(1L))).thenReturn(review1Reactions);
        when(reviewReactionService.getReactionCountsByReview(eq(2L))).thenReturn(review2Reactions);
        
        // Simulate controller's response building
        System.out.println("\nDEBUG: Building response data similar to controller...");
        Map<String, Object> response = new HashMap<>();
        
        // Get reviews
        System.out.println("DEBUG: Calling getReviewsByChef()");
        Page<ReviewResponse> reviewPage = reviewService.getReviewsByChef(testChefId, pageable);
        
        // Kiểm tra xem dữ liệu review có đầy đủ không
        System.out.println("DEBUG: Reviewing data completeness");
        reviewPage.getContent().forEach(rev -> {
            System.out.println("Review ID: " + rev.getId());
            System.out.println("  - Description: " + rev.getDescription());
            System.out.println("  - Experience: " + rev.getOverallExperience());
            System.out.println("  - Main Image: " + rev.getMainImageUrl());
            System.out.println("  - Additional Images: " + rev.getAdditionalImageUrls());
            System.out.println("  - Verified: " + rev.getVerified());
            System.out.println("  - Response: " + rev.getResponse());
            System.out.println("  - Chef Response At: " + rev.getChefResponseAt());
            System.out.println("  - Reactions: " + rev.getReactionCounts());
        });
        
        // Build response map like the controller does
        System.out.println("DEBUG: Adding reviews to response - count: " + reviewPage.getContent().size());
        response.put("reviews", reviewPage.getContent());
        
        System.out.println("DEBUG: Adding pagination data");
        response.put("currentPage", reviewPage.getNumber());
        System.out.println("DEBUG: currentPage = " + reviewPage.getNumber());
        
        response.put("totalItems", reviewPage.getTotalElements());
        System.out.println("DEBUG: totalItems = " + reviewPage.getTotalElements());
        
        response.put("totalPages", reviewPage.getTotalPages());
        System.out.println("DEBUG: totalPages = " + reviewPage.getTotalPages());
        
        System.out.println("DEBUG: Getting average rating");
        BigDecimal averageRating = reviewService.getAverageRatingForChef(testChefId);
        response.put("averageRating", averageRating);
        System.out.println("DEBUG: averageRating = " + averageRating);
        
        System.out.println("DEBUG: Getting total reviews");
        long totalReviews = reviewService.getReviewCountForChef(testChefId);
        response.put("totalReviews", totalReviews);
        System.out.println("DEBUG: totalReviews = " + totalReviews);
        
        System.out.println("DEBUG: Getting rating distribution");
        Map<String, Long> distribution = reviewService.getRatingDistributionForChef(testChefId);
        response.put("ratingDistribution", distribution);
        System.out.println("DEBUG: ratingDistribution = " + distribution);
        
        // Print final response JSON-like format for visual inspection
        System.out.println("\nDEBUG: Final response object:");
        System.out.println("{\n" +
                "  \"totalItems\": " + response.get("totalItems") + ",\n" +
                "  \"totalReviews\": " + response.get("totalReviews") + ",\n" +
                "  \"ratingDistribution\": " + response.get("ratingDistribution") + ",\n" +
                "  \"reviews\": [");
        
        // Format each review with proper indentation and line breaks
        @SuppressWarnings("unchecked")
        List<ReviewResponse> detailedReviewResponses = (List<ReviewResponse>) response.get("reviews");
        for (int i = 0; i < detailedReviewResponses.size(); i++) {
            ReviewResponse review = detailedReviewResponses.get(i);
            System.out.println("    {\n" +
                    "      \"id\": " + review.getId() + ",\n" +
                    "      \"userId\": " + review.getUserId() + ",\n" +
                    "      \"userName\": \"" + review.getUserName() + "\",\n" +
                    "      \"chefId\": " + review.getChefId() + ",\n" +
                    "      \"bookingId\": " + review.getBookingId() + ",\n" +
                    "      \"rating\": " + review.getRating() + ",\n" +
                    "      \"description\": \"" + review.getDescription() + "\",\n" +
                    "      \"overallExperience\": \"" + review.getOverallExperience() + "\",\n" +
                    "      \"mainImageUrl\": \"" + review.getMainImageUrl() + "\",\n" +
                    "      \"additionalImageUrls\": " + review.getAdditionalImageUrls() + ",\n" +
                    "      \"verified\": " + review.getVerified() + ",\n" +
                    "      \"response\": \"" + review.getResponse() + "\",\n" +
                    "      \"chefResponseAt\": \"" + review.getChefResponseAt() + "\",\n" +
                    "      \"createAt\": \"" + review.getCreateAt() + "\",\n" +
                    "      \"reactionCounts\": " + review.getReactionCounts() + "\n" +
                    "    }" + (i < detailedReviewResponses.size() - 1 ? "," : ""));
        }
        
        System.out.println("  ],\n" +
                "  \"averageRating\": " + response.get("averageRating") + ",\n" +
                "  \"currentPage\": " + response.get("currentPage") + ",\n" +
                "  \"totalPages\": " + response.get("totalPages") + "\n" +
                "}");
        
        System.out.println("===== DEBUG: END OF TEST =====\n\n");
        
        // Assert values match what we expect
        assertEquals(expectedAverage, averageRating);
        assertEquals(expectedCount, totalReviews);
        assertEquals(6L, distribution.get("5-star")); // 6
        assertEquals(2L, distribution.get("4-star")); // 8 - 6 = 2
        assertEquals(1L, distribution.get("3-star")); // 9 - 8 = 1
        assertEquals(1L, distribution.get("2-star")); // 10 - 9 = 1
        assertEquals(0L, distribution.get("1-star")); // 10 - 10 = 0
        assertEquals(detailedReviews.size(), reviewPage.getContent().size());
        
        // Assert that review responses contain all expected fields
        ReviewResponse review1Response = reviewPage.getContent().get(0);
        assertNotNull(review1Response.getId());
        assertNotNull(review1Response.getUserId());
        assertNotNull(review1Response.getUserName());
        assertNotNull(review1Response.getChefId());
        assertNotNull(review1Response.getBookingId());
        assertNotNull(review1Response.getRating());
        assertNotNull(review1Response.getDescription());
        assertNotNull(review1Response.getOverallExperience());
        assertNotNull(review1Response.getMainImageUrl());
        assertNotNull(review1Response.getAdditionalImageUrls());
        assertNotNull(review1Response.getVerified());
        assertNotNull(review1Response.getResponse());
        assertNotNull(review1Response.getChefResponseAt());
        assertNotNull(review1Response.getCreateAt());
        assertNotNull(review1Response.getReactionCounts());
        
        // Kiểm tra giá trị cụ thể
        assertEquals("Bữa ăn tuyệt vời và dịch vụ chuyên nghiệp!", review1Response.getDescription());
        assertEquals("Một trong những trải nghiệm ẩm thực tốt nhất tôi từng có", review1Response.getOverallExperience());
        assertEquals("http://example.com/images/review1.jpg", review1Response.getMainImageUrl());
        assertEquals(2, review1Response.getAdditionalImageUrls().size());
        assertTrue(review1Response.getVerified());
        assertEquals("Cảm ơn bạn đã đánh giá tích cực. Chúng tôi rất vui khi bạn đã có trải nghiệm tuyệt vời!", review1Response.getResponse());
        assertEquals(15L, review1Response.getReactionCounts().get("helpful"));
    }
} 