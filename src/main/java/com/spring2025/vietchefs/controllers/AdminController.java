package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.PaymentCycleService;
import com.spring2025.vietchefs.services.UserService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.StatisticsService;
import com.spring2025.vietchefs.services.AdvancedAnalyticsService;
import com.spring2025.vietchefs.services.ExportService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @Autowired
    private ChefService chefService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private PaymentCycleService paymentCycleService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private AdvancedAnalyticsService advancedAnalyticsService;
    @Autowired
    private ExportService exportService;

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/chefs")
    public ResponseEntity<?> createChefUserEX(@Valid @RequestBody SignupDto signupDto) {
        UserDto bt = userService.saveChefUser(signupDto);
        ChefDto chefDto = new ChefDto();
        chefDto.setUser(bt);
        chefDto.setBio("Hello world");
        chefDto.setPrice(BigDecimal.valueOf(20));
        chefDto.setDescription("Bonjour");
        chefDto.setAddress("S302 Vinhomes Grand Park Quan 9 Ho Chi Minh");
        chefDto.setStatus("active");
        ChefDto chefDt = chefService.createChef(chefDto);
        return new ResponseEntity<>(chefDt, HttpStatus.CREATED);
    }
    @GetMapping("/server-time")
    public String getServerTime() {
        return "Server time: " + LocalDateTime.now();
    }
    @GetMapping("/server-timess")
    public ResponseEntity<List<BookingDetail>> getProge() {
        LocalDate today = LocalDate.now();

        List<BookingDetail> overdueDetails = bookingDetailRepository.findOverdueBookingDetails(today);

        return ResponseEntity.ok(overdueDetails);
    }


//    @SecurityRequirement(name = "Bearer Authentication")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    @PostMapping("/bookings/check")
//    public ResponseEntity<?> paymentCyclesCheck() {
//        bookingService.markOverdueAndRefundBookings();
//        return new ResponseEntity<>("Hehe", HttpStatus.OK);
//    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/users")
    public UsersResponse getAllUsers(@RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                     @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                     @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
                                     @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return userService.getAllUser(pageNo, pageSize, sortBy, sortDir);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/customers")
    public UsersResponse getAllUsersCustomer(@RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                     @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                     @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
                                     @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return userService.getAllCustomer(pageNo, pageSize, sortBy, sortDir);

    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/chefs")
    public UsersResponse getAllUsersChef(@RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                     @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                     @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
                                     @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return userService.getAllChef(pageNo, pageSize, sortBy, sortDir);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        String msg = userService.deleteUser(id);
        return new ResponseEntity<>(msg, HttpStatus.NO_CONTENT);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id, @RequestParam boolean banned) {
        userService.setUserBanStatus(id, banned);
        return ResponseEntity.ok("User " + (banned ? "banned" : "unbanned") + " successfully");
    }

    // ==================== STATISTICS APIs ====================

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/statistics/overview")
    public ResponseEntity<AdminOverviewDto> getAdminOverview() {
        AdminOverviewDto overview = statisticsService.getAdminOverview();
        return ResponseEntity.ok(overview);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/statistics/users")
    public ResponseEntity<UserStatisticsDto> getUserStatistics() {
        UserStatisticsDto userStats = statisticsService.getUserStatistics();
        return ResponseEntity.ok(userStats);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/statistics/bookings")
    public ResponseEntity<BookingStatisticsDto> getBookingStatistics() {
        BookingStatisticsDto bookingStats = statisticsService.getBookingStatistics();
        return ResponseEntity.ok(bookingStats);
    }

    // ==================== PHASE 2: ADVANCED ANALYTICS APIs ====================

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/analytics/trends")
    public ResponseEntity<TrendAnalyticsDto> getTrendAnalytics(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        TrendAnalyticsDto trends = advancedAnalyticsService.getTrendAnalytics(start, end);
        return ResponseEntity.ok(trends);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/analytics/chef-rankings")
    public ResponseEntity<ChefRankingDto> getChefRankings(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        ChefRankingDto rankings = advancedAnalyticsService.getChefRankings(limit);
        return ResponseEntity.ok(rankings);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/analytics/advanced")
    public ResponseEntity<AdvancedAnalyticsDto> getAdvancedAnalytics() {
        AdvancedAnalyticsDto analytics = advancedAnalyticsService.getAdvancedAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/analytics/forecasting")
    public ResponseEntity<AdvancedAnalyticsDto.RevenueForecasting> getRevenueForecasting(
            @RequestParam(value = "monthsAhead", defaultValue = "3") int monthsAhead) {
        
        AdvancedAnalyticsDto.RevenueForecasting forecasting = 
            advancedAnalyticsService.generateRevenueForecasting(monthsAhead);
        return ResponseEntity.ok(forecasting);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/analytics/retention/customers")
    public ResponseEntity<AdvancedAnalyticsDto.CustomerRetentionMetrics> getCustomerRetention() {
        AdvancedAnalyticsDto.CustomerRetentionMetrics retention = 
            advancedAnalyticsService.calculateCustomerRetention();
        return ResponseEntity.ok(retention);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/analytics/retention/chefs")
    public ResponseEntity<AdvancedAnalyticsDto.ChefRetentionMetrics> getChefRetention() {
        AdvancedAnalyticsDto.ChefRetentionMetrics retention = 
            advancedAnalyticsService.calculateChefRetention();
        return ResponseEntity.ok(retention);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/analytics/seasonal")
    public ResponseEntity<AdvancedAnalyticsDto.SeasonalAnalysis> getSeasonalAnalysis() {
        AdvancedAnalyticsDto.SeasonalAnalysis seasonal = 
            advancedAnalyticsService.performSeasonalAnalysis();
        return ResponseEntity.ok(seasonal);
    }

    // ==================== EXPORT APIs ====================

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/export/statistics/pdf")
    public ResponseEntity<org.springframework.core.io.Resource> exportStatisticsToPdf(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        org.springframework.core.io.Resource resource = exportService.exportAdminStatisticsToPdf(start, end);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(resource);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/export/statistics/excel")
    public ResponseEntity<org.springframework.core.io.Resource> exportStatisticsToExcel(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        org.springframework.core.io.Resource resource = exportService.exportAdminStatisticsToExcel(start, end);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(resource);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/export/bookings/pdf")
    public ResponseEntity<org.springframework.core.io.Resource> exportBookingAnalyticsToPdf(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        org.springframework.core.io.Resource resource = exportService.exportBookingAnalyticsToPdf(start, end);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(resource);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/export/forecasting/pdf")
    public ResponseEntity<org.springframework.core.io.Resource> exportRevenueForecastingToPdf(
            @RequestParam(value = "monthsAhead", defaultValue = "3") int monthsAhead) {
        
        org.springframework.core.io.Resource resource = exportService.exportRevenueForecastingToPdf(monthsAhead);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(resource);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/export/platform-report")
    public ResponseEntity<org.springframework.core.io.Resource> generatePlatformReport(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        org.springframework.core.io.Resource resource = exportService.generatePlatformReport(start, end, format);
        
        String contentType = "pdf".equalsIgnoreCase(format) ? "application/pdf" : 
                           "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                .header("Content-Type", contentType)
                .body(resource);
    }

}
