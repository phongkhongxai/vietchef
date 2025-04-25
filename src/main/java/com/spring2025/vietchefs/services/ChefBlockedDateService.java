package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRangeRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefBlockedDateResponse;

import java.time.LocalDate;
import java.util.List;

public interface ChefBlockedDateService {
    /**
     * Lấy thông tin ngày bị chặn theo ID
     */
    ChefBlockedDateResponse getBlockedDateById(Long blockId);

    /**
     * Cập nhật thông tin ngày bị chặn
     */
    ChefBlockedDateResponse updateBlockedDate(ChefBlockedDateUpdateRequest request);

    /**
     * Xóa mềm thông tin ngày bị chặn
     */
    void deleteBlockedDate(Long blockId);

    /**
     * Tạo ngày bị chặn mới cho chef hiện tại
     */
    ChefBlockedDateResponse createBlockedDateForCurrentChef(ChefBlockedDateRequest request);

    /**
     * Tạo nhiều ngày bị chặn trong một khoảng thời gian cho chef hiện tại
     */
    List<ChefBlockedDateResponse> createBlockedDateRangeForCurrentChef(ChefBlockedDateRangeRequest request);

    /**
     * Lấy tất cả ngày bị chặn của chef hiện tại
     */
    List<ChefBlockedDateResponse> getBlockedDatesForCurrentChef();

    /**
     * Lấy ngày bị chặn của chef hiện tại trong khoảng ngày
     */
    List<ChefBlockedDateResponse> getBlockedDatesForCurrentChefBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Lấy ngày bị chặn của chef hiện tại vào một ngày cụ thể
     */
    List<ChefBlockedDateResponse> getBlockedDatesForCurrentChefByDate(LocalDate date);
} 