package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBookingDetailResponse {
    private BigDecimal chefCookingFee; // Phí nấu ăn của đầu bếp
    private BigDecimal priceOfDishes; // Tổng tiền món ăn
    private BigDecimal arrivalFee; // Phí di chuyển của đầu bếp
    private BigDecimal platformFee; // Phí nền tảng (25% phí dịch vụ)
    private BigDecimal totalCookTime;
    private BigDecimal totalChefFeePrice; // Tổng phí dịch vụ đầu bếp (cooking + dish + travel)
    private BigDecimal discountAmout;
    private BigDecimal totalPrice; // Tổng giá trị cập nhật của `BookingDetail`
    private LocalTime timeBeginTravel; // Giờ bắt đầu di chuyển của đầu bếp
    private Long menuId;
    private Boolean chefBringIngredients;
    private LocalTime timeBeginCook; // Giờ đầu bếp bắt đầu nấu ăn
    List<BookingDetailItemRequestDto> dishes;

}
