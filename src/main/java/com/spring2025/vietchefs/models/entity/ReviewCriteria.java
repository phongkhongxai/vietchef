package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_criteria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long criteriaId;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private BigDecimal weight = BigDecimal.valueOf(1.0);
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private Integer displayOrder;
    
    @OneToMany(mappedBy = "criteria", cascade = CascadeType.ALL)
    private List<ReviewDetail> reviewDetails = new ArrayList<>();
} 