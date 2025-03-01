package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "booking_detail_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_detail_id", nullable = false)
    private BookingDetail bookingDetail;

    @ManyToOne
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;
    private String notes;
    @Column(nullable = false)
    private Boolean isDeleted = false;
}
