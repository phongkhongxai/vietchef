package com.spring2025.vietchefs.models.payload.requestModel;

import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailUpdateDto {
    private Long menuId; // ID của Menu (nếu chọn cả Menu)

    private List<Long> extraDishIds; // ID các món ăn bổ sung

    private Boolean isServing; // Khách có chọn phục vụ không?
    List<BookingDetailItemRequestDto> dishes;

}
