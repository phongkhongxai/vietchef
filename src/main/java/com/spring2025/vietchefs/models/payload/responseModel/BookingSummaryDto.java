package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.CustomerDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryDto {
    private Long id;
    private CustomerDto customer;
    private ChefResponseDto chef;
    private String status;
    private BigDecimal totalPrice;
    private BigDecimal deposidPaid;
    private String bookingType;
}
