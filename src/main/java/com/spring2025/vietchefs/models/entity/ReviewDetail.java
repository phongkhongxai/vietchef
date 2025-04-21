package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "review_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;
    
    @Column(nullable = false)
    private BigDecimal rating;
    
    @ManyToOne
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
    
    @ManyToOne
    @JoinColumn(name = "criteria_id", nullable = false)
    private ReviewCriteria criteria;
} 