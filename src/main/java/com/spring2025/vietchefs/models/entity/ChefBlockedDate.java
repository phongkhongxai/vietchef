package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "chef_blocked_dates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefBlockedDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long blockId;
    
    @ManyToOne
    @JoinColumn(name = "chef_id", nullable = false)
    private Chef chef;
    
    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;
    
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
} 