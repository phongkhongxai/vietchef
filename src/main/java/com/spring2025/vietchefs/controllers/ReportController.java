package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.Report;
import com.spring2025.vietchefs.models.payload.dto.ReportDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReportHandleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReportRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReportsResponse;
import com.spring2025.vietchefs.services.ReportService;
import com.spring2025.vietchefs.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    @Autowired
    private ReportService reportService;
    @Autowired
    private UserService userService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping
    @Operation(
            summary = "Tạo report với reason là CHEF_NO_SHOW và phải có bookingDetailId, ko cần reviewId "
    )
    public ResponseEntity<ReportDto> createChefNoShowReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReportRequest request
    ) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        ReportDto createdReport = reportService.createReportWithChefNoShow(bto.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReport);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/others")
    @Operation(
            summary = "Tạo report với reason là khác và đi kèm ReviewId, ko cần bookingDetailId "
    )
    public ResponseEntity<ReportDto> createReportWithReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReportRequest request
    ) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        ReportDto createdReport = reportService.createReportWithOtherReason(bto.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReport);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<ReportsResponse> getAllReports(
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(reportService.getAllReports(pageNo, pageSize, sortBy, sortDir));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/pending")
    public ResponseEntity<ReportsResponse> getAllPendingReports(
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(reportService.getAllReportsPending(pageNo, pageSize, sortBy, sortDir));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    public ResponseEntity<ReportDto> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ReportDto> updateReportStatus(@PathVariable Long id, @RequestBody ReportHandleRequest request) {
        return ResponseEntity.ok(reportService.updateReportStatus(id, request));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.deleteReport(id));
    }
}
