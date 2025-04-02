package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ChefTimeSettingsRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefTimeSettingsResponse;

public interface ChefTimeSettingsService {
    /**
     * Lấy cài đặt thời gian của chef hiện tại
     */
    ChefTimeSettingsResponse getTimeSettingsForCurrentChef();
    
    /**
     * Cập nhật cài đặt thời gian của chef hiện tại
     */
    ChefTimeSettingsResponse updateTimeSettingsForCurrentChef(ChefTimeSettingsRequest request);
    
    /**
     * Khôi phục cài đặt thời gian mặc định cho chef hiện tại
     */
    ChefTimeSettingsResponse resetTimeSettingsToDefault();
    
    /**
     * Lấy cài đặt thời gian theo ID
     */
    ChefTimeSettingsResponse getTimeSettingsById(Long settingId);
} 