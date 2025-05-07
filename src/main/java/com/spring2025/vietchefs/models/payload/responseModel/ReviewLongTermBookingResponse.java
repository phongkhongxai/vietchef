package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewLongTermBookingResponse {
    private BigDecimal totalPrice;    // Tổng giá của toàn bộ booking dài hạn
    private BigDecimal discountAmount;// Số tiền giảm giá (nếu có)
    private BigDecimal distanceKm;
    private List<BookingDetailPriceResponse> bookingDetails; // Chi tiết giá của từng buổi

}
