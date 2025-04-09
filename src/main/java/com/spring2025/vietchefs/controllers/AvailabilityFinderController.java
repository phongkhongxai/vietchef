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
     * Tìm các khung giờ trống cho chef hiện tại
     */
    @GetMapping("/chef/me")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsForCurrentChef(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsForCurrentChef(startDate, endDate);
        
        return ResponseEntity.ok(availableSlots);
    }
    
    /**
     * Tìm các khung giờ trống cho chef cụ thể theo ID
     */
    @GetMapping("/chef/{chefId}")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsForChef(
            @PathVariable Long chefId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsForChef(chefId, startDate, endDate);
        
        return ResponseEntity.ok(availableSlots);
    }
    
    /**
     * Tìm các khung giờ trống cho chef cụ thể theo ID và ngày
     */
    @GetMapping("/chef/{chefId}/date/{date}")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsForChefByDate(
            @PathVariable Long chefId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsForChefByDate(chefId, date);
        
        return ResponseEntity.ok(availableSlots);
    }
    
    /**
     * Kiểm tra xem một khung giờ cụ thể có khả dụng hay không
     */
    @GetMapping("/chef/{chefId}/check")
    public ResponseEntity<Boolean> checkTimeSlotAvailability(
            @PathVariable Long chefId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        
        boolean isAvailable = availabilityFinderService
                .isTimeSlotAvailable(chefId, date, startTime, endTime);
        
        return ResponseEntity.ok(isAvailable);
    }
    
    /**
     * Tìm các khung giờ trống cho chef với tính toán thời gian nấu
     * Các khung giờ đã điều chỉnh theo thời gian nấu ăn và bao gồm cả 1 giờ đệm cho việc di chuyển trước booking tiếp theo.
     * Ví dụ: Nếu chef có lịch trống từ 14:00-18:00 nhưng đã có booking lúc 18:00 và cần bắt đầu di chuyển lúc 17:00, 
     * thì khung giờ thực sự khả dụng sẽ được điều chỉnh thành 14:00-16:00 (trừ đi 1 giờ cho buffer di chuyển).
     */
    @GetMapping("/chef/{chefId}/cooking-time")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsWithCookingTime(
            @PathVariable Long chefId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) List<Long> dishIds,
            @RequestParam int guestCount,
            @RequestParam(defaultValue = "6") int maxDishesPerMeal) {
        
        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsWithCookingTime(chefId, date, menuId, dishIds, guestCount, maxDishesPerMeal);
        
        return ResponseEntity.ok(availableSlots);
    }
    
    /**
     * Tìm các khung giờ trống cho chef hiện tại với tính toán thời gian nấu
     * Các khung giờ đã điều chỉnh theo thời gian nấu ăn và bao gồm cả 1 giờ đệm cho việc di chuyển trước booking tiếp theo.
     * Ví dụ: Nếu chef có lịch trống từ 14:00-18:00 nhưng đã có booking lúc 18:00 và cần bắt đầu di chuyển lúc 17:00, 
     * thì khung giờ thực sự khả dụng sẽ được điều chỉnh thành 14:00-16:00 (trừ đi 1 giờ cho buffer di chuyển).
     */
    @GetMapping("/chef/me/cooking-time")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsWithCookingTimeForCurrentChef(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) List<Long> dishIds,
            @RequestParam int guestCount) {
        
        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsWithCookingTimeForCurrentChef(date, menuId, dishIds, guestCount);
        
        return ResponseEntity.ok(availableSlots);
    }
    
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
    @GetMapping("/chef/{chefId}/location-constraints")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsWithLocationConstraints(
            @PathVariable Long chefId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String customerLocation,
            @RequestParam(required = false) Long menuId,
            @RequestParam(required = false) List<Long> dishIds,
            @RequestParam int guestCount,
            @RequestParam(defaultValue = "6") int maxDishesPerMeal) {
        
        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsWithLocationConstraints(
                    chefId, date, customerLocation, menuId, dishIds, 
                    guestCount, maxDishesPerMeal);
        
        return ResponseEntity.ok(availableSlots);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/chef/{chefId}/location-constraints-vip")
    public ResponseEntity<List<AvailableTimeSlotResponse>> findAvailableTimeSlotsWithLocationConstraintsVip(
            @PathVariable Long chefId,
            @RequestParam String customerLocation,
            @RequestParam int guestCount,
            @RequestParam(defaultValue = "6") int maxDishesPerMeal,
            @RequestBody List<AvailableTimeSlotRequest> availableTimeSlotRequests) {

        List<AvailableTimeSlotResponse> availableSlots = availabilityFinderService
                .findAvailableTimeSlotsWithLocationConstraints(
                        chefId, customerLocation,guestCount, maxDishesPerMeal,availableTimeSlotRequests);

        return ResponseEntity.ok(availableSlots);
    }
} 