package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Tên gói, ví dụ: "Gói 7 ngày", "Gói 30 ngày"

    @Column(nullable = false)
    private int durationDays; // Số ngày của gói
    @Column
    private BigDecimal discount; // Mức giảm giá, ví dụ: 0.10 (10%)

    @Column(nullable = false)
    private int maxDishesPerMeal; // Số món tối đa cho mỗi bữa ăn

    @Column(nullable = false)
    private int maxGuestCountPerMeal; // Số khách tối đa cho mỗi bữa ăn

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @ManyToMany(mappedBy = "packages")
    private Set<Chef> chefs;
}

