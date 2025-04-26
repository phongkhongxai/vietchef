package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "booking_details")
@Getter
@Setter
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
    private String status = "SCHEDULED"; // scheduled, confirmed, locked
    @Column(nullable = false)
    private LocalTime startTime;
    @Column(nullable = false)
    private LocalTime timeBeginCook;
    @Column(nullable = false)
    private LocalTime timeBeginTravel;
    @Column(nullable = false)
    private String location;
    @Column
    private BigDecimal totalCookTime;
    @Column
    private BigDecimal chefCookingFee; // Công nấu ăn của đầu bếp
    @Column
    private BigDecimal priceOfDishes;  // Giá của các món ăn
    @Column
    private BigDecimal arrivalFee;      // Phí di chuyển
    @Column(nullable = false)
    private BigDecimal platformFee;
    @Column(nullable = false)
    private BigDecimal totalChefFeePrice;
    @Column(nullable = true)
    private BigDecimal discountAmout;
    @Column
    private Long menuId;
    @Column(nullable = false)
    private BigDecimal totalPrice;
    @OneToMany(mappedBy = "bookingDetail", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingDetailItem> dishes = new ArrayList<>();
    @Column(nullable = false)
    private Boolean isDeleted = false;
    @Column(nullable = false)
    private Boolean isUpdated = false;
    @Column(nullable = false)
    private Boolean chefBringIngredients = true;
}
