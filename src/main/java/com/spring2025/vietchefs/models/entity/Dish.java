package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "dishes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String cuisineType;

    private String serviceType;

    private LocalTime cookTime;

    private String imgaeUrl;

    private LocalTime preparationTime;

    @Column(nullable = false)
    private boolean isAvailable;

    @Column(nullable = false)
    private boolean isDeleted;
}
