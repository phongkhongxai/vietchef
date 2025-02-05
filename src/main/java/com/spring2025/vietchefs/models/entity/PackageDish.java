package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "package_dishes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageDish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private Package pkg;

    @ManyToOne
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    private Integer quantity;

    private Double unitPrice;

    @Column(nullable = false)
    private boolean isDeleted;
}
