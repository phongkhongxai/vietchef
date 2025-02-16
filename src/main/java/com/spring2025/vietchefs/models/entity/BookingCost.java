package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_costs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private String costType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}
