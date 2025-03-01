package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ReviewSingleBookingResponse {
    private LocalTime timeBeginTravel;
    private LocalTime timeBeginCook;
    private BigDecimal totalPrice;
}
