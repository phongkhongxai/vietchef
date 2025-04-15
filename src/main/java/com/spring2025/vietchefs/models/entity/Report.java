package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @ManyToOne
    @JoinColumn(name = "reported_chef_id", nullable = false)
    private Chef reportedChef;

    @Column(nullable = false)
    private String reason; // CHEF_NO_SHOW,
//    LATE_ARRIVAL,
//    WRONG_DISH,
//    UNHYGIENIC,
//    RUDE_BEHAVIOR,
//    OVERCHARGED,
//    POOR_COOKING_QUALITY
    @Column
    private String reasonDetail;

    @Column(nullable = false)
    private String status = "PENDING"; // hoặc: HANDLED, REJECTED

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;
    @ManyToOne
    @JoinColumn(name = "booking_detail_id")
    private BookingDetail bookingDetail;


    @Column(nullable = false)
    private Boolean isDeleted = false;
}
