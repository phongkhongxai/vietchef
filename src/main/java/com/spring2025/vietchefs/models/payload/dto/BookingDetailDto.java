package com.spring2025.vietchefs.models.payload.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailDto {
    private Long id;
    private LocalDate sessionDate;
    private String status;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private BigDecimal totalPrice;
    private BigDecimal chefCookingFee; // Công nấu ăn của đầu bếp
    private BigDecimal priceOfDishes;  // Giá của các món ăn
    private BigDecimal arrivalFee;      // Phí di chuyển
    private BigDecimal chefServingFee;  // Phí phục vụ nếu có
    private BigDecimal platformFee;
    private BigDecimal totalChefFeePrice;
    private BigDecimal discountAmout;
    private LocalTime timeBeginCook;
    private LocalTime timeBeginTravel;
    private Boolean isServing;
    private Boolean isUpdated;
    private Long menuId;
    private Boolean chefBringIngredients;
    private List<BookingDetailItemDto> dishes;
}
