package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response model để trả về thông tin về các khung giờ trống có thể đặt lịch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTimeSlotResponse {
    
    /**
     * ID của chef
     */
    private Long chefId;
    
    /**
     * Tên của chef
     */
    private String chefName;
    
    /**
     * Ngày của khung giờ trống
     */
    private LocalDate date;
    
    /**
     * Thời gian bắt đầu của khung giờ trống
     */
    private LocalTime startTime;
    
    /**
     * Thời gian kết thúc của khung giờ trống
     */
    private LocalTime endTime;
    
    /**
     * Thời lượng của khung giờ (phút)
     */
    private Integer durationMinutes;
    
    /**
     * Ghi chú (nếu có)
     */
    private String note;
} 