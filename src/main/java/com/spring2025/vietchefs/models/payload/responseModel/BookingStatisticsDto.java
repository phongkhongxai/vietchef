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
public class BookingStatisticsDto {
    private Long totalBookings;
    private Long completedBookings;
    private Long canceledBookings;
    private Long pendingBookings;
    private Long confirmedBookings;
    private BigDecimal averageBookingValue;
    private List<BookingTrendDataPoint> bookingTrends;
    private List<TopChefDataPoint> topChefs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingTrendDataPoint {
        private LocalDate date;
        private Long bookingCount;
        private BigDecimal totalValue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopChefDataPoint {
        private Long chefId;
        private String chefName;
        private Long bookingCount;
        private BigDecimal totalEarnings;
        private BigDecimal averageRating;
    }
} 