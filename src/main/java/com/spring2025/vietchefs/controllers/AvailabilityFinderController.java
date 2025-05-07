package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.requestModel.AvailableTimeSlotRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;
import com.spring2025.vietchefs.services.AvailabilityFinderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityFinderController {

    @Autowired
    private AvailabilityFinderService availabilityFinderService;
    
    
    /**
     * Tìm các khung giờ trống cho chef dựa trên vị trí của khách hàng, tính toán thời gian nấu, thời gian di chuyển
     * và thời gian nghỉ giữa các booking.
     * 
     * Phương thức này tính toán sự khả dụng của chef dựa trên:
     * - Thời gian di chuyển từ vị trí chef đến vị trí khách hàng (sử dụng DistanceService)
     * - Thời gian nấu các món ăn được chọn (sử dụng CalculateService)
     * - Thời gian nghỉ bắt buộc 30 phút giữa các booking
     * - Lịch làm việc hiện tại của chef và các booking đã xác nhận
     */
    @GetMapping("/chef/{chefId}/single-date")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsInSingleDate(
            @PathVariable Long chefId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String customerLocation,
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) List<Long> dishIds,
            @RequestParam int guestCount,
            @RequestParam(defaultValue = "6") int maxDishesPerMeal) {
        
        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsWithInSingleDate(
                    chefId, date, customerLocation, menuId, dishIds, 
                    guestCount, maxDishesPerMeal);
        
        return ResponseEntity.ok(availableSlots);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/chef/{chefId}/multiple-dates")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsInMultipleDates(
            @PathVariable Long chefId,
            @RequestParam String customerLocation,
            @RequestParam int guestCount,
            @RequestParam(defaultValue = "6") int maxDishesPerMeal,
            @RequestBody List<AvailableTimeSlotRequest> availableTimeSlotRequests) {

        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsWithInMultipleDates(
                        chefId, customerLocation,guestCount, maxDishesPerMeal,availableTimeSlotRequests);

        return ResponseEntity.ok(availableSlots);
    }
} 