package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.BookingDetailsResponse;

import java.math.BigDecimal;

public interface BookingDetailService {
    BookingDetail createBookingDetail(Booking booking, BookingDetailRequestDto dto);
    BookingDetailsResponse getBookingDetailByBooking(Long bookingId,int pageNo, int pageSize, String sortBy, String sortDir);

}
