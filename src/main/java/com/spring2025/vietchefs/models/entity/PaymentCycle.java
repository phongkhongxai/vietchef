package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_cycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết đến Booking dài hạn
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private LocalDate dueDate;

    // Số tiền cần thanh toán cho đợt này (ban đầu tính theo tỷ lệ cố định)
    @Column(nullable = false)
    private BigDecimal amountDue;

    // Trạng thái của đợt thanh toán: "PENDING", "PAID", "OVERDUE", "CANCELED"
    @Column(nullable = false)
    private String status;

    // (Tuỳ chọn) Thứ tự của đợt, ví dụ: 1, 2, 3,...
    @Column(nullable = false)
    private Integer cycleOrder;
    @Column(nullable = false)
    private LocalDate startDate; // Ngày bắt đầu của `BookingDetail`

    @Column(nullable = false)
    private LocalDate endDate; // Ngày kết thúc của `BookingDetail`
}
