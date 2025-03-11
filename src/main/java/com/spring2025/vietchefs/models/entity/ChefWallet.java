package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "chef_wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết 1-1 với Chef
    @OneToOne
    @JoinColumn(name = "chef_id", nullable = false, unique = true)
    private Chef chef;

    // Số dư hiện tại của ví
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    // Thông tin tài khoản PayPal của Chef
    @Column(nullable = true)
    private String paypalAccountEmail;
}
