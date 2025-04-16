package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ReportDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReportRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ReportsResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReportService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService{
    @Autowired
    private ReportRepository reportRepository;
    @Autowired

    private UserRepository userRepository;
    @Autowired

    private ReviewRepository reviewRepository;
    @Autowired

    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ReportDto createReportWithChefNoShow(Long reporterId, ReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));

        Chef reportedChef = chefRepository.findById(request.getReportedChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Người bị báo cáo không tồn tại"));
        BookingDetail bookingDetail = bookingDetailRepository.findById(request.getBookingDetailId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking detail not found"));
        if(!Objects.equals(reporter.getId(), bookingDetail.getBooking().getCustomer().getId())){
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "You not in booking.");
        }
        if(!Objects.equals(reportedChef.getId(), bookingDetail.getBooking().getChef().getId())){
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef not in booking.");
        }
        Report report = new Report();
        report.setReportedBy(reporter);
        report.setReportedChef(reportedChef);
        report.setReasonDetail(request.getReasonDetail());
        report.setReason(request.getReason());
        report.setStatus("PENDING");
        report.setIsDeleted(false);
        report.setBookingDetail(bookingDetail);
        if (bookingDetail.getStatus().equalsIgnoreCase("WAITING_FOR_CONFIRMATION") && request.getReason().equalsIgnoreCase("CHEF_NO_SHOW")){
            bookingDetail.setStatus("LOCKED");
            bookingDetailRepository.save(bookingDetail);
        }else{
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Current BookingDetail's status not suit for this type report. Complete session to report then.");
        }
        report = reportRepository.save(report);
        return modelMapper.map(report, ReportDto.class);

    }

    @Override
    public ReportsResponse getAllReports(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Report> reportsPage = reportRepository.findAllNotDeleted(pageable);

        List<Report> listOfDishes = reportsPage.getContent();

        List<ReportDto> content = listOfDishes.stream().map(bt -> modelMapper.map(bt, ReportDto.class)).collect(Collectors.toList());

        ReportsResponse response = new ReportsResponse();
        response.setContent(content);
        response.setPageNo(reportsPage.getNumber());
        response.setPageSize(reportsPage.getSize());
        response.setTotalElements(reportsPage.getTotalElements());
        response.setTotalPages(reportsPage.getTotalPages());
        response.setLast(reportsPage.isLast());

        return response;
    }

    @Override
    public ReportDto getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Report not found with id: "+id));

        return modelMapper.map(report, ReportDto.class);
    }

    @Override
    public ReportDto updateReportStatus(Long id, String status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Report not found with id: "+id));

        report.setStatus(status.toUpperCase());
        reportRepository.save(report);

        return modelMapper.map(report, ReportDto.class);
    }

    @Override
    public String deleteReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo"));

        report.setIsDeleted(true);
        reportRepository.save(report);

        return "Deleted report successfully.";
    }


}
