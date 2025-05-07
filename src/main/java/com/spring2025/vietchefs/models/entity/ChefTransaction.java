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
@Table(name = "chef_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ví của đầu bếp nhận tiền
    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "booking_detail_id", nullable = true)
    private BookingDetail bookingDetail;

    @Column(nullable = false)
    private String transactionType = "CREDIT";

    @Column(nullable = false)

    private BigDecimal amount;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime transactionDate=LocalDateTime.now();
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private String status;

    // Đánh dấu xóa mềm nếu cần
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
