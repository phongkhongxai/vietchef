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

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().minusMonths(1);
        endDate = LocalDate.now();
        
        testChef = new Chef();
        testChef.setId(1L);
        
        User testUser = new User();
        testUser.setId(100L);
        testChef.setUser(testUser);
    }

    @Test
    void getTrendAnalytics_ShouldReturnAllCharts() {
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
        
        // No repository interactions are needed as this method generates mock data internally
    }

    @Test
    void getChefRankings_ShouldReturnAllRankings() {
        // Given
        int limit = 10;
        
        // When
        ChefRankingDto result = advancedAnalyticsService.getChefRankings(limit);

        // Then
        assertNotNull(result);
        assertNotNull(result.getTopEarningChefs());
        assertNotNull(result.getTopRatedChefs());
        assertNotNull(result.getMostActiveChefs());
        assertNotNull(result.getFastestGrowingChefs());
        
        assertEquals(limit, result.getTopEarningChefs().size());
        assertEquals(limit, result.getTopRatedChefs().size());
        assertEquals(limit, result.getMostActiveChefs().size());
        assertEquals(limit, result.getFastestGrowingChefs().size());
        
        // No repository interaction needed as the implementation generates mock data
    }

    @Test
    void getAdvancedAnalytics_ShouldReturnComprehensiveAnalytics() {
        // Given
        when(customerTransactionRepository.findTotalRevenue()).thenReturn(BigDecimal.valueOf(50000));
        when(userRepository.countByRole("ROLE_CUSTOMER")).thenReturn(1000L);
        when(userRepository.countByRole("ROLE_CHEF")).thenReturn(200L);
        when(chefRepository.countByStatus("ACTIVE")).thenReturn(180L);

        // When
        AdvancedAnalyticsDto result = advancedAnalyticsService.getAdvancedAnalytics();

        // Then
        assertNotNull(result);
        assertNotNull(result.getCustomerRetention());
        assertNotNull(result.getChefRetention());
        assertNotNull(result.getRevenueForecasting());
        assertNotNull(result.getSeasonalAnalysis());
        assertNotNull(result.getCustomMetrics());
        
        assertEquals(600L, result.getCustomerRetention().getRepeatCustomers());
        assertEquals(180L, result.getChefRetention().getActiveChefs());
        assertEquals(20L, result.getChefRetention().getInactiveChefs());
        
        // Verify interactions
        verify(customerTransactionRepository, atLeastOnce()).findTotalRevenue();
        verify(userRepository, times(1)).countByRole("ROLE_CUSTOMER");
        verify(userRepository, times(1)).countByRole("ROLE_CHEF");
        verify(chefRepository, times(1)).countByStatus("ACTIVE");
    }

    @Test
    void getChefTrendAnalytics_ShouldReturnChefSpecificTrends() {
        // Given
        Long chefId = 1L;

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
        assertEquals(expectedDays, result.getBookingChart().size());
        assertEquals(expectedDays, result.getUserGrowthChart().size());
        assertEquals(expectedDays, result.getPerformanceChart().size());
        
        // No repository interactions since the implementation is simplified
        // and just calls getTrendAnalytics without using the chef ID
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
    void calculateCustomerRetention_ShouldCalculateRetentionMetrics() {
        // Given
        when(userRepository.countByRole("ROLE_CUSTOMER")).thenReturn(1000L);

        // When
        AdvancedAnalyticsDto.CustomerRetentionMetrics result = advancedAnalyticsService.calculateCustomerRetention();

        // Then
        assertNotNull(result);
        assertEquals(600L, result.getRepeatCustomers());
        assertEquals(300L, result.getNewCustomers());
        assertTrue(result.getOverallRetentionRate() > 0);
        assertTrue(result.getMonthlyRetentionRate() > 0);
        assertTrue(result.getChurnRate() > 0);
        assertEquals(BigDecimal.valueOf(500.0), result.getAverageCustomerLifetimeValue());
        
        // Verify interactions
        verify(userRepository).countByRole("ROLE_CUSTOMER");
    }

    @Test
    void calculateChefRetention_ShouldCalculateChefRetentionMetrics() {
        // Given
        when(userRepository.countByRole("ROLE_CHEF")).thenReturn(200L);
        when(chefRepository.countByStatus("ACTIVE")).thenReturn(180L);

        // When
        AdvancedAnalyticsDto.ChefRetentionMetrics result = advancedAnalyticsService.calculateChefRetention();

        // Then
        assertNotNull(result);
        assertEquals(180L, result.getActiveChefs());
        assertEquals(20L, result.getInactiveChefs());
        assertEquals(90.0, result.getOverallRetentionRate().doubleValue());
        assertTrue(result.getMonthlyRetentionRate() > 0);
        assertTrue(result.getChurnRate() > 0);
        assertEquals(BigDecimal.valueOf(2000.0), result.getAverageChefLifetimeValue());
        
        // Verify interactions
        verify(userRepository).countByRole("ROLE_CHEF");
        verify(chefRepository).countByStatus("ACTIVE");
    }

    @Test
    void performSeasonalAnalysis_ShouldReturnSeasonalPatterns() {
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
        assertEquals("SUMMER", result.getPeakSeason());
        assertEquals("WINTER", result.getLowSeason());
    }
} 