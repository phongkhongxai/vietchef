package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalyticsDto {
    private List<RevenueDataPoint> revenueChart;
    private List<BookingDataPoint> bookingChart;
    private List<UserGrowthDataPoint> userGrowthChart;
    private List<PerformanceDataPoint> performanceChart;
    private List<PaymentDataPoint> paymentChart;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueDataPoint {
        private LocalDate date;
        private BigDecimal revenue;
        private BigDecimal commission;
        private Long transactionCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingDataPoint {
        private LocalDate date;
        private Long totalBookings;
        private Long completedBookings;
        private Long canceledBookings;
        private BigDecimal averageValue;
        private BigDecimal averageCompletedValue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrowthDataPoint {
        private LocalDate date;
        private Long newUsers;
        private Long newChefs;
        private Long newCustomers;
        private Long totalActiveUsers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceDataPoint {
        private LocalDate date;
        private BigDecimal averageRating;
        private Double completionRate;
        private Long totalReviews;
        private BigDecimal customerSatisfaction;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDataPoint {
        private LocalDate date;
        private BigDecimal totalDeposit;
        private BigDecimal totalPayout;
    }

} 