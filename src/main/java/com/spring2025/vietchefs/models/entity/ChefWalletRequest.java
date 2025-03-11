package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "chef_wallet_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefWalletRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với Chef để biết yêu cầu này của đầu bếp nào
    @ManyToOne
    @JoinColumn(name = "chef_id", nullable = false)
    private Chef chef;

    // Loại yêu cầu: "DEPOSIT" (nạp tiền) hoặc "WITHDRAWAL" (rút tiền)
    @Column(nullable = false)
    private String requestType;

    // Số tiền yêu cầu
    @Column(nullable = false)
    private BigDecimal amount;

    // Trạng thái của request: "PENDING", "APPROVED", "REJECTED","COMPLETED"
    @Column(nullable = false)
    private String status;

    // Ghi chú (nếu có), ví dụ: lý do nạp/ rút, mô tả thêm
    @Column(columnDefinition = "TEXT")
    private String note;

}
