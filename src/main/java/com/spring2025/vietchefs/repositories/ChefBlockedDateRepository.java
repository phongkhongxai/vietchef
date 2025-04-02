package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefBlockedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ChefBlockedDateRepository extends JpaRepository<ChefBlockedDate, Long> {
    /**
     * Tìm tất cả ngày bị chặn của một chef mà chưa bị xóa
     */
    List<ChefBlockedDate> findByChefAndIsDeletedFalse(Chef chef);
    
    /**
     * Tìm tất cả ngày bị chặn trong khoảng ngày cụ thể
     */
    List<ChefBlockedDate> findByChefAndBlockedDateBetweenAndIsDeletedFalse(
            Chef chef, LocalDate startDate, LocalDate endDate);
    
    /**
     * Tìm tất cả ngày bị chặn cho một ngày cụ thể
     */
    List<ChefBlockedDate> findByChefAndBlockedDateAndIsDeletedFalse(
            Chef chef, LocalDate blockedDate);
    
    /**
     * Kiểm tra xem một khoảng thời gian có bị chặn không
     */
    boolean existsByChefAndBlockedDateAndStartTimeBeforeAndEndTimeAfterAndIsDeletedFalse(
            Chef chef, LocalDate blockedDate, LocalTime endTime, LocalTime startTime);
} 