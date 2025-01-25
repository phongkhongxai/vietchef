package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne
    @JoinColumn(name = "package_id")
    private Package pkg;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private boolean isCustom;
    @Column(nullable = false)
    private boolean isDeleted;
}
