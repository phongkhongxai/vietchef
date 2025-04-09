package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service để tìm các khung giờ trống cho chef
 */
public interface AvailabilityFinderService {

    /**
     * Tìm các khung giờ trống cho một chef trong khoảng ngày
     * 
     * @param chefId ID của chef
     * @param startDate Ngày bắt đầu tìm kiếm
     * @param endDate Ngày kết thúc tìm kiếm
     * @param minDuration Thời lượng tối thiểu cần thiết (tính bằng phút)
     * @return Danh sách các khung giờ trống
     */
    List<AvailableTimeSlotResponse> findAvailableTimeSlotsForChef(
            Long chefId, 
            LocalDate startDate, 
            LocalDate endDate, 
            Integer minDuration);
    
    /**
     * Tìm các khung giờ trống cho chef hiện tại trong khoảng ngày
     * 
     * @param startDate Ngày bắt đầu tìm kiếm
     * @param endDate Ngày kết thúc tìm kiếm
     * @param minDuration Thời lượng tối thiểu cần thiết (tính bằng phút)
     * @return Danh sách các khung giờ trống
     */
    List<AvailableTimeSlotResponse> findAvailableTimeSlotsForCurrentChef(
            LocalDate startDate, 
            LocalDate endDate, 
            Integer minDuration);
    
    /**
     * Tìm các khung giờ trống cho một chef trong một ngày cụ thể
     * 
     * @param chefId ID của chef
     * @param date Ngày cần tìm khung giờ trống
     * @param minDuration Thời lượng tối thiểu cần thiết (tính bằng phút)
     * @return Danh sách các khung giờ trống
     */
    List<AvailableTimeSlotResponse> findAvailableTimeSlotsForChefByDate(
            Long chefId, 
            LocalDate date, 
            Integer minDuration);
    
    /**
     * Kiểm tra xem một khung giờ cụ thể có khả dụng cho chef hay không
     * 
     * @param chefId ID của chef
     * @param date Ngày cần kiểm tra
     * @param startTime Thời gian bắt đầu
     * @param endTime Thời gian kết thúc
     * @return True nếu khung giờ khả dụng, false nếu không
     */
    boolean isTimeSlotAvailable(
            Long chefId, 
            LocalDate date, 
            LocalTime startTime, 
            LocalTime endTime);
    
    /**
     * Tìm các khung giờ trống cho chef với tính toán thời gian nấu
     * 
     * @param chefId ID của chef
     * @param date Ngày cần tìm khung giờ trống
     * @param menuId ID của menu (có thể null)
     * @param dishIds Danh sách ID của các món ăn
     * @param guestCount Số lượng khách
     * @param minDuration Thời lượng tối thiểu cần thiết (tính bằng phút)
     * @return Danh sách các khung giờ trống đã điều chỉnh theo thời gian nấu
     */
    List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithCookingTime(
            Long chefId,
            LocalDate date,
            Long menuId,
            List<Long> dishIds,
            int guestCount,
            int maxDishesPerMeal,
            Integer minDuration);
    
    /**
     * Tìm các khung giờ trống cho chef hiện tại với tính toán thời gian nấu
     * 
     * @param date Ngày cần tìm khung giờ trống
     * @param menuId ID của menu (có thể null)
     * @param dishIds Danh sách ID của các món ăn
     * @param guestCount Số lượng khách
     * @param minDuration Thời lượng tối thiểu cần thiết (tính bằng phút)
     * @return Danh sách các khung giờ trống đã điều chỉnh theo thời gian nấu
     */
    List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithCookingTimeForCurrentChef(
            LocalDate date,
            Long menuId,
            List<Long> dishIds,
            int guestCount,
            Integer minDuration);
} 