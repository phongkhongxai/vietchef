package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết đến ví của khách hàng
    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // Liên kết đến booking (chỉ khi liên quan đến thanh toán hoặc hoàn tiền)
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = true)
    private Booking booking;

    // Loại giao dịch: DEPOSIT, PAYMENT, REFUND
    @Column(nullable = false)
    private String transactionType;

    // Số tiền giao dịch
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
