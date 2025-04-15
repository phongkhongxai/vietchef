package com.spring2025.vietchefs.models.payload.responseModel;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailPriceResponse {
    private LocalDate sessionDate; // Ngày diễn ra buổi ăn
    private BigDecimal totalPrice; // Tổng tiền buổi ăn
    private BigDecimal chefCookingFee; // Công nấu ăn của đầu bếp
    private BigDecimal priceOfDishes;  // Giá của các món ăn
    private BigDecimal arrivalFee;
    private LocalTime timeBeginTravel; // Giờ đầu bếp bắt đầu di chuyển
    private LocalTime timeBeginCook;   // Giờ đầu bếp bắt đầu nấu ăn
    private BigDecimal platformFee; // Phí nền tảng (12% phí dịch vụ)
    private BigDecimal totalChefFeePrice; // Tổng phí dịch vụ đầu bếp (cooking + dish + travel + serving)
    private BigDecimal discountAmout;
    private BigDecimal totalCookTime;
    private LocalTime startTime;
    private String location;
    private Boolean isUpdated;
    private Long menuId;
    private Boolean chefBringIngredients;
    private List<BookingDetailItemRequestDto> dishes;

}
