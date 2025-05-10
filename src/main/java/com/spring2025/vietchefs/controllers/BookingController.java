package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import com.spring2025.vietchefs.models.payload.dto.BookingRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.*;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.PaymentCycleService;
import com.spring2025.vietchefs.services.UserService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
    @Operation(
            summary = "Lấy danh sách đơn đặt của người dùng hiện tại. (ví dụ: PENDING, PAID, CONFIRMED, CANCELED, COMPLETED)"
    )
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @GetMapping("/my-bookings")
    public BookingsResponse getBookingsMySelf(
           @AuthenticationPrincipal UserDetails userDetails,
           @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        if (status != null && !status.isEmpty()) {
            List<String> statusList = switch (status.toUpperCase()) {
                case "PENDING" -> List.of("PENDING", "PENDING_FIRST_CYCLE");
                case "PAID" -> List.of("PAID","DEPOSITED","PAID_FIRST_CYCLE");
                case "CONFIRMED" -> List.of("CONFIRMED", "CONFIRMED_PAID","CONFIRMED_PARTIALLY_PAID");
                case "CANCELED" -> List.of("CANCELED","OVERDUE","REJECTED");
                default -> List.of(status.toUpperCase());
            };
            return bookingService.getBookingsByCustomerIdAndStatus(bto.getId(), statusList, pageNo, pageSize, sortBy, sortDir);
        } else {
            return bookingService.getBookingsByCustomerId(bto.getId(), pageNo, pageSize, sortBy, sortDir);
        }
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Lấy danh sách đơn đặt của người dùng hiện tại. (ví dụ: PAID, CONFIRMED, CANCELED, COMPLETED)"
    )
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/chefs/my-bookings")
    public BookingsResponse getBookingsChefSelf(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        if (status != null && !status.isEmpty()) {
            List<String> statusList = switch (status.toUpperCase()) {
                case "PAID" -> List.of("PAID","DEPOSITED","PAID_FIRST_CYCLE");
                case "CONFIRMED" -> List.of("CONFIRMED", "CONFIRMED_PAID","CONFIRMED_PARTIALLY_PAID");
                case "CANCELED" -> List.of("CANCELED","OVERDUE","REJECTED");
                default -> List.of(status.toUpperCase());
            };
            return bookingService.getBookingsByChefIdAndStatus(bto.getId(), statusList, pageNo, pageSize, sortBy, sortDir);
        } else {
            return bookingService.getBookingsByChefId(bto.getId(), pageNo, pageSize, sortBy, sortDir);
        }
    }
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/booking-details/chefs")
    public BookingDetailsResponse getBookingDetailOfChef(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        if (status != null && !status.isEmpty()) {
            List<String> statusList = List.of(status.toUpperCase());
            return bookingDetailService.getBookingDetailsByChefStatus(bto.getId(), statusList, pageNo, pageSize, sortBy, sortDir);
        } else {
            return bookingDetailService.getBookingDetailsByChef(bto.getId(), pageNo, pageSize, sortBy, sortDir);
        }
    }
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @GetMapping("/booking-details/user")
    public BookingDetailsResponse getBookingDetailOfUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        if (status != null && !status.isEmpty()) {
            List<String> statusList = List.of(status.toUpperCase());
            return bookingDetailService.getBookingDetailsByCustomerStatus(bto.getId(), statusList, pageNo, pageSize, sortBy, sortDir);
        } else {
            return bookingDetailService.getBookingDetailsByCustomer(bto.getId(), pageNo, pageSize, sortBy, sortDir);
        }
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
    public ResponseEntity<ApiResponse<BookingResponseDto>> depositBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookingId) {

        UserDto user = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        ApiResponse<BookingResponseDto> response = bookingService.depositBooking(bookingId, user.getId());

        if (!response.isSuccess()) {
            if (response.getData() != null) {
                // Lỗi nhưng vẫn trả về dữ liệu → dùng 200 OK
                return ResponseEntity.ok(response);
            } else {
                // Lỗi và không có dữ liệu → trả về 400 Bad Request
                return ResponseEntity.badRequest().body(response);
            }
        }

        // Thành công → 200 OK
        return ResponseEntity.ok(response);
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
        BookingDetailResponse bl = bookingDetailService.getBookingDetailById(bookingDetailId);
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
    public ResponseEntity<?> cancelSingleBookingFromCustomer(@PathVariable Long bookingid, @AuthenticationPrincipal UserDetails userDetails) {
        UserDto userDto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.cancelSingleBooking(bookingid,userDto.getId());
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping("/long-term/cancel/{bookingid}")
    public ResponseEntity<?> cancelLongTermBookingFromCustomer(@PathVariable Long bookingid, @AuthenticationPrincipal UserDetails userDetails) {
        UserDto userDto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.cancelLongTermBooking2(bookingid,userDto.getId());
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping("/single/cancel-chef/{bookingid}")
    public ResponseEntity<?> cancelSingleBookingFromChef(@PathVariable Long bookingid, @AuthenticationPrincipal UserDetails userDetails) {
        UserDto userDto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.cancelSingleBookingFromChef(bookingid,userDto.getId());
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping("/long-term/cancel-chef/{bookingid}")
    public ResponseEntity<?> cancelLongTermBookingFromChef(@PathVariable Long bookingid, @AuthenticationPrincipal UserDetails userDetails) {
        UserDto userDto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        BookingResponseDto bookingResponseDto = bookingService.cancelLongTermBookingFromChef(bookingid,userDto.getId());
        return new ResponseEntity<>(bookingResponseDto, HttpStatus.OK);
    }

//    @SecurityRequirement(name = "Bearer Authentication")
//    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
//    @PutMapping("/payment-cycles/cancel/{cycleId}")
//    public ResponseEntity<?> cancelLongTermBookingByPayCycle(@PathVariable Long cycleId) {
//        PaymentCycleResponseDto cancelPaymentCycle = paymentCycleService.cancelPaymentCycle(cycleId);
//        return new ResponseEntity<>(cancelPaymentCycle, HttpStatus.OK);
//    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping(value = "/booking-details/{bookingDetailId}/complete-chef", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateWaitingCustomer(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long bookingDetailId, @RequestParam("files") List<MultipartFile> files) {
        UserDto userDto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        BookingDetailDto bookingDetail = bookingDetailService.updateStatusBookingDetailWatingCompleted(bookingDetailId, userDto.getId(), files);
        return new ResponseEntity<>(bookingDetail, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping("/booking-details/{bookingDetailId}/complete-customer")
    public ResponseEntity<?> completeBookingDetailFromCustomer(@AuthenticationPrincipal UserDetails userDetails,@PathVariable Long bookingDetailId) {
        UserDto userDto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(), userDetails.getUsername());
        BookingDetailDto bookingDetail = bookingDetailService.confirmBookingCompletionByCustomer(bookingDetailId, userDto.getId());
        return new ResponseEntity<>(bookingDetail, HttpStatus.OK);
    }
    @GetMapping("/unavailable-dates")
    public ResponseEntity<Set<LocalDate>> getUnavailableDates(
            @RequestParam Long chefId) {
        Set<LocalDate> unavailableDates = bookingService.getFullyBookedDates(chefId);
        return ResponseEntity.ok(unavailableDates);
    }




}
