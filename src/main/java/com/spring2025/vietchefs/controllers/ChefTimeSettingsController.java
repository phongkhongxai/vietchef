package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.requestModel.ChefTimeSettingsRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefTimeSettingsResponse;
import com.spring2025.vietchefs.services.ChefTimeSettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chef-time-settings")
@RequiredArgsConstructor
public class ChefTimeSettingsController {

    private final ChefTimeSettingsService timeSettingsService;

    /**
     * Lấy cài đặt thời gian hiện tại của chef
     * Endpoint: GET /api/v1/chef-time-settings/me
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/me")
    public ResponseEntity<ChefTimeSettingsResponse> getTimeSettingsForCurrentChef() {
        ChefTimeSettingsResponse response = timeSettingsService.getTimeSettingsForCurrentChef();
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật cài đặt thời gian cho chef hiện tại
     * Endpoint: PUT /api/v1/chef-time-settings/me
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping("/me")
    public ResponseEntity<ChefTimeSettingsResponse> updateTimeSettingsForCurrentChef(
            @Valid @RequestBody ChefTimeSettingsRequest request) {
        ChefTimeSettingsResponse response = timeSettingsService.updateTimeSettingsForCurrentChef(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Khôi phục cài đặt thời gian mặc định
     * Endpoint: POST /api/v1/chef-time-settings/reset
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PostMapping("/reset")
    public ResponseEntity<ChefTimeSettingsResponse> resetTimeSettingsToDefault() {
        ChefTimeSettingsResponse response = timeSettingsService.resetTimeSettingsToDefault();
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy cài đặt thời gian theo ID
     * Endpoint: GET /api/v1/chef-time-settings/{settingId}
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/{settingId}")
    public ResponseEntity<ChefTimeSettingsResponse> getTimeSettingsById(@PathVariable Long settingId) {
        ChefTimeSettingsResponse response = timeSettingsService.getTimeSettingsById(settingId);
        return ResponseEntity.ok(response);
    }
} 