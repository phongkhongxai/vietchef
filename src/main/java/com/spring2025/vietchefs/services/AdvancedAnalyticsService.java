package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.responseModel.AdvancedAnalyticsDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefRankingDto;
import com.spring2025.vietchefs.models.payload.responseModel.TrendAnalyticsDto;

import java.time.LocalDate;

public interface AdvancedAnalyticsService {
    
    /**
     * Get trend analytics for a specific date range
     */
    TrendAnalyticsDto getTrendAnalytics(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get chef rankings and performance comparisons
     */
    ChefRankingDto getChefRankings(int limit);
    
    /**
     * Get advanced analytics including retention, forecasting, and seasonal analysis
     */
    AdvancedAnalyticsDto getAdvancedAnalytics();
    
    /**
     * Get chef-specific trend analytics
     */
    TrendAnalyticsDto getChefTrendAnalytics(Long chefId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Generate revenue forecast for next period
     */
    AdvancedAnalyticsDto.RevenueForecasting generateRevenueForecasting(int monthsAhead);
    
    /**
     * Calculate customer retention metrics
     */
    AdvancedAnalyticsDto.CustomerRetentionMetrics calculateCustomerRetention();
    
    /**
     * Calculate chef retention metrics
     */
    AdvancedAnalyticsDto.ChefRetentionMetrics calculateChefRetention();
    
    /**
     * Perform seasonal analysis
     */
    AdvancedAnalyticsDto.SeasonalAnalysis performSeasonalAnalysis();
} 