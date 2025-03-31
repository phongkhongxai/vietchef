package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "dishes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chef_id", nullable = false)
    private Chef chef;

    @Column(nullable = false)
    private String name;
    private String description;

    @Column(nullable = false)
    private String cuisineType;

    @Column(nullable = false)
    private String serviceType;
    @Column(nullable = false)
    private BigDecimal cookTime; // Thời gian nấu trung bình phut

    @Column(nullable = false)
    private BigDecimal basePrice; // Giá cơ bản cho mỗi người

    private String imageUrl;
    private LocalTime preparationTime;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "food_type_id", nullable = false)
    private FoodType foodType;
}
