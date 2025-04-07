package com.spring2025.vietchefs.models.payload.requestModel;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingDetailUpdateRequest {
    List<BookingDetailItemRequestDto> dishes;
    private BigDecimal totalPrice;
    private BigDecimal chefCookingFee; // Công nấu ăn của đầu bếp
    private BigDecimal priceOfDishes;  // Giá của các món ăn
    private BigDecimal arrivalFee;      // Phí di chuyển
    private BigDecimal platformFee; // Phí nền tảng (12% phí dịch vụ)
    private BigDecimal totalChefFeePrice; // Tổng phí dịch vụ đầu bếp (cooking + dish + travel + serving)
    private BigDecimal discountAmout;
    private LocalTime timeBeginCook;
    private LocalTime timeBeginTravel;
    private Long menuId;

}
