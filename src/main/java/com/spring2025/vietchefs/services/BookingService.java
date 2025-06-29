package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.BookingRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingLTPriceRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingPriceRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface BookingService {
    BookingsResponse getBookingsByCustomerId(Long customerId, int pageNo, int pageSize, String sortBy, String sortDir);
    BookingsResponse getBookingsByChefId(Long userId, int pageNo, int pageSize, String sortBy, String sortDir);
    BookingsResponse getBookingsByCustomerIdAndStatus(Long customerId,List<String> statusList, int pageNo, int pageSize, String sortBy, String sortDir);
    BookingsResponse getBookingsByChefIdAndStatus(Long userId,List<String> statusList, int pageNo, int pageSize, String sortBy, String sortDir);
    BookingResponseDto getBookingById(Long id);
    BookingResponseDto createSingleBooking(BookingRequestDto dto);
    BookingResponseDto createLongtermBooking(BookingRequestDto dto);
    ReviewSingleBookingResponse calculateFinalPriceForSingleBooking(BookingPriceRequestDto dto);
    ReviewLongTermBookingResponse calculateFinalPriceForLongTermBooking(BookingLTPriceRequestDto dto);
    BookingResponseDto updateBookingStatusConfirm(Long bookingId, Long userId,boolean isConfirmed);
    BookingResponseDto paymentBooking(Long bookingId, Long userId);
    List<PaymentCycleResponse> getPaymentCyclesWithDetails(Long bookingId);
    PaymentCycleResponse payForPaymentCycle(Long paymentCycleId, Long userId);
    ApiResponse<BookingResponseDto> depositBooking(Long bookingId, Long userId);
    BookingResponseDto cancelSingleBooking(Long bookingId,Long userId);
    BookingResponseDto cancelSingleBookingFromChef(Long bookingId, Long userId);
    BookingResponseDto cancelLongTermBookingFromChef(Long bookingId,Long userId);
    BookingResponseDto cancelLongTermBooking(Long bookingId);
    BookingResponseDto cancelLongTermBooking2(Long bookingId, Long userId);
    Set<LocalDate> getFullyBookedDates(Long chefId);



}
