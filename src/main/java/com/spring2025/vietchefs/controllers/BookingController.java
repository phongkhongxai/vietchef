package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.BookingRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingPriceRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.BookingDetailsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.MenuResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewSingleBookingResponse;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.MenuService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;
    @Autowired
    private BookingDetailService bookingDetailService;

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> createBookingSingle(@RequestBody BookingRequestDto dto) {
        BookingResponseDto bookingResponseDto = bookingService.createSingleBooking(dto);
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.CREATED);

    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/calculate-single-booking")
    public ResponseEntity<?> calculatePriceBookingSingle(@RequestBody BookingPriceRequestDto dto) {
        ReviewSingleBookingResponse reviewSingleBookingResponse = bookingService.calculateFinalPriceForSingleBooking(dto);
        return new ResponseEntity<>(reviewSingleBookingResponse, HttpStatus.OK);

    }

    @GetMapping("/{id}/booking-details")
    public BookingDetailsResponse getBookingDetailByBookingId(@PathVariable("id") Long bookingId,
                                                              @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                                              @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                                              @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
                                                              @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir) {

        return bookingDetailService.getBookingDetailByBooking(bookingId,pageNo, pageSize, sortBy, sortDir);

    }


}
