package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "dishes")
@Getter
@Setter
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
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String cuisineType;

    @Column(nullable = false)
    private String serviceType;//phục vụ tại nhà
    @Column(nullable = false)
    private BigDecimal cookTime; // Thời gian nấu trung bình phut từ 5p đến 45p

    @Column(nullable = false)
    private BigDecimal basePrice; // Giá cơ bản cho mỗi người từ 0 đến 5 thôi

    private String imageUrl;
    private LocalTime preparationTime;
    @Column(nullable = false)
    private Integer estimatedCookGroup=1;
    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @ManyToMany
    @JoinTable(
            name = "dish_food_type",
            joinColumns = @JoinColumn(name = "dish_id"),
            inverseJoinColumns = @JoinColumn(name = "food_type_id")
    )
    private List<FoodType> foodTypes;

}
