package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ReviewSingleBookingResponse {
    private LocalTime timeBeginTravel;
    private LocalTime timeBeginCook;
    private BigDecimal cookTimeMinutes;
    private BigDecimal chefCookingFee;
    private BigDecimal priceOfDishes;
    private BigDecimal arrivalFee;
    private BigDecimal platformFee;
    private BigDecimal totalPrice;
    private Boolean chefBringIngredients;
    private Long menuId;
    private BigDecimal totalChefFeePrice;
    private BigDecimal distanceKm;

}
