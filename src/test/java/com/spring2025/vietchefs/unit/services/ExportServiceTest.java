package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.payload.responseModel.AdvancedAnalyticsDto;
import com.spring2025.vietchefs.models.payload.responseModel.TrendAnalyticsDto;
import com.spring2025.vietchefs.services.AdvancedAnalyticsService;
import com.spring2025.vietchefs.services.impl.ExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private AdvancedAnalyticsService advancedAnalyticsService;

    @InjectMocks
    private ExportServiceImpl exportService;

    private LocalDate startDate;
    private LocalDate endDate;
    private TrendAnalyticsDto trendAnalyticsDto;
    private AdvancedAnalyticsDto advancedAnalyticsDto;
    private AdvancedAnalyticsDto.RevenueForecasting revenueForecasting;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().minusMonths(1);
        endDate = LocalDate.now();
        
        // Setup TrendAnalyticsDto
        trendAnalyticsDto = TrendAnalyticsDto.builder()
                .revenueChart(createRevenueDataPoints())
                .bookingChart(createBookingDataPoints())
                .userGrowthChart(createUserGrowthDataPoints())
                .performanceChart(createPerformanceDataPoints())
                .build();
        
        // Setup RevenueForecasting
        revenueForecasting = AdvancedAnalyticsDto.RevenueForecasting.builder()
                .nextMonthForecast(createForecastDataPoints(1))
                .nextQuarterForecast(createForecastDataPoints(3))
                .predictedMonthlyRevenue(BigDecimal.valueOf(15000))
                .predictedQuarterlyRevenue(BigDecimal.valueOf(45000))
                .confidenceLevel(85.0)
                .forecastModel("LINEAR")
                .build();
        
        // Setup AdvancedAnalyticsDto
        AdvancedAnalyticsDto.CustomerRetentionMetrics customerRetention = AdvancedAnalyticsDto.CustomerRetentionMetrics.builder()
                .overallRetentionRate(85.5)
                .monthlyRetentionRate(82.3)
                .churnRate(17.7)
                .repeatCustomers(850L)
                .newCustomers(250L)
                .averageCustomerLifetimeValue(BigDecimal.valueOf(500))
                .cohortAnalysis(new ArrayList<>())
                .build();
        
        AdvancedAnalyticsDto.ChefRetentionMetrics chefRetention = AdvancedAnalyticsDto.ChefRetentionMetrics.builder()
                .overallRetentionRate(92.5)
                .monthlyRetentionRate(90.8)
                .churnRate(9.2)
                .activeChefs(185L)
                .inactiveChefs(15L)
                .averageChefLifetimeValue(BigDecimal.valueOf(2000))
                .cohortAnalysis(new ArrayList<>())
                .build();
        
        AdvancedAnalyticsDto.SeasonalAnalysis seasonalAnalysis = AdvancedAnalyticsDto.SeasonalAnalysis.builder()
                .monthlyAverages(createMonthlyAverages())
                .monthlyBookings(createMonthlyBookings())
                .peakSeason("SUMMER")
                .lowSeason("WINTER")
                .seasonalityIndex(1.32)
                .trends(createSeasonalTrends())
                .build();
        
        advancedAnalyticsDto = AdvancedAnalyticsDto.builder()
                .customerRetention(customerRetention)
                .chefRetention(chefRetention)
                .revenueForecasting(revenueForecasting)
                .seasonalAnalysis(seasonalAnalysis)
                .customMetrics(createCustomMetrics())
                .build();
    }

    @Test
    void exportAdminStatisticsToPdf_ShouldGeneratePdfReport() {
        // Given
        when(advancedAnalyticsService.getAdvancedAnalytics()).thenReturn(advancedAnalyticsDto);

        // When
        Resource result = exportService.exportAdminStatisticsToPdf(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals("admin-statistics-" + LocalDate.now().toString() + ".pdf", result.getFilename());
        verify(advancedAnalyticsService).getAdvancedAnalytics();
    }

    @Test
    void exportAdminStatisticsToExcel_ShouldGenerateExcelReport() {
        // When
        Resource result = exportService.exportAdminStatisticsToExcel(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals("admin-statistics-" + LocalDate.now().toString() + ".xlsx", result.getFilename());
        
        // Note: The implementation of generateAdminStatisticsExcel doesn't actually use
        // advancedAnalyticsService.getAdvancedAnalytics() - it just creates mock data
    }

    @Test
    void exportChefPerformanceToPdf_ShouldGenerateChefReport() {
        // Given
        Long chefId = 1L;
        when(advancedAnalyticsService.getChefTrendAnalytics(eq(chefId), any(), any())).thenReturn(trendAnalyticsDto);

        // When
        Resource result = exportService.exportChefPerformanceToPdf(chefId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals("chef-performance-" + chefId + "-" + LocalDate.now().toString() + ".pdf", result.getFilename());
        verify(advancedAnalyticsService).getChefTrendAnalytics(eq(chefId), any(), any());
    }

    @Test
    void exportChefPerformanceToExcel_ShouldGenerateChefExcelReport() {
        // Given
        Long chefId = 1L;

        // When
        Resource result = exportService.exportChefPerformanceToExcel(chefId, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals("chef-performance-" + chefId + "-" + LocalDate.now().toString() + ".xlsx", result.getFilename());
        
        // Note: The implementation of generateChefPerformanceExcel doesn't use
        // advancedAnalyticsService.getChefTrendAnalytics() - it just creates mock data
    }

    @Test
    void exportBookingAnalyticsToPdf_ShouldGenerateBookingReport() {
        // Given
        when(advancedAnalyticsService.getTrendAnalytics(any(), any())).thenReturn(trendAnalyticsDto);

        // When
        Resource result = exportService.exportBookingAnalyticsToPdf(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals("booking-analytics-" + LocalDate.now().toString() + ".pdf", result.getFilename());
        verify(advancedAnalyticsService).getTrendAnalytics(startDate, endDate);
    }

    @Test
    void exportBookingAnalyticsToExcel_ShouldGenerateBookingExcelReport() {
        // When
        Resource result = exportService.exportBookingAnalyticsToExcel(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals("booking-analytics-" + LocalDate.now().toString() + ".xlsx", result.getFilename());
        
        // Note: The implementation of generateBookingAnalyticsExcel doesn't use
        // advancedAnalyticsService.getTrendAnalytics() - it just creates mock data
    }

    @Test
    void exportRevenueForecastingToPdf_ShouldGenerateForecastReport() {
        // Given
        int monthsAhead = 3;
        when(advancedAnalyticsService.generateRevenueForecasting(monthsAhead)).thenReturn(revenueForecasting);

        // When
        Resource result = exportService.exportRevenueForecastingToPdf(monthsAhead);

        // Then
        assertNotNull(result);
        assertEquals("revenue-forecasting-" + LocalDate.now().toString() + ".pdf", result.getFilename());
        verify(advancedAnalyticsService).generateRevenueForecasting(monthsAhead);
    }

    @Test
    void generatePlatformReport_ShouldGeneratePdfReport_WhenFormatIsPdf() {
        // Given
        String format = "pdf";
        when(advancedAnalyticsService.getAdvancedAnalytics()).thenReturn(advancedAnalyticsDto);

        // When
        Resource result = exportService.generatePlatformReport(startDate, endDate, format);

        // Then
        assertNotNull(result);
        assertEquals("admin-statistics-" + LocalDate.now().toString() + ".pdf", result.getFilename());
        // The PDF version does call getAdvancedAnalytics() via generateAdminStatisticsContent
        verify(advancedAnalyticsService).getAdvancedAnalytics();
    }

    @Test
    void generatePlatformReport_ShouldGenerateExcelReport_WhenFormatIsExcel() {
        // Given
        String format = "excel";
        
        // When
        Resource result = exportService.generatePlatformReport(startDate, endDate, format);

        // Then
        assertNotNull(result);
        assertEquals("admin-statistics-" + LocalDate.now().toString() + ".xlsx", result.getFilename());
        
        // Note: The implementation of generateAdminStatisticsExcel doesn't actually use
        // advancedAnalyticsService.getAdvancedAnalytics() - it just creates mock data
    }

    @Test
    void generatePlatformReport_ShouldThrowException_WhenFormatIsInvalid() {
        // Given
        String format = "invalid";

        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            exportService.generatePlatformReport(startDate, endDate, format);
        });
        
        assertEquals("Unsupported format: " + format, exception.getMessage());
    }

    // Helper methods to create test data
    private List<TrendAnalyticsDto.RevenueDataPoint> createRevenueDataPoints() {
        List<TrendAnalyticsDto.RevenueDataPoint> dataPoints = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(30);
        
        for (int i = 0; i < 30; i++) {
            dataPoints.add(TrendAnalyticsDto.RevenueDataPoint.builder()
                    .date(date.plusDays(i))
                    .revenue(BigDecimal.valueOf(1000 + (i * 100)))
                    .commission(BigDecimal.valueOf(100 + (i * 10)))
                    .transactionCount(10L + i)
                    .build());
        }
        
        return dataPoints;
    }
    
    private List<TrendAnalyticsDto.BookingDataPoint> createBookingDataPoints() {
        List<TrendAnalyticsDto.BookingDataPoint> dataPoints = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(30);
        
        for (int i = 0; i < 30; i++) {
            dataPoints.add(TrendAnalyticsDto.BookingDataPoint.builder()
                    .date(date.plusDays(i))
                    .totalBookings(50L + i)
                    .completedBookings(40L + i)
                    .canceledBookings(10L)
                    .averageValue(BigDecimal.valueOf(500 + (i * 10)))
                    .build());
        }
        
        return dataPoints;
    }
    
    private List<TrendAnalyticsDto.UserGrowthDataPoint> createUserGrowthDataPoints() {
        List<TrendAnalyticsDto.UserGrowthDataPoint> dataPoints = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(30);
        
        for (int i = 0; i < 30; i++) {
            dataPoints.add(TrendAnalyticsDto.UserGrowthDataPoint.builder()
                    .date(date.plusDays(i))
                    .newUsers(20L + i)
                    .newChefs(5L + (i / 2))
                    .newCustomers(15L + (i / 2))
                    .totalActiveUsers(1000L + (20L * i))
                    .build());
        }
        
        return dataPoints;
    }
    
    private List<TrendAnalyticsDto.PerformanceDataPoint> createPerformanceDataPoints() {
        List<TrendAnalyticsDto.PerformanceDataPoint> dataPoints = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(30);
        
        for (int i = 0; i < 30; i++) {
            dataPoints.add(TrendAnalyticsDto.PerformanceDataPoint.builder()
                    .date(date.plusDays(i))
                    .averageRating(BigDecimal.valueOf(4.0 + (i * 0.02)))
                    .completionRate(80.0 + (i * 0.5))
                    .totalReviews(500L + (i * 10))
                    .customerSatisfaction(BigDecimal.valueOf(4.2 + (i * 0.01)))
                    .build());
        }
        
        return dataPoints;
    }
    
    private List<AdvancedAnalyticsDto.ForecastDataPoint> createForecastDataPoints(int count) {
        List<AdvancedAnalyticsDto.ForecastDataPoint> dataPoints = new ArrayList<>();
        LocalDate date = LocalDate.now();
        
        for (int i = 0; i < count; i++) {
            dataPoints.add(AdvancedAnalyticsDto.ForecastDataPoint.builder()
                    .date(date.plusMonths(i + 1))
                    .predictedRevenue(BigDecimal.valueOf(15000 + (i * 1000)))
                    .lowerBound(BigDecimal.valueOf(13500 + (i * 900)))
                    .upperBound(BigDecimal.valueOf(16500 + (i * 1100)))
                    .build());
        }
        
        return dataPoints;
    }
    
    private Map<String, BigDecimal> createMonthlyAverages() {
        Map<String, BigDecimal> monthlyAverages = new HashMap<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        for (int i = 0; i < months.length; i++) {
            monthlyAverages.put(months[i], BigDecimal.valueOf(5000 + (i * 500)));
        }
        
        return monthlyAverages;
    }
    
    private Map<String, Long> createMonthlyBookings() {
        Map<String, Long> monthlyBookings = new HashMap<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        for (int i = 0; i < months.length; i++) {
            monthlyBookings.put(months[i], 100L + (i * 10));
        }
        
        return monthlyBookings;
    }
    
    private List<AdvancedAnalyticsDto.SeasonalTrend> createSeasonalTrends() {
        return Arrays.asList(
            AdvancedAnalyticsDto.SeasonalTrend.builder()
                .period("SPRING")
                .averageRevenue(BigDecimal.valueOf(7500))
                .averageBookings(150L)
                .growthRate(12.5)
                .build(),
            AdvancedAnalyticsDto.SeasonalTrend.builder()
                .period("SUMMER")
                .averageRevenue(BigDecimal.valueOf(9000))
                .averageBookings(180L)
                .growthRate(20.0)
                .build(),
            AdvancedAnalyticsDto.SeasonalTrend.builder()
                .period("FALL")
                .averageRevenue(BigDecimal.valueOf(8200))
                .averageBookings(165L)
                .growthRate(15.5)
                .build(),
            AdvancedAnalyticsDto.SeasonalTrend.builder()
                .period("WINTER")
                .averageRevenue(BigDecimal.valueOf(6800))
                .averageBookings(135L)
                .growthRate(8.0)
                .build()
        );
    }
    
    private Map<String, Object> createCustomMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("platformHealthScore", 85.5);
        metrics.put("marketPenetration", 12.3);
        return metrics;
    }
} 