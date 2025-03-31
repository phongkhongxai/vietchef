package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.BookingDetailsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewBookingDetailResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BookingDetailService {
    BookingDetail createBookingDetail(Booking booking, BookingDetailRequestDto dto);
    BookingDetailDto getBookingDetailById(Long id);
    BookingDetailsResponse getBookingDetailByBooking(Long bookingId,int pageNo, int pageSize, String sortBy, String sortDir);
    ReviewBookingDetailResponse calculateUpdatedBookingDetail(Long bookingDetailId, BookingDetailUpdateDto dto);
    BookingDetailDto updateBookingDetail(Long bookingDetailId, BookingDetailUpdateRequest bookingDetailUpdateRequest);

}
