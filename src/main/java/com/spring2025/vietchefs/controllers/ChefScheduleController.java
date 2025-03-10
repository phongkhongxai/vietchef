package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefScheduleResponse;
import com.spring2025.vietchefs.services.ChefScheduleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chef-schedules")
@RequiredArgsConstructor
public class ChefScheduleController {

    private final ChefScheduleService chefScheduleService;

    /**
     * Tạo mới lịch cho chef hiện tại.
     * Endpoint: POST /api/v1/chef-schedules
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PostMapping
    public ResponseEntity<ChefScheduleResponse> createSchedule(
            @Valid @RequestBody ChefScheduleRequest request) {
        ChefScheduleResponse response = chefScheduleService.createScheduleForCurrentChef(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Lấy thông tin lịch theo scheduleId.
     * Endpoint: GET /api/v1/chef-schedules/{scheduleId}
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ChefScheduleResponse> getScheduleById(@PathVariable Long scheduleId) {
        ChefScheduleResponse response = chefScheduleService.getScheduleById(scheduleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách lịch của chef hiện tại.
     * Endpoint: GET /api/v1/chef-schedules/me
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/me")
    public ResponseEntity<List<ChefScheduleResponse>> getSchedulesForCurrentChef() {
        List<ChefScheduleResponse> responses = chefScheduleService.getSchedulesForCurrentChef();
        return ResponseEntity.ok(responses);
    }

    /**
     * Cập nhật lịch của chef.
     * Endpoint: PUT /api/v1/chef-schedules
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping
    public ResponseEntity<ChefScheduleResponse> updateSchedule(
            @Valid @RequestBody ChefScheduleUpdateRequest request) {
        ChefScheduleResponse response = chefScheduleService.updateSchedule(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa mềm lịch của chef theo scheduleId.
     * Endpoint: DELETE /api/v1/chef-schedules/{scheduleId}
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<String> deleteSchedule(@PathVariable Long scheduleId) {
        chefScheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok("Chef schedule deleted successfully");
    }
}