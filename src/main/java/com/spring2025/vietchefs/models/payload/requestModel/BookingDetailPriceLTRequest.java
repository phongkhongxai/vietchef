package com.spring2025.vietchefs.models.payload.requestModel;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingDetailPriceLTRequest {
    @Future(message = "sessionDate phải là một ngày trong tương lai")
    private LocalDate sessionDate;
    private LocalTime startTime;
    private Long menuId;
    private List<Long> extraDishIds;
    private Boolean isDishSelected;
    private List<BookingDetailItemRequestDto> dishes;

}
