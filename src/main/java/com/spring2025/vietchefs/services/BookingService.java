package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.BookingRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingPriceRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewSingleBookingResponse;

import java.math.BigDecimal;

public interface BookingService {
    BookingResponseDto createSingleBooking(BookingRequestDto dto);
    ReviewSingleBookingResponse calculateFinalPriceForSingleBooking(BookingPriceRequestDto dto);
}
