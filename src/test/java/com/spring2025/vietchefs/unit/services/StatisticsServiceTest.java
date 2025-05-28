package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.payload.responseModel.AdminOverviewDto;
import com.spring2025.vietchefs.models.payload.responseModel.BookingStatisticsDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefOverviewDto;
import com.spring2025.vietchefs.models.payload.responseModel.UserStatisticsDto;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerTransactionRepository customerTransactionRepository;

    @Mock
    private ChefTransactionRepository chefTransactionRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private Chef testChef;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100L);
        
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);
        testChef.setReputationPoints(1500);
    }

    // ==================== ADMIN OVERVIEW TESTS ====================

    @Test
    void getAdminOverview_ShouldReturnCorrectOverview_WhenDataExists() {
        // Given
        BigDecimal totalRevenue = BigDecimal.valueOf(50000);
        BigDecimal monthlyRevenue = BigDecimal.valueOf(5000);
        BigDecimal actualPlatformFee = BigDecimal.valueOf(7500); // ✅ ACTUAL commission from booking details
        BigDecimal actualChefPayouts = BigDecimal.valueOf(42500); // ✅ ACTUAL chef payouts from booking details
        
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(totalRevenue);
        when(customerTransactionRepository.findRevenueFromDate(any(LocalDateTime.class)))
                .thenReturn(monthlyRevenue)
                .thenReturn(BigDecimal.valueOf(9000)); // 60 days ago total
        
        // ✅ Mock accurate financial calculations from BookingDetailRepository
        when(bookingDetailRepository.findTotalActualPlatformFee()).thenReturn(actualPlatformFee);
        when(bookingDetailRepository.findTotalChefPayouts()).thenReturn(actualChefPayouts);
        when(bookingDetailRepository.findActualPlatformFeeFromDate(any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(750)) // Monthly commission
                .thenReturn(BigDecimal.valueOf(1350)); // 60 days ago commission
        
        when(userRepository.countActiveUsers()).thenReturn(1000L);
        when(userRepository.countByRole("ROLE_CHEF")).thenReturn(150L);
        when(userRepository.countByRole("ROLE_CUSTOMER")).thenReturn(850L);
        when(userRepository.countNewUsersFromDate(any(LocalDateTime.class))).thenReturn(25L);
        
        when(bookingRepository.countByStatus("CONFIRMED")).thenReturn(50L);
        when(bookingRepository.countByStatus("CONFIRMED_PAID")).thenReturn(30L);
        when(bookingRepository.countByStatus("CONFIRMED_PARTIALLY_PAID")).thenReturn(20L);
        when(bookingRepository.countByStatus("PAID")).thenReturn(40L);
        when(bookingRepository.countByStatus("DEPOSITED")).thenReturn(10L);
        when(bookingRepository.countByStatus("COMPLETED")).thenReturn(200L);
        when(bookingRepository.countBookingsFromDate(any(LocalDateTime.class))).thenReturn(15L);
        
        when(chefRepository.countByStatus("PENDING")).thenReturn(25L);
        when(chefRepository.countByStatus("ACTIVE")).thenReturn(135L);

        // Mock for real customer satisfaction calculation using existing methods
        when(bookingRepository.findTotalRatingSum()).thenReturn(BigDecimal.valueOf(840));
        when(bookingRepository.countTotalRatings()).thenReturn(200L);

        // When
        AdminOverviewDto result = statisticsService.getAdminOverview();

        // Then
        assertNotNull(result);
        assertEquals(totalRevenue, result.getTotalRevenue());
        assertEquals(monthlyRevenue, result.getMonthlyRevenue());
        
        // ✅ UPDATED: Now using actual values instead of 10% estimate
        assertEquals(actualPlatformFee, result.getSystemCommission());
        assertEquals(actualChefPayouts, result.getTotalPayouts());
        
        assertEquals(1000L, result.getTotalUsers());
        assertEquals(150L, result.getTotalChefs());
        assertEquals(850L, result.getTotalCustomers());
        assertEquals(150L, result.getActiveBookings()); // Sum of all active statuses
        assertEquals(200L, result.getCompletedBookings());
        assertEquals(25L, result.getPendingApprovals());
        assertEquals("Excellent", result.getSystemHealth());
        
        // Real chef retention rate: 135/150 * 100 = 90.0
        assertEquals(90.0, result.getChefRetentionRate());
        
        // Real customer satisfaction: 840/200 = 4.2
        assertEquals(4.2, result.getCustomerSatisfaction());
        
        // Verify interactions with real data methods
        verify(bookingRepository).findTotalRatingSum();
        verify(bookingRepository).countTotalRatings();
        verify(chefRepository, times(1)).countByStatus("ACTIVE"); // Called once for retention calculation only
        
        // ✅ VERIFY: New BookingDetailRepository methods are called
        verify(bookingDetailRepository).findTotalActualPlatformFee();
        verify(bookingDetailRepository).findTotalChefPayouts();
        verify(bookingDetailRepository, times(2)).findActualPlatformFeeFromDate(any(LocalDateTime.class));
    }

    @Test
    void getAdminOverview_ShouldHandleNullValues_WhenNoDataExists() {
        // Given
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(null);
        when(customerTransactionRepository.findRevenueFromDate(any(LocalDateTime.class))).thenReturn(null);
        
        // ✅ Mock null values from BookingDetailRepository
        when(bookingDetailRepository.findTotalActualPlatformFee()).thenReturn(null);
        when(bookingDetailRepository.findTotalChefPayouts()).thenReturn(null);
        when(bookingDetailRepository.findActualPlatformFeeFromDate(any(LocalDateTime.class))).thenReturn(null);
        
        when(userRepository.countActiveUsers()).thenReturn(0L);
        when(userRepository.countByRole(anyString())).thenReturn(0L);
        when(userRepository.countNewUsersFromDate(any(LocalDateTime.class))).thenReturn(0L);
        when(bookingRepository.countByStatus(anyString())).thenReturn(0L);
        when(bookingRepository.countBookingsFromDate(any(LocalDateTime.class))).thenReturn(0L);
        when(chefRepository.countByStatus(anyString())).thenReturn(0L);
        
        // Mock null/zero values for real calculations
        when(bookingRepository.findTotalRatingSum()).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.countTotalRatings()).thenReturn(0L);

        // When
        AdminOverviewDto result = statisticsService.getAdminOverview();

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, result.getMonthlyRevenue());
        
        // ✅ UPDATED: Now checking actual BigDecimal.ZERO instead of calculated values
        assertEquals(BigDecimal.ZERO, result.getSystemCommission());
        assertEquals(BigDecimal.ZERO, result.getTotalPayouts());
        
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getActiveBookings());
        assertEquals(Double.valueOf(0.0), result.getPlatformGrowth());
        assertEquals("Good", result.getSystemHealth()); // 0.0 growth falls into "Good" category (< 5)
        
        // Real calculations with null/zero data
        assertEquals(0.0, result.getChefRetentionRate()); // 0/0 = 0
        assertEquals(4.0, result.getCustomerSatisfaction()); // Default value when no ratings exist
        
        // ✅ VERIFY: BookingDetailRepository methods called even with null data
        verify(bookingDetailRepository).findTotalActualPlatformFee();
        verify(bookingDetailRepository).findTotalChefPayouts();
    }

    @Test
    void getAdminOverview_ShouldUseExactCalculations_NotEstimates() {
        // ✅ NEW TEST: Verify that exact calculations are used instead of 10% estimates
        // Given
        BigDecimal totalRevenue = BigDecimal.valueOf(100000);
        BigDecimal actualPlatformFee = BigDecimal.valueOf(15000); // Not 10% of revenue (would be 10000)
        BigDecimal actualChefPayouts = BigDecimal.valueOf(85000); // Actual payouts from booking details
        
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(totalRevenue);
        when(customerTransactionRepository.findRevenueFromDate(any(LocalDateTime.class))).thenReturn(BigDecimal.valueOf(10000));
        
        when(bookingDetailRepository.findTotalActualPlatformFee()).thenReturn(actualPlatformFee);
        when(bookingDetailRepository.findTotalChefPayouts()).thenReturn(actualChefPayouts);
        when(bookingDetailRepository.findActualPlatformFeeFromDate(any(LocalDateTime.class))).thenReturn(BigDecimal.valueOf(1500));
        
        // Setup minimal required mocks
        when(userRepository.countActiveUsers()).thenReturn(100L);
        when(userRepository.countByRole(anyString())).thenReturn(50L);
        when(userRepository.countNewUsersFromDate(any(LocalDateTime.class))).thenReturn(5L);
        when(bookingRepository.countByStatus(anyString())).thenReturn(10L);
        when(bookingRepository.countBookingsFromDate(any(LocalDateTime.class))).thenReturn(2L);
        when(chefRepository.countByStatus(anyString())).thenReturn(5L);
        when(bookingRepository.findTotalRatingSum()).thenReturn(BigDecimal.valueOf(80));
        when(bookingRepository.countTotalRatings()).thenReturn(20L);

        // When
        AdminOverviewDto result = statisticsService.getAdminOverview();

        // Then
        // ✅ VERIFY: Uses actual values, not 10% estimate
        assertEquals(actualPlatformFee, result.getSystemCommission()); // 15000, not 10000 (10% of 100000)
        assertEquals(actualChefPayouts, result.getTotalPayouts()); // 85000, not 90000 (90% of 100000)
        
        // ✅ VERIFY: Sum should equal total revenue (or close to it, accounting for other fees)
        // Note: In real system, totalRevenue might not equal systemCommission + totalPayouts 
        // due to other factors like discounts, refunds, etc.
        assertNotNull(result.getSystemCommission());
        assertNotNull(result.getTotalPayouts());
        
        verify(bookingDetailRepository).findTotalActualPlatformFee();
        verify(bookingDetailRepository).findTotalChefPayouts();
    }

    // ==================== USER STATISTICS TESTS ====================

    @Test
    void getUserStatistics_ShouldReturnCorrectUserStats() {
        // Given
        when(userRepository.countActiveUsers()).thenReturn(1000L);
        when(userRepository.countByRole("ROLE_CUSTOMER")).thenReturn(800L);
        when(userRepository.countByRole("ROLE_CHEF")).thenReturn(200L);
        when(chefRepository.countByStatus("ACTIVE")).thenReturn(150L);
        when(chefRepository.countByStatus("PENDING")).thenReturn(50L);
        when(userRepository.countByIsBannedTrue()).thenReturn(25L);
        when(userRepository.countByIsPremiumTrue()).thenReturn(100L);

        // When
        UserStatisticsDto result = statisticsService.getUserStatistics();

        // Then
        assertNotNull(result);
        assertEquals(1000L, result.getTotalUsers());
        assertEquals(800L, result.getTotalCustomers());
        assertEquals(200L, result.getTotalChefs());
        assertEquals(150L, result.getActiveChefs());
        assertEquals(50L, result.getPendingChefs());
        assertEquals(25L, result.getBannedUsers());
        assertEquals(100L, result.getPremiumUsers());
        assertEquals(0L, result.getNewUsersThisMonth()); // Still placeholder
        assertNotNull(result.getUserGrowthChart());
        assertTrue(result.getUserGrowthChart().isEmpty());
    }

    // ==================== BOOKING STATISTICS TESTS ====================

    @Test
    void getBookingStatistics_ShouldReturnCorrectBookingStats() {
        // Given
        BigDecimal averageValue = BigDecimal.valueOf(150.50);
        when(bookingRepository.findAverageBookingValue()).thenReturn(averageValue);
        when(bookingRepository.countActiveBookings()).thenReturn(500L);
        when(bookingRepository.countByStatus("COMPLETED")).thenReturn(300L);
        when(bookingRepository.countByStatus("CANCELED")).thenReturn(50L);
        when(bookingRepository.countByStatus("PENDING")).thenReturn(100L);
        when(bookingRepository.countByStatus("CONFIRMED")).thenReturn(30L);
        when(bookingRepository.countByStatus("CONFIRMED_PAID")).thenReturn(15L);
        when(bookingRepository.countByStatus("CONFIRMED_PARTIALLY_PAID")).thenReturn(5L);

        // When
        BookingStatisticsDto result = statisticsService.getBookingStatistics();

        // Then
        assertNotNull(result);
        assertEquals(500L, result.getTotalBookings());
        assertEquals(300L, result.getCompletedBookings());
        assertEquals(50L, result.getCanceledBookings());
        assertEquals(100L, result.getPendingBookings());
        assertEquals(50L, result.getConfirmedBookings()); // Sum of confirmed statuses
        assertEquals(averageValue, result.getAverageBookingValue());
        assertNotNull(result.getBookingTrends());
        assertNotNull(result.getTopChefs());
    }

    @Test
    void getBookingStatistics_ShouldHandleNullAverageValue() {
        // Given
        when(bookingRepository.findAverageBookingValue()).thenReturn(null);
        when(bookingRepository.countActiveBookings()).thenReturn(100L);
        when(bookingRepository.countByStatus(anyString())).thenReturn(20L);

        // When
        BookingStatisticsDto result = statisticsService.getBookingStatistics();

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getAverageBookingValue());
    }

    // ==================== CHEF OVERVIEW TESTS ====================

    @Test
    void getChefOverview_ShouldReturnCorrectChefOverview_WhenChefExists() {
        // Given
        Long chefUserId = 100L;
        BigDecimal totalEarnings = BigDecimal.valueOf(10000);
        BigDecimal monthlyEarnings = BigDecimal.valueOf(2000);
        BigDecimal weeklyEarnings = BigDecimal.valueOf(500);
        BigDecimal todayEarnings = BigDecimal.valueOf(100);
        BigDecimal averageRating = BigDecimal.valueOf(4.5);
        BigDecimal averageOrderValue = BigDecimal.valueOf(200);

        when(chefRepository.findByUserId(chefUserId)).thenReturn(Optional.of(testChef));
        when(chefTransactionRepository.findTotalEarningsByChef(chefUserId)).thenReturn(totalEarnings);
        when(chefTransactionRepository.findEarningsByChefFromDate(eq(chefUserId), any(LocalDateTime.class)))
                .thenReturn(monthlyEarnings) // 30 days ago
                .thenReturn(weeklyEarnings)  // 7 days ago
                .thenReturn(BigDecimal.valueOf(3000)) // 60 days ago
                .thenReturn(BigDecimal.valueOf(700)); // 14 days ago
        when(chefTransactionRepository.findTodayEarningsByChef(chefUserId)).thenReturn(todayEarnings);

        // Booking statistics
        when(bookingRepository.countByChefId(testChef.getId())).thenReturn(100L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "COMPLETED")).thenReturn(80L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CONFIRMED")).thenReturn(10L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CONFIRMED_PAID")).thenReturn(5L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CONFIRMED_PARTIALLY_PAID")).thenReturn(3L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "PAID")).thenReturn(2L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "DEPOSITED")).thenReturn(0L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CANCELED")).thenReturn(5L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "OVERDUE")).thenReturn(2L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "PENDING")).thenReturn(8L);

        when(bookingRepository.countUniqueCustomersByChef(testChef.getId())).thenReturn(50L);
        when(bookingRepository.findAverageOrderValueByChef(testChef.getId())).thenReturn(averageOrderValue);

        // Review statistics - using ReviewService as before
        when(reviewService.getAverageRatingForChef(testChef.getId())).thenReturn(averageRating);
        when(reviewService.getReviewCountForChef(testChef.getId())).thenReturn(75L);

        // When
        ChefOverviewDto result = statisticsService.getChefOverview(chefUserId);

        // Then
        assertNotNull(result);
        assertEquals(totalEarnings, result.getTotalEarnings());
        assertEquals(monthlyEarnings, result.getMonthlyEarnings());
        assertEquals(weeklyEarnings, result.getWeeklyEarnings());
        assertEquals(todayEarnings, result.getTodayEarnings());
        assertEquals(100L, result.getTotalBookings());
        assertEquals(80L, result.getCompletedBookings());
        assertEquals(20L, result.getUpcomingBookings()); // Sum of confirmed statuses
        assertEquals(7L, result.getCanceledBookings()); // CANCELED + OVERDUE
        assertEquals(8L, result.getPendingBookings());
        assertEquals(averageRating, result.getAverageRating());
        assertEquals(75L, result.getTotalReviews());
        assertEquals(1500, result.getReputationPoints());
        assertEquals(50L, result.getTotalCustomers());
        assertEquals(averageOrderValue, result.getAverageOrderValue());
        assertEquals(80.0, result.getCompletionRate()); // 80/100 * 100
        
        // Real calculation: 80 completed bookings * 3 hours per booking = 240 hours
        assertEquals(240, result.getActiveHours()); 
        assertEquals("Average", result.getPerformanceStatus());
        
        // Verify growth calculations
        assertNotNull(result.getMonthlyGrowth());
        assertNotNull(result.getWeeklyGrowth());
        
        // Verify ReviewService method calls
        verify(reviewService).getAverageRatingForChef(testChef.getId());
        verify(reviewService).getReviewCountForChef(testChef.getId());
    }

    @Test
    void getChefOverview_ShouldThrowException_WhenChefNotFound() {
        // Given
        Long chefUserId = 999L;
        when(chefRepository.findByUserId(chefUserId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> statisticsService.getChefOverview(chefUserId));
        assertEquals("Chef not found", exception.getMessage());
    }

    @Test
    void getChefOverview_ShouldHandleNullValues_WhenNoEarningsData() {
        // Given
        Long chefUserId = 100L;
        when(chefRepository.findByUserId(chefUserId)).thenReturn(Optional.of(testChef));
        when(chefTransactionRepository.findTotalEarningsByChef(chefUserId)).thenReturn(null);
        when(chefTransactionRepository.findEarningsByChefFromDate(eq(chefUserId), any(LocalDateTime.class))).thenReturn(null);
        when(chefTransactionRepository.findTodayEarningsByChef(chefUserId)).thenReturn(null);

        // Mock booking data
        when(bookingRepository.countByChefId(testChef.getId())).thenReturn(0L);
        when(bookingRepository.countByChefIdAndStatus(eq(testChef.getId()), anyString())).thenReturn(0L);
        when(bookingRepository.countUniqueCustomersByChef(testChef.getId())).thenReturn(0L);
        when(bookingRepository.findAverageOrderValueByChef(testChef.getId())).thenReturn(null);

        // Mock review data with null values
        when(reviewService.getAverageRatingForChef(testChef.getId())).thenReturn(null);
        when(reviewService.getReviewCountForChef(testChef.getId())).thenReturn(0L);

        // When
        ChefOverviewDto result = statisticsService.getChefOverview(chefUserId);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalEarnings());
        assertEquals(BigDecimal.ZERO, result.getMonthlyEarnings());
        assertEquals(BigDecimal.ZERO, result.getWeeklyEarnings());
        assertEquals(BigDecimal.ZERO, result.getTodayEarnings());
        assertEquals(BigDecimal.ZERO, result.getAverageOrderValue());
        assertEquals(0.0, result.getMonthlyGrowth());
        assertEquals(0.0, result.getWeeklyGrowth());
        assertEquals(0.0, result.getCompletionRate());
        assertEquals("Average", result.getPerformanceStatus());
        
        // Real calculation results with null/zero data
        assertNull(result.getAverageRating()); // reviewService returns null when no ratings
        assertEquals(0L, result.getTotalReviews());
        assertEquals(0, result.getActiveHours()); // 0 completed bookings * 3 = 0
    }

    @Test
    void getChefOverview_ShouldCalculateCorrectPerformanceStatus() {
        // Given - Poor performance scenario (rating < 3.0)
        Long chefUserId = 100L;
        BigDecimal lowRating = BigDecimal.valueOf(2.5);
        
        when(chefRepository.findByUserId(chefUserId)).thenReturn(Optional.of(testChef));
        when(chefTransactionRepository.findTotalEarningsByChef(chefUserId)).thenReturn(BigDecimal.valueOf(1000));
        when(chefTransactionRepository.findEarningsByChefFromDate(eq(chefUserId), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(100));
        when(chefTransactionRepository.findTodayEarningsByChef(chefUserId)).thenReturn(BigDecimal.valueOf(10));

        // Minimal booking data needed for the service to run
        when(bookingRepository.countByChefId(testChef.getId())).thenReturn(10L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "COMPLETED")).thenReturn(5L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CONFIRMED")).thenReturn(1L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CONFIRMED_PAID")).thenReturn(1L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CONFIRMED_PARTIALLY_PAID")).thenReturn(1L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "PAID")).thenReturn(1L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "DEPOSITED")).thenReturn(1L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "CANCELED")).thenReturn(0L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "OVERDUE")).thenReturn(0L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "PENDING")).thenReturn(0L);
        when(bookingRepository.countUniqueCustomersByChef(testChef.getId())).thenReturn(5L);
        when(bookingRepository.findAverageOrderValueByChef(testChef.getId())).thenReturn(BigDecimal.valueOf(50));

        // The key mock for "Poor" performance: rating < 3.0
        when(reviewService.getAverageRatingForChef(testChef.getId())).thenReturn(lowRating);
        when(reviewService.getReviewCountForChef(testChef.getId())).thenReturn(10L);

        // When
        ChefOverviewDto result = statisticsService.getChefOverview(chefUserId);

        // Then
        assertEquals("Poor", result.getPerformanceStatus());
        assertEquals(lowRating, result.getAverageRating());
        assertEquals(10L, result.getTotalReviews());
    }

    @Test
    void getChefOverview_ShouldCalculateRealActiveHours() {
        // Given
        Long chefUserId = 100L;
        when(chefRepository.findByUserId(chefUserId)).thenReturn(Optional.of(testChef));
        when(chefTransactionRepository.findTotalEarningsByChef(chefUserId)).thenReturn(BigDecimal.valueOf(5000));
        when(chefTransactionRepository.findEarningsByChefFromDate(eq(chefUserId), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(500));
        when(chefTransactionRepository.findTodayEarningsByChef(chefUserId)).thenReturn(BigDecimal.valueOf(50));

        // Set up 50 completed bookings for active hours calculation
        when(bookingRepository.countByChefId(testChef.getId())).thenReturn(60L);
        when(bookingRepository.countByChefIdAndStatus(eq(testChef.getId()), anyString())).thenReturn(5L);
        when(bookingRepository.countByChefIdAndStatus(testChef.getId(), "COMPLETED")).thenReturn(50L);
        when(bookingRepository.countUniqueCustomersByChef(testChef.getId())).thenReturn(30L);
        when(bookingRepository.findAverageOrderValueByChef(testChef.getId())).thenReturn(BigDecimal.valueOf(100));

        when(reviewService.getAverageRatingForChef(testChef.getId())).thenReturn(BigDecimal.valueOf(4.0));
        when(reviewService.getReviewCountForChef(testChef.getId())).thenReturn(45L);

        // When
        ChefOverviewDto result = statisticsService.getChefOverview(chefUserId);

        // Then
        // Real calculation: 50 completed bookings * 3 hours per booking = 150 active hours
        assertEquals(150, result.getActiveHours());
        assertEquals(50L, result.getCompletedBookings());
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    void allMethods_ShouldNotThrowExceptions_WhenCalledWithValidData() {
        // Given - Setup all mocks with valid data
        setupValidMockData();

        // When & Then - All methods should execute without exceptions
        assertDoesNotThrow(() -> {
            AdminOverviewDto adminOverview = statisticsService.getAdminOverview();
            assertNotNull(adminOverview);
            
            UserStatisticsDto userStats = statisticsService.getUserStatistics();
            assertNotNull(userStats);
            
            BookingStatisticsDto bookingStats = statisticsService.getBookingStatistics();
            assertNotNull(bookingStats);
            
            ChefOverviewDto chefOverview = statisticsService.getChefOverview(100L);
            assertNotNull(chefOverview);
        });
    }

    private void setupValidMockData() {
        // Customer transaction mocks
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(BigDecimal.valueOf(50000));
        when(customerTransactionRepository.findRevenueFromDate(any(LocalDateTime.class))).thenReturn(BigDecimal.valueOf(5000));

        // ✅ BookingDetailRepository mocks for accurate calculations
        when(bookingDetailRepository.findTotalActualPlatformFee()).thenReturn(BigDecimal.valueOf(7500));
        when(bookingDetailRepository.findTotalChefPayouts()).thenReturn(BigDecimal.valueOf(42500));
        when(bookingDetailRepository.findActualPlatformFeeFromDate(any(LocalDateTime.class))).thenReturn(BigDecimal.valueOf(750));

        // User repository mocks
        when(userRepository.countActiveUsers()).thenReturn(1000L);
        when(userRepository.countByRole(anyString())).thenReturn(500L);
        when(userRepository.countNewUsersFromDate(any(LocalDateTime.class))).thenReturn(50L);
        when(userRepository.countByIsBannedTrue()).thenReturn(10L);
        when(userRepository.countByIsPremiumTrue()).thenReturn(100L);

        // Chef repository mocks
        when(chefRepository.countByStatus(anyString())).thenReturn(25L);
        when(chefRepository.findByUserId(100L)).thenReturn(Optional.of(testChef));

        // Booking repository mocks
        when(bookingRepository.countByStatus(anyString())).thenReturn(50L);
        when(bookingRepository.countActiveBookings()).thenReturn(200L);
        when(bookingRepository.findAverageBookingValue()).thenReturn(BigDecimal.valueOf(150));
        when(bookingRepository.countBookingsFromDate(any(LocalDateTime.class))).thenReturn(20L);
        when(bookingRepository.countByChefId(anyLong())).thenReturn(50L);
        when(bookingRepository.countByChefIdAndStatus(anyLong(), anyString())).thenReturn(10L);
        when(bookingRepository.countUniqueCustomersByChef(anyLong())).thenReturn(25L);
        when(bookingRepository.findAverageOrderValueByChef(anyLong())).thenReturn(BigDecimal.valueOf(200));

        // Chef transaction mocks
        when(chefTransactionRepository.findTotalEarningsByChef(anyLong())).thenReturn(BigDecimal.valueOf(10000));
        when(chefTransactionRepository.findEarningsByChefFromDate(anyLong(), any(LocalDateTime.class))).thenReturn(BigDecimal.valueOf(1000));
        when(chefTransactionRepository.findTodayEarningsByChef(anyLong())).thenReturn(BigDecimal.valueOf(100));

        // Review service mocks
        when(reviewService.getAverageRatingForChef(anyLong())).thenReturn(BigDecimal.valueOf(4.5));
        when(reviewService.getReviewCountForChef(anyLong())).thenReturn(50L);
        
        // Real data calculation mocks
        when(bookingRepository.findTotalRatingSum()).thenReturn(BigDecimal.valueOf(800));
        when(bookingRepository.countTotalRatings()).thenReturn(200L);
    }
} 