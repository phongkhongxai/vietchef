package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String requestType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status; //PENDING, APPROVED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String note;
}
