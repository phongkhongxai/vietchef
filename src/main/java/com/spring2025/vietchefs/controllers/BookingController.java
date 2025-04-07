package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import com.spring2025.vietchefs.models.payload.dto.BookingRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.*;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.services.*;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserService userService;
    @Autowired
    private PaymentCycleService paymentCycleService;
    @Autowired
    private BookingDetailService bookingDetailService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_CHEF')")
    @GetMapping("/users/my-bookings")
    public BookingsResponse getBookingsMySelf(
           // @RequestParam(value = "customerId", required = false) Long customerId,
           @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return bookingService.getBookingsByCustomerId(bto.getId(),pageNo, pageSize, sortBy, sortDir);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_CHEF')")
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable Long bookingId){
        BookingResponseDto dto = bookingService.getBookingById(bookingId);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> createBookingSingle(@RequestBody BookingRequestDto dto) {
        BookingResponseDto bookingResponseDto = bookingService.createSingleBooking(dto);
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.CREATED);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<?> paySingleBooking(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long bookingId) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.paymentBooking(bookingId, bto.getId());
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);

    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/{bookingId}/deposit")
    public ResponseEntity<?> depositBooking(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long bookingId) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.depositBooking(bookingId, bto.getId());
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);

    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping("/{bookingId}/confirm")
    public ResponseEntity<?> confirmBooking(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long bookingId) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.updateBookingStatusConfirm(bookingId, bto.getId(),true);
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);

    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<?> rejectBooking(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long bookingId) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.updateBookingStatusConfirm(bookingId, bto.getId(),false);
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);

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

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/calculate-long-term-booking")
    public ResponseEntity<?> calculatePriceLongTermBooking(@RequestBody BookingLTPriceRequestDto dto) {
        ReviewLongTermBookingResponse reviewResponse = bookingService.calculateFinalPriceForLongTermBooking(dto);
        return new ResponseEntity<>(reviewResponse, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/booking-details/{bookingDetailId}/calculate")
    public ResponseEntity<?> calculateUpdateBookingDetail(@PathVariable Long bookingDetailId,@RequestBody BookingDetailUpdateDto dto) {
        ReviewBookingDetailResponse reviewResponse = bookingDetailService.calculateUpdatedBookingDetail(bookingDetailId, dto);
        return new ResponseEntity<>(reviewResponse, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/long-term")
    public ResponseEntity<?> createLongTermBooking(@RequestBody BookingRequestDto dto) {
        BookingResponseDto bookingResponseDto = bookingService.createLongtermBooking(dto);
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.CREATED);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping("/booking-details/{bookingDetailId}")
    public ResponseEntity<?> updateBookingDetail(@PathVariable Long bookingDetailId,
                                                 @RequestBody BookingDetailUpdateRequest updateRequest) {
        BookingDetailDto bookingDetailDto = bookingDetailService.updateBookingDetail(bookingDetailId, updateRequest);
        return new ResponseEntity<>(bookingDetailDto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{bookingId}/payment-cycles")
    public ResponseEntity<?> getPaymentCyclesWithDetails(@PathVariable Long bookingId) {
        List<PaymentCycleResponse> paymentCycles = bookingService.getPaymentCyclesWithDetails(bookingId);
        return new ResponseEntity<>(paymentCycles, HttpStatus.OK);
    }

    @GetMapping("/booking-details/{bookingDetailId}")
    public ResponseEntity<?> getBookingDetailByid(@PathVariable Long bookingDetailId) {
        BookingDetailDto bl = bookingDetailService.getBookingDetailById(bookingDetailId);
        return new ResponseEntity<>(bl, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/payment-cycles/{paymentCycleId}/pay")
    public ResponseEntity<?> payForPaymentCycle(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long paymentCycleId) {
        UserDto userDto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        PaymentCycleResponse paymentResponse = bookingService.payForPaymentCycle(paymentCycleId, userDto.getId());
        return new ResponseEntity<>(paymentResponse, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping("/single/cancel/{bookingid}")
    public ResponseEntity<?> cancelSingleBooking(@PathVariable Long bookingid) {
        BookingResponseDto bookingResponseDto = bookingService.cancelSingleBooking(bookingid);
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping("/long-term/cancel/{bookingid}")
    public ResponseEntity<?> cancelLongTermBooking(@PathVariable Long bookingid) {
        BookingResponseDto bookingResponseDto = bookingService.cancelLongTermBooking(bookingid);
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping("/payment-cycles/cancel/{cycleId}")
    public ResponseEntity<?> cancelLongTermBookingByPayCycle(@PathVariable Long cycleId) {
        PaymentCycleResponseDto cancelPaymentCycle = paymentCycleService.cancelPaymentCycle(cycleId);
        return new ResponseEntity<>(cancelPaymentCycle, HttpStatus.OK);
    }



}
