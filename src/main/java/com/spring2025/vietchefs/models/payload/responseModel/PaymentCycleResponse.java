package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCycleResponse {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amountDue;
    private String status;
    private Integer cycleOrder;
    private List<BookingDetailDto> bookingDetails;

}
