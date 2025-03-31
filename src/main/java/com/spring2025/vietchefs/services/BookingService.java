package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.BookingRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingLTPriceRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingPriceRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.PaymentCycleResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewLongTermBookingResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewSingleBookingResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BookingService {
    BookingResponseDto createSingleBooking(BookingRequestDto dto);
    BookingResponseDto createLongtermBooking(BookingRequestDto dto);
    ReviewSingleBookingResponse calculateFinalPriceForSingleBooking(BookingPriceRequestDto dto);
    ReviewLongTermBookingResponse calculateFinalPriceForLongTermBooking(BookingLTPriceRequestDto dto);
    BookingResponseDto updateBookingStatusConfirm(Long bookingId, Long userId,boolean isConfirmed);
    BookingResponseDto paymentBooking(Long bookingId, Long userId);
    List<PaymentCycleResponse> getPaymentCyclesWithDetails(Long bookingId);
    PaymentCycleResponse payForPaymentCycle(Long paymentCycleId, Long userId);
    BookingResponseDto depositBooking(Long bookingId, Long userId);
    BookingResponseDto cancelSingleBooking(Long bookingId);
    BookingResponseDto cancelLongTermBooking(Long bookingId);


}
