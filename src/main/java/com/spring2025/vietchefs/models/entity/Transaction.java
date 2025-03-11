package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết đến ví của đầu bếp
    @ManyToOne
    @JoinColumn(name = "chef_wallet_id", nullable = false)
    private ChefWallet chefWallet;

    // Liên kết đến booking (nếu giao dịch liên quan đến một booking cụ thể)
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // Loại giao dịch: CREDIT (tiền vào ví) hoặc DEBIT (tiền ra ví)
    @Column(nullable = false)
    private String transactionType;

    // Số tiền của giao dịch
    @Column(nullable = false)
    private BigDecimal amount;

    // Mô tả giao dịch, ví dụ "Payment for Booking #123", "Refund for Booking #123", etc.
    @Column(nullable = false)
    private String description;
}
