package com.spring2025.vietchefs.models.payload.dto;

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
public class BookingRequestDto {
    private Long customerId;
    private Long chefId;
    private String requestDetails;
    private int guestCount;
    private BigDecimal totalPrice;
    private List<BookingDetailRequestDto> bookingDetails;
}
