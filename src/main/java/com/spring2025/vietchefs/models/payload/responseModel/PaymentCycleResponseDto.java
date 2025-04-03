package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCycleResponseDto {
    private Long id;
    private String status;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private Long bookingId;
}

