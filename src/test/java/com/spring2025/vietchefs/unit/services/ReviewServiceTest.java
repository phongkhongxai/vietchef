package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCreateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ContentFilterService;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import com.spring2025.vietchefs.services.ReviewReactionService;
import com.spring2025.vietchefs.services.impl.ImageService;
import com.spring2025.vietchefs.services.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewDetailRepository reviewDetailRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ImageRepository imageRepository;
    
    @Mock
    private ReviewCriteriaService reviewCriteriaService;
    
    @Mock
    private ReviewReactionService reviewReactionService;
    
    @Mock
    private ImageService imageService;
    
    @Mock
    private ContentFilterService contentFilterService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Captor
    private ArgumentCaptor<Review> reviewCaptor;

    private User testUser;
    private Chef testChef;
    private User chefUser;
    private Booking testBooking;
    private Review testReview;
    private LocalDateTime testTime;
    private List<ReviewDetail> reviewDetails;
    private ReviewCriteria tasteCriteria;
    private ReviewCriteria presentationCriteria;
    private ReviewCriteriaResponse tasteCriteriaResponse;
    private ReviewCriteriaResponse presentationCriteriaResponse;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        
        // Cài đặt test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setAvatarUrl("avatar.jpg");

        // Cài đặt chef user
        chefUser = new User();
        chefUser.setId(2L);
        chefUser.setFullName("Chef Gordon");
        chefUser.setEmail("chef@example.com");

        // Cài đặt chef
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(chefUser);
        testChef.setDescription("Professional chef with 10 years experience");
        
        // Cài đặt booking
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setChef(testChef);
        testBooking.setCustomer(testUser);
        testBooking.setStatus("COMPLETED");

        // Cài đặt review criteria
        tasteCriteria = new ReviewCriteria();
        tasteCriteria.setCriteriaId(1L);
        tasteCriteria.setName("Taste");
        tasteCriteria.setDescription("Quality of the taste");
        tasteCriteria.setWeight(new BigDecimal(0.5));
        tasteCriteria.setIsActive(true);
        tasteCriteria.setDisplayOrder(1);
                
        presentationCriteria = new ReviewCriteria();
        presentationCriteria.setCriteriaId(2L);
        presentationCriteria.setName("Presentation");
        presentationCriteria.setDescription("Visual appeal of dishes");
        presentationCriteria.setWeight(new BigDecimal(0.3));
        presentationCriteria.setIsActive(true);
        presentationCriteria.setDisplayOrder(2);

        // Cài đặt review criteria response
        tasteCriteriaResponse = new ReviewCriteriaResponse();
        tasteCriteriaResponse.setCriteriaId(1L);
        tasteCriteriaResponse.setName("Taste");
        tasteCriteriaResponse.setDescription("Quality of the taste");
        tasteCriteriaResponse.setWeight(new BigDecimal(0.5));
        tasteCriteriaResponse.setIsActive(true);
        tasteCriteriaResponse.setDisplayOrder(1);
                
        presentationCriteriaResponse = new ReviewCriteriaResponse();
        presentationCriteriaResponse.setCriteriaId(2L);
        presentationCriteriaResponse.setName("Presentation");
        presentationCriteriaResponse.setDescription("Visual appeal of dishes");
        presentationCriteriaResponse.setWeight(new BigDecimal(0.3));
        presentationCriteriaResponse.setIsActive(true);
        presentationCriteriaResponse.setDisplayOrder(2);

        // Cài đặt review details
        ReviewDetail tasteDetail = new ReviewDetail();
        tasteDetail.setDetailId(1L);
        tasteDetail.setCriteria(tasteCriteria);
        tasteDetail.setRating(new BigDecimal("4.5"));
        
        ReviewDetail presentationDetail = new ReviewDetail();
        presentationDetail.setDetailId(2L);
        presentationDetail.setCriteria(presentationCriteria);
        presentationDetail.setRating(new BigDecimal("4.0"));
        
        reviewDetails = Arrays.asList(tasteDetail, presentationDetail);

        // Cài đặt test review
        testReview = new Review();
        testReview.setId(1L);
        testReview.setUser(testUser);
        testReview.setChef(testChef);
        testReview.setBooking(testBooking);
        testReview.setRating(new BigDecimal("4.3"));
        testReview.setOverallExperience("Excellent service and delicious food");
        testReview.setCreateAt(testTime);
        testReview.setReviewDetails(reviewDetails);
        testReview.setIsDeleted(false);
        testReview.setImageUrl("main-image.jpg");
    }

    // Tests for getReviewById

    @Test
    @DisplayName("Test 1: getReviewById when review exists should return review")
    void getReviewById_WhenReviewExists_ShouldReturnReview() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        
        // Action
        ReviewResponse result = reviewService.getReviewById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testChef.getId(), result.getChefId());
        assertEquals("Excellent service and delicious food", result.getOverallExperience());
        assertEquals(testReview.getRating(), result.getRating());
        assertEquals(testTime, result.getCreateAt());
    }

    @Test
    @DisplayName("Test 2: getReviewById when review not found should throw ResourceNotFoundException")
    void getReviewById_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewById(99L);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
    }

    @Test
    @DisplayName("Test 3: getReviewById when review is deleted should throw ResourceNotFoundException")
    void getReviewById_WhenReviewIsDeleted_ShouldThrowResourceNotFoundException() {
        // Arrange
        Review deletedReview = new Review();
        deletedReview.setId(1L);
        deletedReview.setUser(testUser);
        deletedReview.setChef(testChef);
        deletedReview.setBooking(testBooking);
        deletedReview.setRating(new BigDecimal("4.3"));
        deletedReview.setOverallExperience("Excellent service and delicious food");
        deletedReview.setCreateAt(testTime);
        deletedReview.setReviewDetails(reviewDetails);
        deletedReview.setIsDeleted(true);
                
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(deletedReview));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewById(1L);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
    }

    @Test
    @DisplayName("Test 4: getReviewById should include main image URL in response")
    void getReviewById_ShouldIncludeMainImageURLInResponse() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // Act
        ReviewResponse result = reviewService.getReviewById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("main-image.jpg", result.getMainImageUrl());
    }

    // Tests for getReviewsByChef

    @Test
    @DisplayName("Test 1: getReviewsByChef should return paginated reviews for chef")
    void getReviewsByChef_ShouldReturnPaginatedReviewsForChef() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Review> reviews = Arrays.asList(testReview);
        Page<Review> reviewPage = new PageImpl<>(reviews, pageable, reviews.size());
        
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.findByChefAndIsDeletedFalse(any(Chef.class), any(Pageable.class)))
                .thenReturn(reviewPage);

        // Act
        Page<ReviewResponse> result = reviewService.getReviewsByChef(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testReview.getId(), result.getContent().get(0).getId());
        assertEquals(testReview.getUser().getId(), result.getContent().get(0).getUserId());
    }

    @Test
    @DisplayName("Test 2: getReviewsByChef when chef not found should throw ResourceNotFoundException")
    void getReviewsByChef_WhenChefNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(chefRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewsByChef(99L, pageable);
        });
        
        assertTrue(exception.getMessage().contains("Chef not found"));
    }

    @Test
    @DisplayName("Test 3: getReviewsByChef when chef has no reviews should return empty page")
    void getReviewsByChef_WhenChefHasNoReviews_ShouldReturnEmptyPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.findByChefAndIsDeletedFalse(any(Chef.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<ReviewResponse> result = reviewService.getReviewsByChef(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Test 4: getReviewsByChef should exclude deleted reviews")
    void getReviewsByChef_ShouldExcludeDeletedReviews() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        
        // This method call verifies we're using the repository method that filters out deleted reviews
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.findByChefAndIsDeletedFalse(eq(testChef), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        reviewService.getReviewsByChef(1L, pageable);

        // Assert
        verify(reviewRepository).findByChefAndIsDeletedFalse(eq(testChef), eq(pageable));
    }

    // Tests for createReview

    @Test
    @DisplayName("Test 1: createReview with valid data should create and return new review")
    void createReview_WithValidData_ShouldCreateAndReturnNewReview() throws IOException {
        // Arrange
        String reviewContent = "Amazing chef, would book again";
        
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setChefId(1L);
        request.setBookingId(1L);
        request.setOverallExperience(reviewContent);
        
        // Create a mock multipart file for main image
        MockMultipartFile mainImage = new MockMultipartFile(
            "main-image", "test.jpg", "image/jpeg", "test image content".getBytes());
        request.setMainImage(mainImage);
        
        // Setup criteria ratings
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5"));
        criteriaRatings.put(2L, new BigDecimal("4.2"));
        request.setCriteriaRatings(criteriaRatings);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.findByBookingAndIsDeletedFalse(testBooking)).thenReturn(Optional.empty());
        when(contentFilterService.filterText(reviewContent)).thenReturn(reviewContent);
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(tasteCriteriaResponse);
        when(reviewCriteriaService.getCriteriaById(2L)).thenReturn(presentationCriteriaResponse);
        when(imageService.uploadImage(any(MultipartFile.class), anyLong(), anyString())).thenReturn("uploaded-image.jpg");
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        // Act
        ReviewResponse result = reviewService.createReview(request, 1L);

        // Assert
        assertNotNull(result);
        
        // Xác minh reviewRepository.save được gọi đúng số lần (2 lần trong implementation)
        verify(reviewRepository, times(2)).save(reviewCaptor.capture());
        
        // Lấy tất cả các giá trị được capture
        List<Review> capturedReviews = reviewCaptor.getAllValues();
        
        // Kiểm tra đối tượng Review được lưu ở lần gọi đầu tiên (lưu ban đầu)
        // vì đây là đối tượng mà chúng ta thiết lập giá trị trước khi lưu
        Review firstCapturedReview = capturedReviews.get(0);
        assertEquals(testUser, firstCapturedReview.getUser());
        assertEquals(testChef, firstCapturedReview.getChef());
        assertEquals(testBooking, firstCapturedReview.getBooking());
        assertEquals(reviewContent, firstCapturedReview.getOverallExperience());
        assertFalse(firstCapturedReview.getIsDeleted());
        
        // Verify reviewDetailRepository.save was called for each criteria
        verify(reviewDetailRepository, times(2)).save(any(ReviewDetail.class));
        
        // Verify image upload was called
        verify(imageService).uploadImage(any(MultipartFile.class), anyLong(), eq("REVIEW"));
    }

    @Test
    @DisplayName("Test 2: createReview when booking not found should throw ResourceNotFoundException")
    void createReview_WhenBookingNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setChefId(1L);
        request.setBookingId(99L);
        request.setOverallExperience("Great experience");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(request, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Booking not found"));
    }

    @Test
    @DisplayName("Test 3: createReview when review already exists for booking should throw VchefApiException (BR-46)")
    void createReview_WhenReviewAlreadyExistsForBooking_ShouldThrowVchefApiException() {
        // Arrange
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setChefId(1L);
        request.setBookingId(1L);
        request.setOverallExperience("Great experience");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.findByBookingAndIsDeletedFalse(testBooking)).thenReturn(Optional.of(testReview));

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            reviewService.createReview(request, 1L);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("BR-46"));
        assertTrue(exception.getMessage().contains("Mỗi buổi đặt chỉ cho phép gửi một đánh giá"));
    }

    @Test
    @DisplayName("Test 4: createReview should filter profanity in review text")
    void createReview_ShouldFilterProfanityInReviewText() throws IOException {
        // Arrange
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setChefId(1L);
        request.setBookingId(1L);
        request.setOverallExperience("Amazing chef with some bad words");
        
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5"));
        request.setCriteriaRatings(criteriaRatings);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.findByBookingAndIsDeletedFalse(testBooking)).thenReturn(Optional.empty());
        when(contentFilterService.filterText("Amazing chef with some bad words"))
            .thenReturn("Amazing chef with some **** words");
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(tasteCriteriaResponse);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        // Act
        reviewService.createReview(request, 1L);

        // Assert
        verify(contentFilterService).filterText("Amazing chef with some bad words");
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();
        assertEquals("Amazing chef with some **** words", capturedReview.getOverallExperience());
    }

    // Tests for calculateWeightedRating

    @Test
    @DisplayName("Test 1: calculateWeightedRating should return correct weighted average")
    void calculateWeightedRating_ShouldReturnCorrectWeightedAverage() {
        // Arrange
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5")); // Taste with weight 0.5
        criteriaRatings.put(2L, new BigDecimal("4.0")); // Presentation with weight 0.3
        
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(tasteCriteriaResponse);
        when(reviewCriteriaService.getCriteriaById(2L)).thenReturn(presentationCriteriaResponse);

        // Act
        BigDecimal result = reviewService.calculateWeightedRating(criteriaRatings);

        // Assert
        // Weighted average: (4.5 * 0.5 + 4.0 * 0.3) / (0.5 + 0.3) = 4.31
        BigDecimal expected = new BigDecimal("4.31");
        assertEquals(0, expected.compareTo(result.setScale(2, RoundingMode.HALF_UP)), 
                     "Weighted rating should be 4.31");
    }

    @Test
    @DisplayName("Test 2: calculateWeightedRating when criteria not found should throw ResourceNotFoundException")
    void calculateWeightedRating_WhenCriteriaNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(99L, new BigDecimal("4.5"));
        
        when(reviewCriteriaService.getCriteriaById(99L)).thenThrow(new ResourceNotFoundException("Review criteria not found"));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.calculateWeightedRating(criteriaRatings);
        });
        
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Test 3: calculateWeightedRating with empty criteria should return zero")
    void calculateWeightedRating_WithEmptyCriteria_ShouldReturnZero() {
        // Arrange
        Map<Long, BigDecimal> emptyCriteriaRatings = new HashMap<>();

        // Act
        BigDecimal result = reviewService.calculateWeightedRating(emptyCriteriaRatings);
        
        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(result), 
                     "Should return zero when no criteria are provided");
    }

    @Test
    @DisplayName("Test 4: calculateWeightedRating should skip inactive criteria")
    void calculateWeightedRating_ShouldSkipInactiveCriteria() {
        // Arrange
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5")); // Active criteria with weight 0.5
        criteriaRatings.put(2L, new BigDecimal("4.0")); // Will be set as inactive
        
        // Set the second criteria as inactive
        presentationCriteriaResponse.setIsActive(false);
        
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(tasteCriteriaResponse);
        when(reviewCriteriaService.getCriteriaById(2L)).thenReturn(presentationCriteriaResponse);

        // Act
        BigDecimal result = reviewService.calculateWeightedRating(criteriaRatings);

        // Assert
        // Only the active criteria should be used: 4.5 * 0.5 / 0.5 = 4.5
        BigDecimal expected = new BigDecimal("4.5");
        assertEquals(0, expected.compareTo(result.setScale(1, RoundingMode.HALF_UP)), 
                     "Should only use active criteria in calculation");
        
        // Restore the inactive criteria for other tests
        presentationCriteriaResponse.setIsActive(true);
    }

    // Tests for getAverageRatingForChef

    @Test
    @DisplayName("Test 1: getAverageRatingForChef should return correct average rating")
    void getAverageRatingForChef_ShouldReturnCorrectAverageRating() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.of(new BigDecimal("4.3")));

        // Act
        BigDecimal result = reviewService.getAverageRatingForChef(1L);

        // Assert
        assertEquals(0, new BigDecimal("4.3").compareTo(result));
    }

    @Test
    @DisplayName("Test 2: getAverageRatingForChef when chef not found should throw ResourceNotFoundException")
    void getAverageRatingForChef_WhenChefNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(chefRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getAverageRatingForChef(99L);
        });
        
        assertTrue(exception.getMessage().contains("Chef not found"));
    }

    @Test
    @DisplayName("Test 3: getAverageRatingForChef when no reviews exist should return zero")
    void getAverageRatingForChef_WhenNoReviewsExist_ShouldReturnZero() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.empty());

        // Act
        BigDecimal result = reviewService.getAverageRatingForChef(1L);

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    @DisplayName("Test 4: getAverageRatingForChef should round to two decimal places")
    void getAverageRatingForChef_ShouldRoundToTwoDecimalPlaces() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.findAverageRatingByChef(testChef)).thenReturn(Optional.of(new BigDecimal("4.375")));

        // Act
        BigDecimal result = reviewService.getAverageRatingForChef(1L);

        // Assert
        assertEquals(0, new BigDecimal("4.38").compareTo(result));
        assertEquals(2, result.scale()); // Đảm bảo kết quả được làm tròn đến 2 chữ số thập phân
    }

    // Tests for getReviewCountForChef
    
    @Test
    @DisplayName("Test 1: getReviewCountForChef should return correct count")
    void getReviewCountForChef_ShouldReturnCorrectCount() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChef(testChef)).thenReturn(5L);

        // Act
        long result = reviewService.getReviewCountForChef(1L);

        // Assert
        assertEquals(5L, result);
        verify(reviewRepository).countByChef(testChef);
    }
    
    @Test
    @DisplayName("Test 2: getReviewCountForChef when chef not found should throw ResourceNotFoundException")
    void getReviewCountForChef_WhenChefNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(chefRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewCountForChef(99L);
        });
        
        assertTrue(exception.getMessage().contains("Chef not found"));
    }
    
    @Test
    @DisplayName("Test 3: getReviewCountForChef should exclude deleted reviews")
    void getReviewCountForChef_ShouldExcludeDeletedReviews() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChef(testChef)).thenReturn(3L);
        
        // Act
        reviewService.getReviewCountForChef(1L);
        
        // Assert
        // Verify that it's calling the correct method that counts non-deleted reviews
        verify(reviewRepository).countByChef(testChef);
    }
    
    @Test
    @DisplayName("Test 4: getReviewCountForChef should return zero for chef with no reviews")
    void getReviewCountForChef_ShouldReturnZeroForChefWithNoReviews() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(reviewRepository.countByChef(testChef)).thenReturn(0L);
        
        // Act
        long result = reviewService.getReviewCountForChef(1L);
        
        // Assert
        assertEquals(0L, result);
    }

    // Tests for getRatingDistributionForChef

    @Test
    @DisplayName("Test 1: getRatingDistributionForChef should return correct distribution")
    void getRatingDistributionForChef_ShouldReturnCorrectDistribution() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        
        // Mock counts for different rating ranges
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("4.5")))).thenReturn(10L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("3.5")))).thenReturn(20L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("2.5")))).thenReturn(30L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("1.5")))).thenReturn(40L);
        when(reviewRepository.countByChef(testChef)).thenReturn(50L);
        
        // Act
        Map<String, Long> result = reviewService.getRatingDistributionForChef(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(10L, result.get("5-star"));
        assertEquals(10L, result.get("4-star")); // 20 - 10 = 10
        assertEquals(10L, result.get("3-star")); // 30 - 20 = 10
        assertEquals(10L, result.get("2-star")); // 40 - 30 = 10
        assertEquals(10L, result.get("1-star")); // 50 - 40 = 10
    }
    
    @Test
    @DisplayName("Test 2: getRatingDistributionForChef when chef not found should throw ResourceNotFoundException")
    void getRatingDistributionForChef_WhenChefNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(chefRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getRatingDistributionForChef(99L);
        });
        
        assertTrue(exception.getMessage().contains("Chef not found"));
    }
    
    @Test
    @DisplayName("Test 3: getRatingDistributionForChef when no reviews exist should return zeros")
    void getRatingDistributionForChef_WhenNoReviewsExist_ShouldReturnZeros() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        
        // Mock counts to return zero for all ratings
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("4.5")))).thenReturn(0L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("3.5")))).thenReturn(0L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("2.5")))).thenReturn(0L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("1.5")))).thenReturn(0L);
        when(reviewRepository.countByChef(testChef)).thenReturn(0L);
        
        // Act
        Map<String, Long> result = reviewService.getRatingDistributionForChef(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(0L, result.get("5-star"));
        assertEquals(0L, result.get("4-star"));
        assertEquals(0L, result.get("3-star"));
        assertEquals(0L, result.get("2-star"));
        assertEquals(0L, result.get("1-star"));
    }
    
    @Test
    @DisplayName("Test 4: getRatingDistributionForChef with uneven distribution should return correct counts")
    void getRatingDistributionForChef_WithUnevenDistribution_ShouldReturnCorrectCounts() {
        // Arrange
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        
        // Mock an uneven distribution
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("4.5")))).thenReturn(5L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("3.5")))).thenReturn(15L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("2.5")))).thenReturn(20L);
        when(reviewRepository.countByChefAndRatingGreaterThanEqual(eq(testChef), eq(new BigDecimal("1.5")))).thenReturn(22L);
        when(reviewRepository.countByChef(testChef)).thenReturn(25L);
        
        // Act
        Map<String, Long> result = reviewService.getRatingDistributionForChef(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(5L, result.get("5-star"));
        assertEquals(10L, result.get("4-star")); // 15 - 5 = 10
        assertEquals(5L, result.get("3-star"));  // 20 - 15 = 5
        assertEquals(2L, result.get("2-star"));  // 22 - 20 = 2
        assertEquals(3L, result.get("1-star"));  // 25 - 22 = 3
    }
    
    // Tests for getReviewsByUser
    
    @Test
    @DisplayName("Test 1: getReviewsByUser should return user's reviews")
    void getReviewsByUser_ShouldReturnUserReviews() {
        // Arrange
        List<Review> reviews = Arrays.asList(testReview);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByUserAndIsDeletedFalseOrderByCreateAtDesc(testUser)).thenReturn(reviews);
        
        // Act
        List<ReviewResponse> result = reviewService.getReviewsByUser(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testReview.getId(), result.get(0).getId());
        assertEquals(testReview.getUser().getId(), result.get(0).getUserId());
        assertEquals(testReview.getChef().getId(), result.get(0).getChefId());
    }
    
    @Test
    @DisplayName("Test 2: getReviewsByUser when user not found should throw ResourceNotFoundException")
    void getReviewsByUser_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewsByUser(99L);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
    }
    
    @Test
    @DisplayName("Test 3: getReviewsByUser when user has no reviews should return empty list")
    void getReviewsByUser_WhenUserHasNoReviews_ShouldReturnEmptyList() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByUserAndIsDeletedFalseOrderByCreateAtDesc(testUser)).thenReturn(Collections.emptyList());
        
        // Act
        List<ReviewResponse> result = reviewService.getReviewsByUser(1L);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("Test 4: getReviewsByUser should exclude deleted reviews")
    void getReviewsByUser_ShouldExcludeDeletedReviews() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByUserAndIsDeletedFalseOrderByCreateAtDesc(testUser)).thenReturn(Collections.emptyList());
        
        // Act
        reviewService.getReviewsByUser(1L);
        
        // Assert
        verify(reviewRepository).findByUserAndIsDeletedFalseOrderByCreateAtDesc(testUser);
    }
    
    // Tests for getReviewByBooking
    
    @Test
    @DisplayName("Test 1: getReviewByBooking should return review for booking")
    void getReviewByBooking_ShouldReturnReviewForBooking() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.findByBookingAndIsDeletedFalse(testBooking)).thenReturn(Optional.of(testReview));
        
        // Act
        ReviewResponse result = reviewService.getReviewByBooking(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(testReview.getId(), result.getId());
        assertEquals(testReview.getBooking().getId(), testBooking.getId());
    }
    
    @Test
    @DisplayName("Test 2: getReviewByBooking when booking not found should throw ResourceNotFoundException")
    void getReviewByBooking_WhenBookingNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewByBooking(99L);
        });
        
        assertTrue(exception.getMessage().contains("Booking not found"));
    }
    
    @Test
    @DisplayName("Test 3: getReviewByBooking when review not found should throw ResourceNotFoundException")
    void getReviewByBooking_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.findByBookingAndIsDeletedFalse(testBooking)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewByBooking(1L);
        });
        
        assertTrue(exception.getMessage().contains("Review not found for booking"));
    }
    
    @Test
    @DisplayName("Test 4: getReviewByBooking should exclude deleted reviews")
    void getReviewByBooking_ShouldExcludeDeletedReviews() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.findByBookingAndIsDeletedFalse(testBooking)).thenReturn(Optional.of(testReview));
        
        // Act
        reviewService.getReviewByBooking(1L);
        
        // Assert
        verify(reviewRepository).findByBookingAndIsDeletedFalse(testBooking);
    }
    
    // Tests for updateReview
    
    @Test
    @DisplayName("Test 1: updateReview with valid data should update and return review")
    void updateReview_WithValidData_ShouldUpdateAndReturnReview() throws IOException {
        // Arrange
        String updatedContent = "Updated review content - even better experience";
        
        ReviewUpdateRequest request = new ReviewUpdateRequest();
        request.setOverallExperience(updatedContent);
        
        // Setup criteria ratings
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.8"));
        criteriaRatings.put(2L, new BigDecimal("4.6"));
        request.setCriteriaRatings(criteriaRatings);
        
        // Create a mock multipart file for main image
        MockMultipartFile newMainImage = new MockMultipartFile(
            "updated-image", "updated.jpg", "image/jpeg", "updated image content".getBytes());
        request.setMainImage(newMainImage);
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(contentFilterService.filterText(updatedContent)).thenReturn(updatedContent);
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(tasteCriteriaResponse);
        when(reviewCriteriaService.getCriteriaById(2L)).thenReturn(presentationCriteriaResponse);
        when(imageService.uploadImage(any(MultipartFile.class), anyLong(), anyString())).thenReturn("new-image-url.jpg");
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        
        // Act
        ReviewResponse result = reviewService.updateReview(1L, request, 1L);
        
        // Assert
        assertNotNull(result);
        
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();
        assertEquals(updatedContent, capturedReview.getOverallExperience());
        
        verify(imageService).uploadImage(any(MultipartFile.class), eq(1L), eq("REVIEW"));
    }
    
    @Test
    @DisplayName("Test 2: updateReview when review not found should throw ResourceNotFoundException")
    void updateReview_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        ReviewUpdateRequest request = new ReviewUpdateRequest();
        request.setOverallExperience("Updated content");
        
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.updateReview(99L, request, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
    }
    
    @Test
    @DisplayName("Test 3: updateReview when user is not review owner should throw IllegalArgumentException")
    void updateReview_WhenUserNotOwner_ShouldThrowIllegalArgumentException() {
        // Arrange
        ReviewUpdateRequest request = new ReviewUpdateRequest();
        request.setOverallExperience("Updated content");
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Use a different user ID (2L) than the review's owner (1L)
            reviewService.updateReview(1L, request, 2L);
        });
        
        assertTrue(exception.getMessage().contains("Only the user who created"));
    }
    
    @Test
    @DisplayName("Test 4: updateReview should filter profanity in review text")
    void updateReview_ShouldFilterProfanityInReviewText() throws IOException {
        // Arrange
        String contentWithProfanity = "Updated review with some bad words";
        String filteredContent = "Updated review with some **** words";
        
        ReviewUpdateRequest request = new ReviewUpdateRequest();
        request.setOverallExperience(contentWithProfanity);
        
        // Add criteria ratings to prevent NullPointerException
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("4.5"));
        request.setCriteriaRatings(criteriaRatings);
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(contentFilterService.filterText(contentWithProfanity)).thenReturn(filteredContent);
        when(reviewCriteriaService.getCriteriaById(1L)).thenReturn(tasteCriteriaResponse);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        
        // Act
        reviewService.updateReview(1L, request, 1L);
        
        // Assert
        verify(contentFilterService).filterText(contentWithProfanity);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();
        assertEquals(filteredContent, capturedReview.getOverallExperience());
    }
    
    // Tests for deleteReview
    
    @Test
    @DisplayName("Test 1: deleteReview should mark review as deleted")
    void deleteReview_ShouldMarkReviewAsDeleted() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        
        // Act
        reviewService.deleteReview(1L);
        
        // Assert
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();
        assertTrue(capturedReview.getIsDeleted());
    }
    
    @Test
    @DisplayName("Test 2: deleteReview when review not found should throw ResourceNotFoundException")
    void deleteReview_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.deleteReview(99L);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
    }
    
    @Test
    @DisplayName("Test 3: deleteReview should not physically delete review from database")
    void deleteReview_ShouldNotPhysicallyDeleteReview() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        
        // Act
        reviewService.deleteReview(1L);
        
        // Assert
        verify(reviewRepository, never()).deleteById(anyLong());
        verify(reviewRepository, never()).delete(any(Review.class));
        verify(reviewRepository).save(any(Review.class)); // Only saves with updated isDeleted flag
    }
    
    @Test
    @DisplayName("Test 4: deleteReview should maintain relationships for deleted review")
    void deleteReview_ShouldMaintainRelationships() {
        // Arrange
        Review reviewWithRelationships = new Review();
        reviewWithRelationships.setId(1L);
        reviewWithRelationships.setUser(testUser);
        reviewWithRelationships.setChef(testChef);
        reviewWithRelationships.setBooking(testBooking);
        reviewWithRelationships.setRating(new BigDecimal("4.3"));
        reviewWithRelationships.setOverallExperience("Good experience");
        reviewWithRelationships.setReviewDetails(reviewDetails);
        reviewWithRelationships.setIsDeleted(false);
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(reviewWithRelationships));
        
        // Act
        reviewService.deleteReview(1L);
        
        // Assert
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();
        assertTrue(capturedReview.getIsDeleted());
        
        // Verify relationships are maintained
        assertNotNull(capturedReview.getUser());
        assertNotNull(capturedReview.getChef());
        assertNotNull(capturedReview.getBooking());
        assertNotNull(capturedReview.getReviewDetails());
        assertEquals(testUser.getId(), capturedReview.getUser().getId());
        assertEquals(testChef.getId(), capturedReview.getChef().getId());
        assertEquals(testBooking.getId(), capturedReview.getBooking().getId());
        assertEquals(reviewDetails.size(), capturedReview.getReviewDetails().size());
    }
    
    // Tests for addChefResponse
    
    @Test
    @DisplayName("Test 1: addChefResponse should add chef response to review")
    void addChefResponse_ShouldAddChefResponseToReview() {
        // Arrange
        String responseText = "Thank you for your feedback. We're glad you enjoyed the experience!";
        String filteredResponse = responseText; // Assuming no profanity
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(contentFilterService.filterText(responseText)).thenReturn(filteredResponse);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        
        // Act
        ReviewResponse result = reviewService.addChefResponse(1L, responseText, 2L);
        
        // Assert
        assertNotNull(result);
        
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();
        assertEquals(filteredResponse, capturedReview.getResponse());
        assertNotNull(capturedReview.getChefResponseAt());
    }
    
    @Test
    @DisplayName("Test 2: addChefResponse when review not found should throw ResourceNotFoundException")
    void addChefResponse_WhenReviewNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.addChefResponse(99L, "Thank you!", 2L);
        });
        
        assertTrue(exception.getMessage().contains("Review not found"));
    }
    
    @Test
    @DisplayName("Test 3: addChefResponse when chef is not review recipient should throw IllegalArgumentException")
    void addChefResponse_WhenChefNotRecipient_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            // Use a different chef ID (3L) than the review's chef (2L)
            reviewService.addChefResponse(1L, "Thank you!", 3L);
        });
        
        assertTrue(exception.getMessage().contains("Only the chef being reviewed"));
    }
    
    @Test
    @DisplayName("Test 4: addChefResponse should filter profanity in response text")
    void addChefResponse_ShouldFilterProfanityInResponseText() {
        // Arrange
        String responseWithProfanity = "Thank you for the damn good review!";
        String filteredResponse = "Thank you for the **** good review!";
        
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(contentFilterService.filterText(responseWithProfanity)).thenReturn(filteredResponse);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        
        // Act
        reviewService.addChefResponse(1L, responseWithProfanity, 2L);
        
        // Assert
        verify(contentFilterService).filterText(responseWithProfanity);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();
        assertEquals(filteredResponse, capturedReview.getResponse());
    }
} 