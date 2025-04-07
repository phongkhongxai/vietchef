package com.spring2025.vietchefs.models.payload.requestModel;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingDetailPriceRequestDto {
    @Future(message = "sessionDate phải là một ngày trong tương lai")
    private LocalDate sessionDate;
    //private Boolean isServing;
    private LocalTime startTime;
    //private LocalTime endTime;
    private String location;
    private Long menuId;
    private List<Long> extraDishIds;
    private List<BookingDetailItemRequestDto> dishes;
}
