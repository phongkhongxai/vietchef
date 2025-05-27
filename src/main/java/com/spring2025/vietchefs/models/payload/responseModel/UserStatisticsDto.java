package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDto {
    private Long totalUsers;
    private Long totalCustomers;
    private Long totalChefs;
    private Long activeChefs;
    private Long pendingChefs;
    private Long bannedUsers;
    private Long premiumUsers;
    private Long newUsersThisMonth;
    private List<UserGrowthDataPoint> userGrowthChart;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrowthDataPoint {
        private LocalDate date;
        private Long newUsers;
        private Long totalUsers;
    }
} 