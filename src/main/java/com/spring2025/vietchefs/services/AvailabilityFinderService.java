package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.payload.requestModel.AvailableTimeSlotRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service để tìm các khung giờ trống cho chef
 */
public interface AvailabilityFinderService {
    
    /**
     * Tìm các khung giờ trống cho chef với tính toán thời gian nấu, thời gian di chuyển và thời gian nghỉ giữa các booking
     * 
     * @param chefId ID của chef
     * @param date Ngày cần tìm khung giờ trống
     * @param customerLocation Địa chỉ của khách hàng
     * @param menuId ID của menu (có thể null)
     * @param dishIds Danh sách ID của các món ăn (có thể null nếu menuId không null)
     * @param guestCount Số lượng khách
     * @param maxDishesPerMeal Số lượng món ăn tối đa trong bữa ăn (dùng khi không có menuId và dishIds)
     * @return Danh sách các khung giờ trống đã điều chỉnh theo thời gian nấu, thời gian di chuyển và thời gian nghỉ
     */
    List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithInSingleDate(
            Long chefId,
            LocalDate date,
            String customerLocation,
            Long menuId,
            List<Long> dishIds,
            int guestCount,
            int maxDishesPerMeal);
            
    /**
     * Tìm các khung giờ trống cho chef trên nhiều ngày với tính toán thời gian nấu, thời gian di chuyển
     * 
     * @param chefId ID của chef
     * @param customerLocation Địa chỉ của khách hàng
     * @param guestCount Số lượng khách
     * @param maxDishesPerMeal Số lượng món ăn tối đa trong bữa ăn
     * @param requests Danh sách các yêu cầu tìm khung giờ trống, mỗi yêu cầu chứa ngày và thông tin menu/món ăn
     * @return Danh sách các khung giờ trống đã điều chỉnh theo thời gian nấu, thời gian di chuyển và thời gian nghỉ
     */
    List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithInMultipleDates(
            Long chefId,
            String customerLocation,
            int guestCount,
            int maxDishesPerMeal,
            List<AvailableTimeSlotRequest> requests);
} 