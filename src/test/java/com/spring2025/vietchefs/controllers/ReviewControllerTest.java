package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
import com.spring2025.vietchefs.services.ReviewReplyService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.RoleService;
import com.spring2025.vietchefs.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private ReviewCriteriaService reviewCriteriaService;

    @Mock
    private ReviewReplyService reviewReplyService;

    @Mock
    private ReviewReactionService reviewReactionService;

    @Mock
    private UserService userService;

    @Mock
    private ChefService chefService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private ReviewController reviewController;

    private User testUser;
    private Chef testChef;
    private ReviewResponse testReviewResponse;
    private Page<ReviewResponse> emptyReviewPage;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test User");

        testChef = new Chef();
        testChef.setId(2L);

        testReviewResponse = new ReviewResponse();
        testReviewResponse.setId(1L);
        testReviewResponse.setUserId(testUser.getId());
        testReviewResponse.setUserName(testUser.getFullName());
        testReviewResponse.setChefId(testChef.getId());
        testReviewResponse.setRating(new BigDecimal("4.5"));
        testReviewResponse.setDescription("Test review description");
        testReviewResponse.setCreateAt(LocalDateTime.now());

        // Create empty page for testing
        emptyReviewPage = new PageImpl<>(Collections.emptyList());
    }

    @Test
    void testEmptyReviewResponseMatchingScreenshot() {
        // Arrange - Set up our mocks for the empty response case
        Long chefId = 1L;
        int page = 0;
        int size = 10;
        String sort = "newest"; // default sort parameter

        // Create pageable object like the controller would
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());

        // Mock the service methods to return empty data
        when(reviewService.getReviewsByChef(eq(chefId), any(Pageable.class))).thenReturn(emptyReviewPage);
        when(reviewService.getAverageRatingForChef(chefId)).thenReturn(BigDecimal.ZERO);
        when(reviewService.getReviewCountForChef(chefId)).thenReturn(0L);
        
        // Mock the rating distribution to return zeroes for all star counts
        Map<String, Long> emptyDistribution = new HashMap<>();
        emptyDistribution.put("1-star", 0L);
        emptyDistribution.put("2-star", 0L);
        emptyDistribution.put("3-star", 0L);
        emptyDistribution.put("4-star", 0L);
        emptyDistribution.put("5-star", 0L);
        when(reviewService.getRatingDistributionForChef(chefId)).thenReturn(emptyDistribution);

        // Act - Call the controller method
        ResponseEntity<Map<String, Object>> responseEntity = reviewController.getReviewsByChef(chefId, page, size, sort);

        // Assert - Verify the response structure and values match the screenshot
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCodeValue());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        
        // Print the response for debugging
        System.out.println("\n===== DEBUG: CONTROLLER RESPONSE OBJECT =====");
        responseBody.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Check that the response contains all the expected keys with zero/empty values
        assertEquals(Collections.emptyList(), responseBody.get("reviews"));
        assertEquals(0, responseBody.get("currentPage"));
        assertEquals(1, responseBody.get("totalPages"));
        assertEquals(BigDecimal.ZERO, responseBody.get("averageRating"));
        assertEquals(0L, responseBody.get("totalReviews"));
        
        // Check the rating distribution
        @SuppressWarnings("unchecked")
        Map<String, Long> returnedDistribution = (Map<String, Long>) responseBody.get("ratingDistribution");
        assertNotNull(returnedDistribution);
        assertEquals(0L, returnedDistribution.get("1-star"));
        assertEquals(0L, returnedDistribution.get("2-star"));
        assertEquals(0L, returnedDistribution.get("3-star"));
        assertEquals(0L, returnedDistribution.get("4-star"));
        assertEquals(0L, returnedDistribution.get("5-star"));
        
        // Print the JSON-like format
        System.out.println("\nJSON representation of response:");
        System.out.println("{\n" +
                "  \"totalReviews\": " + responseBody.get("totalReviews") + ",\n" +
                "  \"ratingDistribution\": " + returnedDistribution + ",\n" +
                "  \"reviews\": " + responseBody.get("reviews") + ",\n" +
                "  \"averageRating\": " + responseBody.get("averageRating") + ",\n" +
                "  \"currentPage\": " + responseBody.get("currentPage") + ",\n" +
                "  \"totalPages\": " + responseBody.get("totalPages") + "\n" +
                "}");
        System.out.println("===== END DEBUG =====\n");
    }

    @Test
    void testReviewResponseWithValues() {
        // Arrange - Set up our mocks for the case with actual review data
        Long chefId = 1L;
        int page = 0;
        int size = 10;
        String sort = "newest"; // default sort parameter
        
        System.out.println("\n===== DEBUG: CONTROLLER RESPONSE WITH VALUES TEST =====");

        // Create a list of review responses with complete data
        ReviewResponse review1 = new ReviewResponse();
        review1.setId(1L);
        review1.setUserId(testUser.getId());
        review1.setUserName(testUser.getFullName());
        review1.setChefId(testChef.getId());
        review1.setBookingId(101L);
        review1.setRating(new BigDecimal("5.0"));
        review1.setDescription("Amazing food and service!");
        review1.setOverallExperience("One of the best dining experiences I've ever had!");
        review1.setMainImageUrl("http://example.com/images/review1-main.jpg");
        
        List<String> additionalImagesForReview1 = new ArrayList<>();
        additionalImagesForReview1.add("http://example.com/images/review1-additional1.jpg");
        additionalImagesForReview1.add("http://example.com/images/review1-additional2.jpg");
        review1.setAdditionalImageUrls(additionalImagesForReview1);
        
        review1.setResponse("Thank you for your wonderful review! We're glad you enjoyed the experience.");
        review1.setChefResponseAt(LocalDateTime.now().minusHours(1));
        review1.setCreateAt(LocalDateTime.now());
        
        Map<String, Long> reactionCounts1 = new HashMap<>();
        reactionCounts1.put("helpful", 15L);
        reactionCounts1.put("not_helpful", 2L);
        review1.setReactionCounts(reactionCounts1);
        
        ReviewResponse review2 = new ReviewResponse();
        review2.setId(2L);
        review2.setUserId(testUser.getId());
        review2.setUserName(testUser.getFullName());
        review2.setChefId(testChef.getId());
        review2.setBookingId(102L);
        review2.setRating(new BigDecimal("4.0"));
        review2.setDescription("Great experience, would recommend!");
        review2.setOverallExperience("Really enjoyable dinner and great presentation");
        review2.setMainImageUrl("http://example.com/images/review2-main.jpg");
        
        List<String> additionalImagesForReview2 = new ArrayList<>();
        additionalImagesForReview2.add("http://example.com/images/review2-additional1.jpg");
        review2.setAdditionalImageUrls(additionalImagesForReview2);
        
        review2.setResponse("We appreciate your feedback and hope to see you again soon!");
        review2.setChefResponseAt(LocalDateTime.now().minusHours(2));
        review2.setCreateAt(LocalDateTime.now().minusDays(1));
        
        Map<String, Long> reactionCounts2 = new HashMap<>();
        reactionCounts2.put("helpful", 8L);
        reactionCounts2.put("not_helpful", 1L);
        review2.setReactionCounts(reactionCounts2);
        
        // Create page with actual reviews
        Page<ReviewResponse> reviewPage = new PageImpl<>(
                java.util.Arrays.asList(review1, review2),
                PageRequest.of(page, size),
                2);
        
        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());

        // Mock average rating with an actual value
        BigDecimal averageRating = new BigDecimal("4.5");
        
        // Mock the service methods to return data
        when(reviewService.getReviewsByChef(eq(chefId), any(Pageable.class))).thenReturn(reviewPage);
        when(reviewService.getAverageRatingForChef(chefId)).thenReturn(averageRating);
        when(reviewService.getReviewCountForChef(chefId)).thenReturn(2L);
        
        // Mock the rating distribution with actual values
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("1-star", 0L);
        distribution.put("2-star", 0L);
        distribution.put("3-star", 0L);
        distribution.put("4-star", 1L);
        distribution.put("5-star", 1L);
        when(reviewService.getRatingDistributionForChef(chefId)).thenReturn(distribution);

        // Act - Call the controller method
        ResponseEntity<Map<String, Object>> responseEntity = reviewController.getReviewsByChef(chefId, page, size, sort);

        // Assert - Verify the response structure and values
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCodeValue());
        
        Map<String, Object> responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        
        // Print the response for debugging
        System.out.println("DEBUG: CONTROLLER RESPONSE OBJECT");
        responseBody.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Check that the response contains all the expected keys with non-zero/non-empty values
        @SuppressWarnings("unchecked")
        java.util.List<ReviewResponse> reviews = (java.util.List<ReviewResponse>) responseBody.get("reviews");
        assertEquals(2, reviews.size());
        assertEquals(0, responseBody.get("currentPage"));
        assertEquals(1, responseBody.get("totalPages"));
        assertEquals(averageRating, responseBody.get("averageRating"));
        assertEquals(2L, responseBody.get("totalReviews"));
        
        // Verify the first review's complete data
        ReviewResponse returnedReview1 = reviews.get(0);
        assertEquals(1L, returnedReview1.getId());
        assertEquals(testUser.getId(), returnedReview1.getUserId());
        assertEquals("Test User", returnedReview1.getUserName());
        assertEquals(testChef.getId(), returnedReview1.getChefId());
        assertEquals(101L, returnedReview1.getBookingId());
        assertEquals(new BigDecimal("5.0"), returnedReview1.getRating());
        assertEquals("Amazing food and service!", returnedReview1.getDescription());
        assertEquals("One of the best dining experiences I've ever had!", returnedReview1.getOverallExperience());
        assertEquals("http://example.com/images/review1-main.jpg", returnedReview1.getMainImageUrl());
        assertEquals(additionalImagesForReview1, returnedReview1.getAdditionalImageUrls());
        assertEquals("Thank you for your wonderful review! We're glad you enjoyed the experience.", returnedReview1.getResponse());
        assertNotNull(returnedReview1.getChefResponseAt());
        assertNotNull(returnedReview1.getCreateAt());
        assertEquals(reactionCounts1, returnedReview1.getReactionCounts());
        
        // Check the rating distribution
        @SuppressWarnings("unchecked")
        Map<String, Long> returnedDistribution = (Map<String, Long>) responseBody.get("ratingDistribution");
        assertNotNull(returnedDistribution);
        assertEquals(0L, returnedDistribution.get("1-star"));
        assertEquals(0L, returnedDistribution.get("2-star"));
        assertEquals(0L, returnedDistribution.get("3-star"));
        assertEquals(1L, returnedDistribution.get("4-star"));
        assertEquals(1L, returnedDistribution.get("5-star"));
        
        // Print the JSON-like format
        System.out.println("\nJSON representation of response:");
        System.out.println("{\n" +
                "  \"totalReviews\": " + responseBody.get("totalReviews") + ",\n" +
                "  \"ratingDistribution\": " + returnedDistribution + ",\n" +
                "  \"reviews\": [");
        
        // Format each review object with indentation for better readability
        for (int i = 0; i < reviews.size(); i++) {
            ReviewResponse review = reviews.get(i);
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
                    "      \"response\": \"" + review.getResponse() + "\",\n" +
                    "      \"chefResponseAt\": \"" + review.getChefResponseAt() + "\",\n" +
                    "      \"createAt\": \"" + review.getCreateAt() + "\",\n" +
                    "      \"reactionCounts\": " + review.getReactionCounts() + "\n" +
                    "    }" + (i < reviews.size() - 1 ? "," : ""));
        }
        
        System.out.println("  ],\n" +
                "  \"averageRating\": " + responseBody.get("averageRating") + ",\n" +
                "  \"currentPage\": " + responseBody.get("currentPage") + ",\n" +
                "  \"totalPages\": " + responseBody.get("totalPages") + "\n" +
                "}");
        System.out.println("===== END DEBUG =====\n");
    }

    @Test
    void testReviewsWithDifferentSortingOptions() {
        // Arrange - Set up our mocks for the case with actual review data
        Long chefId = 1L;
        int page = 0;
        int size = 10;
        
        // Create review responses with different timestamps and ratings
        ReviewResponse review1 = new ReviewResponse();
        review1.setId(1L);
        review1.setRating(new BigDecimal("5.0"));
        review1.setDescription("Newest, highest rating review");
        review1.setCreateAt(LocalDateTime.now());
        
        ReviewResponse review2 = new ReviewResponse();
        review2.setId(2L);
        review2.setRating(new BigDecimal("4.0"));
        review2.setDescription("Middle date review");
        review2.setCreateAt(LocalDateTime.now().minusDays(1));
        
        ReviewResponse review3 = new ReviewResponse();
        review3.setId(3L);
        review3.setRating(new BigDecimal("3.0"));
        review3.setDescription("Oldest, lowest rating review");
        review3.setCreateAt(LocalDateTime.now().minusDays(2));
        
        List<ReviewResponse> reviewList = List.of(review1, review2, review3);
        
        // Test "newest" sort option
        when(reviewService.getReviewsByChef(eq(chefId), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            
            if (pageable.getSort().equals(Sort.by("createAt").descending())) {
                // For newest first, return reviews in original order (newest first)
                return new PageImpl<>(reviewList, pageable, 3);
            } else if (pageable.getSort().equals(Sort.by("createAt").ascending())) {
                // For oldest first, return reviews in reverse chronological order
                List<ReviewResponse> reversed = new ArrayList<>(reviewList);
                Collections.reverse(reversed);
                return new PageImpl<>(reversed, pageable, 3);
            } else if (pageable.getSort().equals(Sort.by("rating").descending())) {
                // For highest rating, keep original order (as our fake data has descending ratings)
                return new PageImpl<>(reviewList, pageable, 3);
            } else if (pageable.getSort().equals(Sort.by("rating").ascending())) {
                // For lowest rating, reverse the order
                List<ReviewResponse> reversed = new ArrayList<>(reviewList);
                Collections.reverse(reversed);
                return new PageImpl<>(reversed, pageable, 3);
            }
            
            return new PageImpl<>(reviewList, pageable, 3);
        });
        
        when(reviewService.getAverageRatingForChef(chefId)).thenReturn(new BigDecimal("4.0"));
        when(reviewService.getReviewCountForChef(chefId)).thenReturn(3L);
        
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("1-star", 0L);
        distribution.put("2-star", 0L);
        distribution.put("3-star", 1L);
        distribution.put("4-star", 1L);
        distribution.put("5-star", 1L);
        when(reviewService.getRatingDistributionForChef(chefId)).thenReturn(distribution);
        
        // Test newest sort (default)
        ResponseEntity<Map<String, Object>> responseNewest = reviewController.getReviewsByChef(chefId, page, size, "newest");
        Map<String, Object> responseBodyNewest = responseNewest.getBody();
        assertNotNull(responseBodyNewest);
        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviewsNewest = (List<ReviewResponse>) responseBodyNewest.get("reviews");
        assertEquals(3, reviewsNewest.size());
        assertEquals(1L, reviewsNewest.get(0).getId());  // Newest should be first
        assertEquals(3L, reviewsNewest.get(2).getId());  // Oldest should be last
        
        // Test oldest sort
        ResponseEntity<Map<String, Object>> responseOldest = reviewController.getReviewsByChef(chefId, page, size, "oldest");
        Map<String, Object> responseBodyOldest = responseOldest.getBody();
        assertNotNull(responseBodyOldest);
        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviewsOldest = (List<ReviewResponse>) responseBodyOldest.get("reviews");
        assertEquals(3, reviewsOldest.size());
        assertEquals(3L, reviewsOldest.get(0).getId());  // Oldest should be first
        assertEquals(1L, reviewsOldest.get(2).getId());  // Newest should be last
        
        // Test highest-rating sort
        ResponseEntity<Map<String, Object>> responseHighest = reviewController.getReviewsByChef(chefId, page, size, "highest-rating");
        Map<String, Object> responseBodyHighest = responseHighest.getBody();
        assertNotNull(responseBodyHighest);
        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviewsHighest = (List<ReviewResponse>) responseBodyHighest.get("reviews");
        assertEquals(3, reviewsHighest.size());
        assertEquals(new BigDecimal("5.0"), reviewsHighest.get(0).getRating());  // Highest rating should be first
        assertEquals(new BigDecimal("3.0"), reviewsHighest.get(2).getRating());  // Lowest rating should be last
        
        // Test lowest-rating sort
        ResponseEntity<Map<String, Object>> responseLowest = reviewController.getReviewsByChef(chefId, page, size, "lowest-rating");
        Map<String, Object> responseBodyLowest = responseLowest.getBody();
        assertNotNull(responseBodyLowest);
        @SuppressWarnings("unchecked")
        List<ReviewResponse> reviewsLowest = (List<ReviewResponse>) responseBodyLowest.get("reviews");
        assertEquals(3, reviewsLowest.size());
        assertEquals(new BigDecimal("3.0"), reviewsLowest.get(0).getRating());  // Lowest rating should be first
        assertEquals(new BigDecimal("5.0"), reviewsLowest.get(2).getRating());  // Highest rating should be last
    }
} 