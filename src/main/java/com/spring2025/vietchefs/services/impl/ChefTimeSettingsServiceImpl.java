package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefTimeSettings;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefTimeSettingsRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefTimeSettingsResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefTimeSettingsRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ChefTimeSettingsService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ChefTimeSettingsServiceImpl implements ChefTimeSettingsService {

    @Autowired
    private ChefTimeSettingsRepository timeSettingsRepository;

    @Autowired
    private ChefRepository chefRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ChefTimeSettingsResponse getTimeSettingsForCurrentChef() {
        Chef chef = getCurrentChef();
        ChefTimeSettings timeSettings = getOrCreateTimeSettings(chef);
        return modelMapper.map(timeSettings, ChefTimeSettingsResponse.class);
    }

    @Override
    public ChefTimeSettingsResponse updateTimeSettingsForCurrentChef(ChefTimeSettingsRequest request) {
        Chef chef = getCurrentChef();
        ChefTimeSettings timeSettings = getOrCreateTimeSettings(chef);
        
        // Cập nhật các giá trị từ request nếu được cung cấp
        if (request.getStandardPrepTime() != null) {
            timeSettings.setStandardPrepTime(request.getStandardPrepTime());
        }
        if (request.getStandardCleanupTime() != null) {
            timeSettings.setStandardCleanupTime(request.getStandardCleanupTime());
        }
        if (request.getTravelBufferPercentage() != null) {
            timeSettings.setTravelBufferPercentage(request.getTravelBufferPercentage());
        }
        if (request.getCookingEfficiencyFactor() != null) {
            timeSettings.setCookingEfficiencyFactor(request.getCookingEfficiencyFactor());
        }
        if (request.getMinBookingNoticeHours() != null) {
            timeSettings.setMinBookingNoticeHours(request.getMinBookingNoticeHours());
        }
        if (request.getMaxBookingDaysAhead() != null) {
            timeSettings.setMaxBookingDaysAhead(request.getMaxBookingDaysAhead());
        }
        if (request.getMaxDishesPerSession() != null) {
            timeSettings.setMaxDishesPerSession(request.getMaxDishesPerSession());
        }
        if (request.getMaxGuestsPerSession() != null) {
            timeSettings.setMaxGuestsPerSession(request.getMaxGuestsPerSession());
        }
        if (request.getServiceRadiusKm() != null) {
            timeSettings.setServiceRadiusKm(request.getServiceRadiusKm());
        }
        if (request.getMaxSessionsPerDay() != null) {
            timeSettings.setMaxSessionsPerDay(request.getMaxSessionsPerDay());
        }
        
        ChefTimeSettings savedSettings = timeSettingsRepository.save(timeSettings);
        return modelMapper.map(savedSettings, ChefTimeSettingsResponse.class);
    }

    @Override
    public ChefTimeSettingsResponse resetTimeSettingsToDefault() {
        Chef chef = getCurrentChef();
        ChefTimeSettings timeSettings = getOrCreateTimeSettings(chef);
        
        // Đặt lại các giá trị mặc định
        timeSettings.setStandardPrepTime(0);
        timeSettings.setStandardCleanupTime(0);
        timeSettings.setTravelBufferPercentage(0);
        timeSettings.setCookingEfficiencyFactor(new BigDecimal("0.0"));
        timeSettings.setMinBookingNoticeHours(24);
        timeSettings.setMaxBookingDaysAhead(7);
        timeSettings.setMaxDishesPerSession(5);
        timeSettings.setMaxGuestsPerSession(8);
        timeSettings.setServiceRadiusKm(15);
        timeSettings.setMaxSessionsPerDay(3);
        
        ChefTimeSettings savedSettings = timeSettingsRepository.save(timeSettings);
        return modelMapper.map(savedSettings, ChefTimeSettingsResponse.class);
    }

    @Override
    public ChefTimeSettingsResponse getTimeSettingsById(Long settingId) {
        ChefTimeSettings timeSettings = timeSettingsRepository.findById(settingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Time settings not found with id: " + settingId));
        
        // Kiểm tra xem người dùng hiện tại có phải là chef sở hữu cài đặt này không
        Chef currentChef = getCurrentChef();
        if (!timeSettings.getChef().getId().equals(currentChef.getId())) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "You are not allowed to access these time settings");
        }
        
        return modelMapper.map(timeSettings, ChefTimeSettingsResponse.class);
    }
    
    /**
     * Lấy chef từ người dùng hiện tại
     */
    private Chef getCurrentChef() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        return chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));
    }
    
    /**
     * Lấy cài đặt thời gian hiện có của chef hoặc tạo mới nếu chưa có
     */
    private ChefTimeSettings getOrCreateTimeSettings(Chef chef) {
        return timeSettingsRepository.findByChef(chef)
                .orElseGet(() -> {
                    ChefTimeSettings newSettings = new ChefTimeSettings();
                    newSettings.setChef(chef);
                    // Các giá trị mặc định đã được thiết lập trong entity
                    return timeSettingsRepository.save(newSettings);
                });
    }
} 