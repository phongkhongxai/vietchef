package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.payload.responseModel.AdvancedAnalyticsDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefRankingDto;
import com.spring2025.vietchefs.models.payload.responseModel.TrendAnalyticsDto;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.impl.AdvancedAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancedAnalyticsServiceTest {

    @Mock
    private CustomerTransactionRepository customerTransactionRepository;
    
    @Mock
    private ChefTransactionRepository chefTransactionRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ChefRepository chefRepository;

    @InjectMocks
    private AdvancedAnalyticsServiceImpl advancedAnalyticsService;

    private LocalDate startDate;
    private LocalDate endDate;
    private Chef testChef;
    private User testUser;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().minusMonths(1);
        endDate = LocalDate.now();
        
        testUser = new User();
        testUser.setId(100L);
        testUser.setFullName("Test Chef");
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
        
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);
        testChef.setReputationPoints(80);
    }

    @Test
    void getTrendAnalytics_ShouldReturnAllCharts_WithRealDatabaseData() {
        // Given - Mock repository responses for real data queries
        when(customerTransactionRepository.findRevenueByDate(any())).thenReturn(BigDecimal.valueOf(1000));
        when(customerTransactionRepository.countTransactionsByDate(any())).thenReturn(10L);
        when(bookingRepository.countBookingsByDate(any())).thenReturn(5L);
        when(bookingRepository.countCompletedBookingsByDate(any())).thenReturn(4L);
        when(bookingRepository.countCanceledBookingsByDate(any())).thenReturn(1L);
        when(bookingRepository.findAverageBookingValueByDate(any())).thenReturn(BigDecimal.valueOf(200));
        when(userRepository.countNewUsersByDate(any())).thenReturn(3L);
        when(userRepository.countNewChefsByDate(any())).thenReturn(1L);
        when(userRepository.countNewCustomersByDate(any())).thenReturn(2L);
        when(userRepository.countUsersByDate(any())).thenReturn(100L);
        when(bookingRepository.findAverageRatingByDate(any())).thenReturn(BigDecimal.valueOf(4.5));
        when(bookingRepository.countReviewsByDate(any())).thenReturn(3L);

        // When
        TrendAnalyticsDto result = advancedAnalyticsService.getTrendAnalytics(startDate, endDate);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRevenueChart());
        assertNotNull(result.getBookingChart());
        assertNotNull(result.getUserGrowthChart());
        assertNotNull(result.getPerformanceChart());
        
        // Verify data is generated for the entire date range
        int expectedDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        assertEquals(expectedDays, result.getRevenueChart().size());
        assertEquals(expectedDays, result.getBookingChart().size());
        assertEquals(expectedDays, result.getUserGrowthChart().size());
        assertEquals(expectedDays, result.getPerformanceChart().size());
        
        // Verify real data from database (not random)
        assertTrue(result.getRevenueChart().stream().allMatch(point -> 
            point.getRevenue() != null && point.getRevenue().equals(BigDecimal.valueOf(1000))));
        assertTrue(result.getBookingChart().stream().allMatch(point -> 
            point.getTotalBookings() != null && point.getTotalBookings().equals(5L)));
        
        // Verify repository interactions for real data queries
        verify(customerTransactionRepository, times(expectedDays)).findRevenueByDate(any());
        verify(customerTransactionRepository, times(expectedDays)).countTransactionsByDate(any());
        verify(bookingRepository, times(expectedDays * 2)).countBookingsByDate(any()); // Called by both generateBookingChart() and generatePerformanceChart()
        verify(bookingRepository, times(expectedDays * 2)).countCompletedBookingsByDate(any()); // Called by both generateBookingChart() and generatePerformanceChart()
        verify(bookingRepository, times(expectedDays)).countCanceledBookingsByDate(any()); // Only called by generateBookingChart()
        verify(bookingRepository, times(expectedDays)).findAverageBookingValueByDate(any()); // Only called by generateBookingChart()
        verify(userRepository, times(expectedDays)).countNewUsersByDate(any());
        verify(userRepository, times(expectedDays)).countNewChefsByDate(any());
        verify(userRepository, times(expectedDays)).countNewCustomersByDate(any());
        verify(userRepository, times(expectedDays)).countUsersByDate(any());
        verify(bookingRepository, times(expectedDays)).findAverageRatingByDate(any()); // Only called by generatePerformanceChart()
        verify(bookingRepository, times(expectedDays)).countReviewsByDate(any()); // Only called by generatePerformanceChart()
    }

    @Test
    void getChefRankings_ShouldReturnAllRankings_WithRealData() {
        // Given
        int limit = 5;
        List<Chef> activeChefs = Arrays.asList(testChef);
        
        when(chefRepository.findByStatusAndLimit("ACTIVE", limit)).thenReturn(activeChefs);
        when(chefRepository.findByStatusOrderByReputationDesc("ACTIVE", limit)).thenReturn(activeChefs);
        when(chefRepository.findByStatusAndLimit("ACTIVE", limit * 2)).thenReturn(activeChefs);
        
        when(chefTransactionRepository.findTotalEarningsByChef(anyLong()))
                .thenReturn(BigDecimal.valueOf(5000));
        when(bookingRepository.countByChefId(anyLong()))
                .thenReturn(50L);
        when(bookingRepository.countByChefIdAndStatus(anyLong(), eq("COMPLETED")))
                .thenReturn(40L);

        // When
        ChefRankingDto result = advancedAnalyticsService.getChefRankings(limit);

        // Then
        assertNotNull(result);
        assertNotNull(result.getTopEarningChefs());
        assertNotNull(result.getTopRatedChefs());
        assertNotNull(result.getMostActiveChefs());
        assertNotNull(result.getFastestGrowingChefs());
        
        assertEquals(1, result.getTopEarningChefs().size());
        assertEquals(1, result.getTopRatedChefs().size());
        assertEquals(1, result.getMostActiveChefs().size());
        assertEquals(1, result.getFastestGrowingChefs().size());
        
        // Verify the chef data is correctly mapped
        ChefRankingDto.TopChef topChef = result.getTopEarningChefs().get(0);
        assertEquals(testChef.getId(), topChef.getChefId());
        assertEquals(testUser.getFullName(), topChef.getChefName());
        assertEquals(BigDecimal.valueOf(5000), topChef.getTotalEarnings());
        
        // Verify repository interactions
        verify(chefRepository, times(2)).findByStatusAndLimit("ACTIVE", limit); // Called by generateTopEarningChefs and generateFastestGrowingChefs
        verify(chefRepository).findByStatusOrderByReputationDesc("ACTIVE", limit); // Called by generateTopRatedChefs
        verify(chefRepository).findByStatusAndLimit("ACTIVE", limit * 2); // Called by generateMostActiveChefs
        verify(chefTransactionRepository, atLeastOnce()).findTotalEarningsByChef(anyLong());
    }

    @Test
    void getAdvancedAnalytics_ShouldReturnComprehensiveAnalytics_WithRealCalculations() {
        // Given - Only mock the methods actually called by getAdvancedAnalytics()
        // calculateCustomerRetention() calls:
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(BigDecimal.valueOf(50000));
        when(userRepository.countByRole("ROLE_CUSTOMER")).thenReturn(1000L);
        
        // calculateChefRetention() calls:
        when(userRepository.countByRole("ROLE_CHEF")).thenReturn(200L);
        when(chefRepository.countByStatus("ACTIVE")).thenReturn(180L);
        when(chefTransactionRepository.findTotalEarnings()).thenReturn(BigDecimal.valueOf(36000));
        
        // performSeasonalAnalysis() calls:
        when(customerTransactionRepository.findRevenueByDateRange(any(), any())).thenReturn(BigDecimal.valueOf(4000));
        when(bookingRepository.countBookingsByDateRange(any(), any())).thenReturn(30L);

        // When
        AdvancedAnalyticsDto result = advancedAnalyticsService.getAdvancedAnalytics();

        // Then
        assertNotNull(result);
        assertNotNull(result.getCustomerRetention());
        assertNotNull(result.getChefRetention());
        assertNotNull(result.getRevenueForecasting());
        assertNotNull(result.getSeasonalAnalysis());
        assertNotNull(result.getCustomMetrics());
        
        // Verify real calculations
        assertEquals(600L, result.getCustomerRetention().getRepeatCustomers());
        assertEquals(180L, result.getChefRetention().getActiveChefs());
        assertEquals(20L, result.getChefRetention().getInactiveChefs());
        
        // NOW USING REAL CALCULATIONS: Customer Lifetime Value = Total Revenue / Total Customers
        BigDecimal expectedCustomerLifetimeValue = BigDecimal.valueOf(50000).divide(BigDecimal.valueOf(1000), 2, BigDecimal.ROUND_HALF_UP);
        assertEquals(expectedCustomerLifetimeValue, result.getCustomerRetention().getAverageCustomerLifetimeValue());
        
        // NOW USING REAL CALCULATIONS: Chef Lifetime Value = Total Earnings / Active Chefs
        BigDecimal expectedChefLifetimeValue = BigDecimal.valueOf(36000).divide(BigDecimal.valueOf(180), 2, BigDecimal.ROUND_HALF_UP);
        assertEquals(expectedChefLifetimeValue, result.getChefRetention().getAverageChefLifetimeValue());
        
        // Platform health score is still placeholder
        Double healthScore = (Double) result.getCustomMetrics().get("platformHealthScore");
        assertNotNull(healthScore);
        assertTrue(healthScore > 40 && healthScore <= 100);
        
        // Verify interactions for methods actually called
        verify(customerTransactionRepository, times(2)).findTotalRevenue(); // Called by calculateCustomerRetention() and generateRevenueForecasting()
        verify(userRepository).countByRole("ROLE_CUSTOMER");
        verify(userRepository).countByRole("ROLE_CHEF");
        verify(chefRepository).countByStatus("ACTIVE");
        verify(chefTransactionRepository).findTotalEarnings();
        verify(customerTransactionRepository, times(12)).findRevenueByDateRange(any(), any()); // 12 months for seasonal analysis
        verify(bookingRepository, times(12)).countBookingsByDateRange(any(), any()); // 12 months for seasonal analysis
    }

    @Test
    void generateRevenueForecasting_ShouldGenerateForecastForSpecifiedMonths() {
        // Given
        int monthsAhead = 3;
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(BigDecimal.valueOf(10000));

        // When
        AdvancedAnalyticsDto.RevenueForecasting result = advancedAnalyticsService.generateRevenueForecasting(monthsAhead);

        // Then
        assertNotNull(result);
        assertNotNull(result.getNextMonthForecast());
        assertNotNull(result.getNextQuarterForecast());
        assertNotNull(result.getPredictedMonthlyRevenue());
        assertNotNull(result.getPredictedQuarterlyRevenue());
        assertEquals("LINEAR", result.getForecastModel());
        assertEquals(85.0, result.getConfidenceLevel().doubleValue());
        assertEquals(1, result.getNextMonthForecast().size());
        assertEquals(monthsAhead, result.getNextQuarterForecast().size());
        
        // Verify interactions
        verify(customerTransactionRepository).findTotalRevenue();
    }

    @Test
    void calculateCustomerRetention_ShouldCalculateRealRetentionMetrics() {
        // Given
        when(userRepository.countByRole("ROLE_CUSTOMER")).thenReturn(1000L);
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(BigDecimal.valueOf(50000));

        // When
        AdvancedAnalyticsDto.CustomerRetentionMetrics result = advancedAnalyticsService.calculateCustomerRetention();

        // Then
        assertNotNull(result);
        assertEquals(600L, result.getRepeatCustomers());
        assertEquals(300L, result.getNewCustomers());
        assertTrue(result.getOverallRetentionRate() > 0);
        assertTrue(result.getMonthlyRetentionRate() > 0);
        assertTrue(result.getChurnRate() > 0);
        
        // NOW USING REAL CALCULATION: Total Revenue / Total Customers
        BigDecimal expectedLifetimeValue = BigDecimal.valueOf(50000).divide(BigDecimal.valueOf(1000), 2, BigDecimal.ROUND_HALF_UP);
        assertEquals(expectedLifetimeValue, result.getAverageCustomerLifetimeValue());
        
        // Verify interactions
        verify(userRepository).countByRole("ROLE_CUSTOMER");
        verify(customerTransactionRepository).findTotalRevenue();
    }

    @Test
    void calculateChefRetention_ShouldCalculateRealChefRetentionMetrics() {
        // Given
        when(userRepository.countByRole("ROLE_CHEF")).thenReturn(200L);
        when(chefRepository.countByStatus("ACTIVE")).thenReturn(180L);
        when(chefTransactionRepository.findTotalEarnings()).thenReturn(BigDecimal.valueOf(36000));

        // When
        AdvancedAnalyticsDto.ChefRetentionMetrics result = advancedAnalyticsService.calculateChefRetention();

        // Then
        assertNotNull(result);
        assertEquals(180L, result.getActiveChefs());
        assertEquals(20L, result.getInactiveChefs());
        assertEquals(90.0, result.getOverallRetentionRate().doubleValue());
        assertTrue(result.getMonthlyRetentionRate() > 0);
        assertTrue(result.getChurnRate() > 0);
        
        // NOW USING REAL CALCULATION: Total Earnings / Active Chefs
        BigDecimal expectedLifetimeValue = BigDecimal.valueOf(36000).divide(BigDecimal.valueOf(180), 2, BigDecimal.ROUND_HALF_UP);
        assertEquals(expectedLifetimeValue, result.getAverageChefLifetimeValue());
        
        // Verify interactions
        verify(userRepository).countByRole("ROLE_CHEF");
        verify(chefRepository).countByStatus("ACTIVE");
        verify(chefTransactionRepository).findTotalEarnings();
    }

    @Test
    void performSeasonalAnalysis_ShouldReturnSeasonalPatterns_WithRealDatabaseData() {
        // Given - Mock repository responses for real seasonal data
        when(customerTransactionRepository.findRevenueByDateRange(any(), any())).thenReturn(BigDecimal.valueOf(4000));
        when(bookingRepository.countBookingsByDateRange(any(), any())).thenReturn(30L);

        // When
        AdvancedAnalyticsDto.SeasonalAnalysis result = advancedAnalyticsService.performSeasonalAnalysis();

        // Then
        assertNotNull(result);
        assertNotNull(result.getMonthlyAverages());
        assertNotNull(result.getMonthlyBookings());
        assertNotNull(result.getPeakSeason());
        assertNotNull(result.getLowSeason());
        assertTrue(result.getSeasonalityIndex() > 0);
        assertNotNull(result.getTrends());
        assertEquals(4, result.getTrends().size());
        
        // Peak/Low seasons are now calculated from real data
        assertNotNull(result.getPeakSeason());
        assertNotNull(result.getLowSeason());
        
        // Verify monthly data is generated for all 12 months
        assertEquals(12, result.getMonthlyAverages().size());
        assertEquals(12, result.getMonthlyBookings().size());
        
        // Verify real database calls for 12 months
        verify(customerTransactionRepository, times(12)).findRevenueByDateRange(any(), any());
        verify(bookingRepository, times(12)).countBookingsByDateRange(any(), any());
    }

    @Test
    void getChefTrendAnalytics_ShouldReturnChefSpecificTrends() {
        // Given
        Long chefId = 1L;
        
        // Mock repository responses for real data queries
        when(customerTransactionRepository.findRevenueByDate(any())).thenReturn(BigDecimal.valueOf(500));
        when(customerTransactionRepository.countTransactionsByDate(any())).thenReturn(5L);
        when(bookingRepository.countBookingsByDate(any())).thenReturn(3L);
        when(bookingRepository.countCompletedBookingsByDate(any())).thenReturn(2L);
        when(bookingRepository.countCanceledBookingsByDate(any())).thenReturn(1L);
        when(bookingRepository.findAverageBookingValueByDate(any())).thenReturn(BigDecimal.valueOf(150));
        when(userRepository.countNewUsersByDate(any())).thenReturn(2L);
        when(userRepository.countNewChefsByDate(any())).thenReturn(1L);
        when(userRepository.countNewCustomersByDate(any())).thenReturn(1L);
        when(userRepository.countUsersByDate(any())).thenReturn(50L);
        when(bookingRepository.findAverageRatingByDate(any())).thenReturn(BigDecimal.valueOf(4.2));
        when(bookingRepository.countReviewsByDate(any())).thenReturn(2L);

        // When
        TrendAnalyticsDto result = advancedAnalyticsService.getChefTrendAnalytics(chefId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertNotNull(result.getRevenueChart());
        assertNotNull(result.getBookingChart());
        assertNotNull(result.getUserGrowthChart());
        assertNotNull(result.getPerformanceChart());
        
        // Verify data is generated for the entire date range
        int expectedDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        assertEquals(expectedDays, result.getRevenueChart().size());
        
        // Implementation currently just calls getTrendAnalytics, but now uses real data
        verify(customerTransactionRepository, times(expectedDays)).findRevenueByDate(any());
        verify(customerTransactionRepository, times(expectedDays)).countTransactionsByDate(any());
        verify(bookingRepository, times(expectedDays * 2)).countBookingsByDate(any()); // Called by both generateBookingChart() and generatePerformanceChart()
        verify(bookingRepository, times(expectedDays * 2)).countCompletedBookingsByDate(any()); // Called by both generateBookingChart() and generatePerformanceChart()
        verify(bookingRepository, times(expectedDays)).countCanceledBookingsByDate(any()); 
        verify(bookingRepository, times(expectedDays)).findAverageBookingValueByDate(any());
        verify(userRepository, times(expectedDays)).countNewUsersByDate(any());
        verify(userRepository, times(expectedDays)).countNewChefsByDate(any());
        verify(userRepository, times(expectedDays)).countNewCustomersByDate(any());
        verify(userRepository, times(expectedDays)).countUsersByDate(any());
        verify(bookingRepository, times(expectedDays)).findAverageRatingByDate(any());
        verify(bookingRepository, times(expectedDays)).countReviewsByDate(any());
    }
} 