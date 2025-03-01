package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

import java.util.List;
@Data
public class BookingPriceRequestDto {
    private Long chefId;
    private Boolean isServing;
    private List<BookingDetailPriceRequestDto> bookingDetails;
}
