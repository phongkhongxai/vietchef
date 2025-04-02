package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefBlockedDateResponse;
import com.spring2025.vietchefs.services.ChefBlockedDateService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chef-blocked-dates")
@RequiredArgsConstructor
public class ChefBlockedDateController {

    private final ChefBlockedDateService blockedDateService;

    /**
     * Tạo mới ngày bị chặn cho chef hiện tại
     * Endpoint: POST /api/v1/chef-blocked-dates
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PostMapping
    public ResponseEntity<ChefBlockedDateResponse> createBlockedDate(
            @Valid @RequestBody ChefBlockedDateRequest request) {
        ChefBlockedDateResponse response = blockedDateService.createBlockedDateForCurrentChef(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Lấy thông tin ngày bị chặn theo ID
     * Endpoint: GET /api/v1/chef-blocked-dates/{blockId}
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/{blockId}")
    public ResponseEntity<ChefBlockedDateResponse> getBlockedDateById(@PathVariable Long blockId) {
        ChefBlockedDateResponse response = blockedDateService.getBlockedDateById(blockId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tất cả ngày bị chặn của chef hiện tại
     * Endpoint: GET /api/v1/chef-blocked-dates/me
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/me")
    public ResponseEntity<List<ChefBlockedDateResponse>> getAllBlockedDatesForCurrentChef() {
        List<ChefBlockedDateResponse> responses = blockedDateService.getBlockedDatesForCurrentChef();
        return ResponseEntity.ok(responses);
    }

    /**
     * Lấy ngày bị chặn của chef hiện tại trong khoảng ngày
     * Endpoint: GET /api/v1/chef-blocked-dates/me/range
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/me/range")
    public ResponseEntity<List<ChefBlockedDateResponse>> getBlockedDatesBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ChefBlockedDateResponse> responses = blockedDateService.getBlockedDatesForCurrentChefBetween(startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * Lấy ngày bị chặn của chef hiện tại vào một ngày cụ thể
     * Endpoint: GET /api/v1/chef-blocked-dates/me/date
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/me/date")
    public ResponseEntity<List<ChefBlockedDateResponse>> getBlockedDatesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ChefBlockedDateResponse> responses = blockedDateService.getBlockedDatesForCurrentChefByDate(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * Cập nhật thông tin ngày bị chặn
     * Endpoint: PUT /api/v1/chef-blocked-dates
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping
    public ResponseEntity<ChefBlockedDateResponse> updateBlockedDate(
            @Valid @RequestBody ChefBlockedDateUpdateRequest request) {
        ChefBlockedDateResponse response = blockedDateService.updateBlockedDate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa mềm ngày bị chặn
     * Endpoint: DELETE /api/v1/chef-blocked-dates/{blockId}
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @DeleteMapping("/{blockId}")
    public ResponseEntity<String> deleteBlockedDate(@PathVariable Long blockId) {
        blockedDateService.deleteBlockedDate(blockId);
        return ResponseEntity.ok("Blocked date deleted successfully");
    }
} 