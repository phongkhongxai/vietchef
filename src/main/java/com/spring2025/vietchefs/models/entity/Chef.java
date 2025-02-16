package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String bio;
    private String description;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}
