package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "chef_id", nullable = false)
    private Chef chef;

    @Column(nullable = false)
    private String bookingType; // "single", "long-term"

    @Column(nullable = false)
    private String status; // "pending", "confirmed", "completed", "canceled"
    @Column(columnDefinition = "TEXT")
    private String requestDetails;
    @Column(nullable = false)
    private int guestCount;
    @Column(nullable = false)
    private BigDecimal totalPrice;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingDetail> bookingDetails; // Danh sách các buổi đặt trong booking
    @Column(nullable = false)
    private Boolean isDeleted = false;
    @Column(nullable = false)
    private Boolean isEdit = false;
    @ManyToOne
    @JoinColumn(name = "package_id")
    private Package bookingPackage;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column
    private BigDecimal depositPaid;

}
