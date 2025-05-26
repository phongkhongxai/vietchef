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
public class AdminOverviewDto {
    // Financial metrics
    private BigDecimal totalRevenue; // Total platform revenue
    private BigDecimal monthlyRevenue; // Revenue this month
    private BigDecimal systemCommission; // Platform commission earned
    private BigDecimal totalPayouts; // Total paid to chefs
    
    // Platform statistics
    private Long totalUsers;
    private Long totalChefs;
    private Long totalCustomers;
    private Long activeBookings;
    private Long completedBookings;
    private Long pendingApprovals; // Pending chef approvals
    
    // Performance indicators
    private Double platformGrowth; // Monthly growth percentage
    private Double chefRetentionRate; // Chef retention rate
    private Double customerSatisfaction; // Average rating across platform
    private String systemHealth; // "Excellent", "Good", "Warning", "Critical"
    
    // Recent activity
    private Long newSignupsToday;
    private Long bookingsToday;
    private BigDecimal revenueToday;
} 