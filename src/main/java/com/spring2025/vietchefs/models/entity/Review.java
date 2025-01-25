package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "dishes_id")
    private Dish dish;

    @ManyToOne
    @JoinColumn(name = "chef_id")
    private Chef chef;

    private BigDecimal rating;

    private String description;

    private String response;

    private LocalDateTime createAt;

    @Column(nullable = false)
    private boolean isDeleted;
}
