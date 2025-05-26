package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.responseModel.AdminOverviewDto;
import com.spring2025.vietchefs.models.payload.responseModel.BookingStatisticsDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefOverviewDto;
import com.spring2025.vietchefs.models.payload.responseModel.UserStatisticsDto;

public interface StatisticsService {
    
    /**
     * Get comprehensive admin overview statistics
     */
    AdminOverviewDto getAdminOverview();
    
    /**
     * Get detailed user statistics for admin
     */
    UserStatisticsDto getUserStatistics();
    
    /**
     * Get detailed booking statistics for admin
     */
    BookingStatisticsDto getBookingStatistics();
    
    /**
     * Get chef overview statistics for authenticated chef
     */
    ChefOverviewDto getChefOverview(Long chefUserId);
} 