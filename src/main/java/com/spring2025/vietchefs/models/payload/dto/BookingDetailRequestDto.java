package com.spring2025.vietchefs.models.payload.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
public class BookingDetailRequestDto {
    @Future(message = "sessionDate phải là một ngày trong tương lai")
    @NotNull(message = "sessionDate không được để trống")
    private LocalDate sessionDate;
    private LocalTime startTime;
    private String location;
    private BigDecimal totalPrice;
    private BigDecimal chefCookingFee; // Công nấu ăn của đầu bếp
    private BigDecimal priceOfDishes;  // Giá của các món ăn
    private BigDecimal arrivalFee;      // Phí di chuyển
    private BigDecimal discountAmout;
    private LocalTime timeBeginCook;
    private LocalTime timeBeginTravel;
    private BigDecimal platformFee;
    private BigDecimal totalChefFeePrice;
    private BigDecimal totalCookTime;
    private Boolean isUpdated;
    private Long menuId;
    private Boolean chefBringIngredients;
    private List<BookingDetailItemRequestDto> dishes;
}
