package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.dto.ReportDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReportHandleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReportRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReportsResponse;

public interface ReportService {
    ReportDto createReportWithChefNoShow(Long reporterId, ReportRequest request);
    ReportDto createReportWithOtherReason(Long reporterId, ReportRequest request);
    ReportsResponse getAllReports(int pageNo, int pageSize, String sortBy, String sortDir);
    ReportsResponse getAllMyReports(Long userId,int pageNo, int pageSize, String sortBy, String sortDir);
    ReportsResponse getAllReportsPending(int pageNo, int pageSize, String sortBy, String sortDir);
    ReportDto getReportById(Long id);
    ReportDto updateReportStatus(Long id, ReportHandleRequest request);
    String deleteReport(Long id);

}
