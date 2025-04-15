package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.Report;
import com.spring2025.vietchefs.models.payload.dto.ReportDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReportRequest;
import com.spring2025.vietchefs.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    @Autowired
    private ReportService reportService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping
    @Operation(
            summary = "Tạo report với reason là CHEF_NO_SHOW "
    )
    public ResponseEntity<ReportDto> createChefNoShowReport(
            @RequestParam Long reporterId,
            @RequestBody ReportRequest request
    ) {
        ReportDto createdReport = reportService.createReportWithChefNoShow(reporterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReport);
    }
}
