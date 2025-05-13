package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.BookingDetailResponse;
import com.spring2025.vietchefs.models.payload.responseModel.BookingDetailsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewBookingDetailResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface BookingDetailService {
    BookingDetail createBookingDetail(Booking booking, BookingDetailRequestDto dto);
    BookingDetailResponse getBookingDetailById(Long id);
    BookingDetailsResponse getBookingDetailsByChef(Long userId,int pageNo, int pageSize, String sortBy, String sortDir);
    BookingDetailsResponse getBookingDetailsByChefStatus(Long userId,List<String> statusList,int pageNo, int pageSize, String sortBy, String sortDir);
    BookingDetailsResponse getBookingDetailsByCustomer(Long customerId,int pageNo, int pageSize, String sortBy, String sortDir);
    BookingDetailsResponse getBookingDetailsByCustomerStatus(Long customerId, List<String> statusList,int pageNo, int pageSize, String sortBy, String sortDir);
    BookingDetailsResponse getBookingDetailByBooking(Long bookingId,int pageNo, int pageSize, String sortBy, String sortDir);
    ReviewBookingDetailResponse calculateUpdatedBookingDetail(Long bookingDetailId, BookingDetailUpdateDto dto);
    BookingDetailDto updateBookingDetail(Long bookingDetailId, BookingDetailUpdateRequest bookingDetailUpdateRequest);
    BookingDetailDto updateStatusBookingDetailWatingCompleted(Long bookingDetailId,Long userId,List<MultipartFile> files, Double chefLat, Double chefLng);
    BookingDetailDto confirmBookingCompletionByCustomer(Long bookingDetailId,Long userId );
    BigDecimal refundBookingDetail(Long bookingDetailId);



}
