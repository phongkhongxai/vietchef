package com.spring2025.vietchefs.models.payload.dto;

import com.spring2025.vietchefs.models.entity.Chef;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private Long customerId;
    private Long chefId;
    private String bookingType;
    private String status;
    private String requestDetails;
    private int guestCount;
    private BigDecimal totalPrice;
}
