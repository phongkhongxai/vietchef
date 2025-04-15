package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.ReportDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReportRequest;

public interface ReportService {
    ReportDto createReportWithChefNoShow(Long reporterId, ReportRequest request);
}
