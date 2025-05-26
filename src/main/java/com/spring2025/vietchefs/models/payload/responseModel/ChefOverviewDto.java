package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefOverviewDto {
    // Dashboard metrics matching the image
    private BigDecimal totalEarnings;
    private BigDecimal monthlyEarnings;
    private BigDecimal weeklyEarnings;
    private BigDecimal todayEarnings;
    
    // Booking statistics
    private Long totalBookings;
    private Long completedBookings;
    private Long upcomingBookings;
    private Long canceledBookings;
    private Long pendingBookings;
    
    // Performance metrics
    private BigDecimal averageRating;
    private Long totalReviews;
    private Integer reputationPoints;
    private String performanceStatus; // "Excellent", "Good", "Average", "Poor"
    
    // Growth and trends
    private Double monthlyGrowth; // Percentage growth compared to last month
    private Double weeklyGrowth; // Percentage growth compared to last week
    
    // Additional metrics for dashboard cards
    private Long totalCustomers; // Total unique customers served
    private BigDecimal averageOrderValue;
    private Double completionRate; // Percentage of completed vs total bookings
    private Integer activeHours; // Hours worked this month
} 