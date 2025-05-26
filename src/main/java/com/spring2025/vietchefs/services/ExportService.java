package com.spring2025.vietchefs.services;

import org.springframework.core.io.Resource;

import java.time.LocalDate;

public interface ExportService {
    
    /**
     * Export admin statistics to PDF
     */
    Resource exportAdminStatisticsToPdf(LocalDate startDate, LocalDate endDate);
    
    /**
     * Export admin statistics to Excel
     */
    Resource exportAdminStatisticsToExcel(LocalDate startDate, LocalDate endDate);
    
    /**
     * Export chef performance report to PDF
     */
    Resource exportChefPerformanceToPdf(Long chefId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Export chef performance report to Excel
     */
    Resource exportChefPerformanceToExcel(Long chefId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Export booking analytics to PDF
     */
    Resource exportBookingAnalyticsToPdf(LocalDate startDate, LocalDate endDate);
    
    /**
     * Export booking analytics to Excel
     */
    Resource exportBookingAnalyticsToExcel(LocalDate startDate, LocalDate endDate);
    
    /**
     * Export revenue forecasting report to PDF
     */
    Resource exportRevenueForecastingToPdf(int monthsAhead);
    
    /**
     * Generate comprehensive platform report
     */
    Resource generatePlatformReport(LocalDate startDate, LocalDate endDate, String format);
} 