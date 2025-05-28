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
    // Financial metrics - EXACT CALCULATIONS FROM ACTUAL DATA
    private BigDecimal totalRevenue; // Total platform revenue from customer payments
    private BigDecimal monthlyRevenue; // Revenue this month
    private BigDecimal systemCommission; // ✅ ACTUAL platform commission from booking details (25% cooking fee + 20% dish price - discounts)
    private BigDecimal totalPayouts; // ✅ ACTUAL total paid to chefs from booking details (cooking fee + 80% dish price + travel fee)
    
    // Platform statistics
    private Long totalUsers;
    private Long totalChefs;
    private Long totalCustomers;
    private Long activeBookings;
    private Long completedBookings;
    private Long pendingApprovals; // Pending chef approvals
    
    // Performance indicators
    private Double platformGrowth; // ✅ Monthly growth percentage based on actual commission growth
    private Double chefRetentionRate; // Chef retention rate
    private Double customerSatisfaction; // Average rating across platform
    private String systemHealth; // "Excellent", "Good", "Warning", "Critical"
    
    // Recent activity
    private Long newSignupsToday;
    private Long bookingsToday;
    private BigDecimal revenueToday;
} 