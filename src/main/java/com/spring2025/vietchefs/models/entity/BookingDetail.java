package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


@Entity
@Table(name = "booking_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private LocalDate sessionDate; // Ngày diễn ra buổi ăn

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private Boolean isServing;
    @Column(nullable = false)
    private LocalTime timeBeginCook;
    @Column(nullable = false)
    private LocalTime timeBeginTravel;
    @Column
    private LocalTime endTime;
    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private BigDecimal totalPrice;
    @OneToMany(mappedBy = "bookingDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingDetailItem> dishes;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}
