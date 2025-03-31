package com.spring2025.vietchefs.models.payload.requestModel;

import lombok.Data;

import java.util.List;

@Data
public class BookingLTPriceRequestDto {
    private Long chefId;
    private Long packageId;
    private int guestCount;
    private String location;
    private List<BookingDetailPriceLTRequest> bookingDetails;
}
