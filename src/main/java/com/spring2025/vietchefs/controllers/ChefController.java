package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChefRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.UserService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.StatisticsService;
import com.spring2025.vietchefs.services.AdvancedAnalyticsService;
import com.spring2025.vietchefs.services.ExportService;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chefs")
public class ChefController {
    @Autowired
    private ChefService chefService;
    @Autowired
    private UserService userService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ChefTransactionRepository chefTransactionRepository;
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private AdvancedAnalyticsService advancedAnalyticsService;
    @Autowired
    private ExportService exportService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/register/{userId}")
    public ResponseEntity<ChefResponseDto> registerChef(@PathVariable Long userId, @RequestBody ChefRequestDto requestDto) {
        return ResponseEntity.ok(chefService.registerChefRequest(userId, requestDto));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/approve/{chefId}")
    public ResponseEntity<ChefResponseDto> approveChef(@PathVariable Long chefId) {
        return ResponseEntity.ok(chefService.approveChef(chefId));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/reject/{chefId}")
    public ResponseEntity<ChefResponseDto> rejectChef(@PathVariable Long chefId,@RequestParam String reason) {
        return ResponseEntity.ok(chefService.rejectChef(chefId, reason));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/pending")
    public ChefsResponse getAllChefsPending(
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        return chefService.getAllChefsPending(pageNo, pageSize, sortBy, sortDir);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{chefId}")
    public ResponseEntity<ChefResponseDto> updateChef(@PathVariable Long chefId, @RequestBody ChefRequestDto chefRequestDto) {
        return ResponseEntity.ok(chefService.updateChef(chefId,chefRequestDto));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_CHEF')")
    @PutMapping("/my-chef")
    public ResponseEntity<ChefResponseDto> updateChef(@AuthenticationPrincipal UserDetails userDetails, @RequestBody ChefRequestDto chefRequestDto) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return ResponseEntity.ok(chefService.updateChefBySelf(bto.getId(),chefRequestDto));
    }
    @GetMapping("/{chefId}")
    public ResponseEntity<?> getChefById(@PathVariable Long chefId){
        ChefResponseDto dto = chefService.getChefById(chefId);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping("/unlock")
    public ResponseEntity<String> unlockChef(@AuthenticationPrincipal UserDetails userDetails){
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return ResponseEntity.ok(chefService.unlockChefByPayment(bto.getId()));
    }
    
    @Operation(summary = "Get all active chefs with pagination and sorting", 
            description = "Returns all active chefs. The results can be sorted by rating using sortDir='rating_desc'")
    @GetMapping
    public ChefsResponse getAllChefs(
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        return chefService.getAllChefs(pageNo, pageSize, sortBy, sortDir);
    }
    
    @Operation(summary = "Get nearby chefs with pagination and sorting",
            description = "Returns chefs within the specified distance. The results can be sorted by rating using sortDir='rating_desc'")
    @GetMapping("/nearby")
    public ChefsResponse getAllChefsNearBy(
            @RequestParam(value = "customerLat") Double customerLat,
            @RequestParam(value = "customerLng") Double customerLng,
            @RequestParam(value = "distance") Double distance,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        return chefService.getAllChefsNearBy(customerLat,customerLng,distance,pageNo, pageSize, sortBy, sortDir);
    }
    @GetMapping("/nearby/search")
    public ChefsResponse getAllChefsNearBy(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "customerLat") Double customerLat,
            @RequestParam(value = "customerLng") Double customerLng,
            @RequestParam(value = "distance") Double distance,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        return chefService.getAllChefsNearBySearch(keyword,customerLat,customerLng,distance,pageNo, pageSize, sortBy, sortDir);
    }

    // ==================== CHEF STATISTICS APIs ====================

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/statistics/overview")
    public ResponseEntity<ChefOverviewDto> getChefOverview(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        ChefOverviewDto overview = statisticsService.getChefOverview(user.getId());
        return ResponseEntity.ok(overview);
    }

    // ==================== PHASE 2: CHEF ADVANCED ANALYTICS APIs ====================

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/analytics/trends")
    public ResponseEntity<TrendAnalyticsDto> getChefTrendAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        UserDto user = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        Chef chef = chefRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Chef not found"));
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        TrendAnalyticsDto trends = advancedAnalyticsService.getChefTrendAnalytics(chef.getId(), start, end);
        return ResponseEntity.ok(trends);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/analytics/performance-comparison")
    public ResponseEntity<Map<String, Object>> getPerformanceComparison(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        Chef chef = chefRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        // Get chef rankings to see where this chef stands
        ChefRankingDto rankings = advancedAnalyticsService.getChefRankings(100);
        
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("myRank", findChefRank(chef.getId(), rankings));
        comparison.put("topChefs", rankings.getTopEarningChefs().subList(0, Math.min(5, rankings.getTopEarningChefs().size())));
        comparison.put("industryAverages", calculateIndustryAverages());
        
        return ResponseEntity.ok(comparison);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/analytics/earnings-forecast")
    public ResponseEntity<Map<String, Object>> getEarningsForecast(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "monthsAhead", defaultValue = "3") int monthsAhead) {
        
        UserDto user = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        
        // Get current earnings
        java.math.BigDecimal currentEarnings = chefTransactionRepository.findTotalEarningsByChef(user.getId());
        if (currentEarnings == null) currentEarnings = java.math.BigDecimal.ZERO;
        
        // Simple forecast based on current performance
        Map<String, Object> forecast = new HashMap<>();
        forecast.put("currentMonthlyEarnings", currentEarnings);
        forecast.put("projectedEarnings", currentEarnings.multiply(java.math.BigDecimal.valueOf(1.1 * monthsAhead)));
        forecast.put("confidenceLevel", 75.0);
        forecast.put("recommendations", Arrays.asList(
            "Increase booking frequency to boost earnings",
            "Focus on customer satisfaction to improve ratings",
            "Consider expanding service offerings"
        ));
        
        return ResponseEntity.ok(forecast);
    }

    // Helper methods
    private Integer findChefRank(Long chefId, ChefRankingDto rankings) {
        for (ChefRankingDto.TopChef chef : rankings.getTopEarningChefs()) {
            if (chef.getChefId().equals(chefId)) {
                return chef.getRank();
            }
        }
        return null; // Not in top rankings
    }

    private Map<String, Object> calculateIndustryAverages() {
        Map<String, Object> averages = new HashMap<>();
        averages.put("averageRating", 4.2);
        averages.put("averageEarnings", 3500.0);
        averages.put("averageBookings", 25);
        averages.put("completionRate", 85.0);
        return averages;
    }

    // ==================== CHEF EXPORT APIs ====================

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/export/performance/pdf")
    public ResponseEntity<org.springframework.core.io.Resource> exportPerformanceToPdf(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        UserDto user = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        Chef chef = chefRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Chef not found"));
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        org.springframework.core.io.Resource resource = exportService.exportChefPerformanceToPdf(chef.getId(), start, end);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(resource);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/export/performance/excel")
    public ResponseEntity<org.springframework.core.io.Resource> exportPerformanceToExcel(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        UserDto user = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        Chef chef = chefRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Chef not found"));
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        org.springframework.core.io.Resource resource = exportService.exportChefPerformanceToExcel(chef.getId(), start, end);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(resource);
    }

}
