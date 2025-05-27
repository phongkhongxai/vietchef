package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefRankingDto {
    private List<TopChef> topEarningChefs;
    private List<TopChef> topRatedChefs;
    private List<TopChef> mostActiveChefs;
    private List<TopChef> fastestGrowingChefs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopChef {
        private Long chefId;
        private String chefName;
        private String profileImage;
        private BigDecimal totalEarnings;
        private BigDecimal averageRating;
        private Long totalBookings;
        private Long completedBookings;
        private Double completionRate;
        private Double growthRate;
        private Integer rank;
        private String speciality;
        private String location;
    }
} 