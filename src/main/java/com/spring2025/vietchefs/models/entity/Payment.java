package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = true)//test payout
    private Booking booking;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private String paymentMethod; // PayPal, CreditCard, etc.

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(nullable = false)
    private String paymentType; // PAYMENT, REFUND

    @Column(nullable = true)
    private String refundReference; // Nếu có refund, lưu transactionId của refund

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}