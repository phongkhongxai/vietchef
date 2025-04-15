package com.spring2025.vietchefs.models.payload.dto;

import com.spring2025.vietchefs.models.payload.responseModel.BookingDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDto {
    private Long id;
    private CustomerDto reportedBy;
    private ChefDto reportedChef;
    private String reason;
    private String reasonDetail;
    private String status;
    private LocalDateTime createAt;
    private Long reviewId;
    private BookingDetailResponse bookingDetail;
}
