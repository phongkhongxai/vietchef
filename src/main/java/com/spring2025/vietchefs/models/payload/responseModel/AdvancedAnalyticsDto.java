package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedAnalyticsDto {
    private CustomerRetentionMetrics customerRetention;
    private ChefRetentionMetrics chefRetention;
    private RevenueForecasting revenueForecasting;
    private SeasonalAnalysis seasonalAnalysis;
    private Map<String, Object> customMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRetentionMetrics {
        private Double overallRetentionRate;
        private Double monthlyRetentionRate;
        private Double churnRate;
        private Long repeatCustomers;
        private Long newCustomers;
        private BigDecimal averageCustomerLifetimeValue;
        private List<RetentionCohort> cohortAnalysis;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChefRetentionMetrics {
        private Double overallRetentionRate;
        private Double monthlyRetentionRate;
        private Double churnRate;
        private Long activeChefs;
        private Long inactiveChefs;
        private BigDecimal averageChefLifetimeValue;
        private List<RetentionCohort> cohortAnalysis;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetentionCohort {
        private LocalDate cohortMonth;
        private Long initialUsers;
        private Map<Integer, Double> retentionRates; // Month -> Retention Rate
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueForecasting {
        private List<ForecastDataPoint> nextMonthForecast;
        private List<ForecastDataPoint> nextQuarterForecast;
        private BigDecimal predictedMonthlyRevenue;
        private BigDecimal predictedQuarterlyRevenue;
        private Double confidenceLevel;
        private String forecastModel; // "LINEAR", "EXPONENTIAL", "SEASONAL"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastDataPoint {
        private LocalDate date;
        private BigDecimal predictedRevenue;
        private BigDecimal lowerBound;
        private BigDecimal upperBound;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalAnalysis {
        private Map<String, BigDecimal> monthlyAverages; // Month -> Average Revenue
        private Map<String, Long> monthlyBookings; // Month -> Booking Count
        private String peakSeason;
        private String lowSeason;
        private Double seasonalityIndex;
        private List<SeasonalTrend> trends;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeasonalTrend {
        private String period; // "SPRING", "SUMMER", "FALL", "WINTER"
        private BigDecimal averageRevenue;
        private Long averageBookings;
        private Double growthRate;
    }
} 