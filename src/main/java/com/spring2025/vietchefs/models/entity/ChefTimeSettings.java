package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "chef_time_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefTimeSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;
    
    @Column(name = "standard_prep_time")
    private Integer standardPrepTime = 0;
    
    @Column(name = "standard_cleanup_time")
    private Integer standardCleanupTime = 0;
    
    @Column(name = "travel_buffer_percentage")
    private Integer travelBufferPercentage = 0;
    
    @Column(name = "cooking_efficiency_factor")
    private BigDecimal cookingEfficiencyFactor = new BigDecimal("0.0");
    
    @Column(name = "min_booking_notice_hours")
    private Integer minBookingNoticeHours = 24;
    
    @Column(name = "max_booking_days_ahead")
    private Integer maxBookingDaysAhead = 7;
    
    @Column(name = "max_dishes_per_session")
    private Integer maxDishesPerSession = 5;
    
    @Column(name = "max_guests_per_session")
    private Integer maxGuestsPerSession = 8;
    
    @Column(name = "service_radius_km")
    private Integer serviceRadiusKm = 15;
    
    @Column(name = "max_sessions_per_day")
    private Integer maxSessionsPerDay = 3;
    
    @OneToOne
    @JoinColumn(name = "chef_id", nullable = false, unique = true)
    private Chef chef;
} 