package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ReportDto;
import com.spring2025.vietchefs.models.payload.requestModel.NotificationRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReportHandleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ReportRequest;
import com.spring2025.vietchefs.models.payload.responseModel.DishResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ReportsResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.ReportService;
import org.checkerframework.checker.units.qual.A;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private ChefService chefService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private BookingDetailService bookingDetailService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ReportDto createReportWithChefNoShow(Long reporterId, ReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));

        Chef reportedChef = chefRepository.findById(request.getReportedChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "ReportedUser not found"));
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
    public ReportDto createReportWithOtherReason(Long reporterId, ReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));

        Chef reportedChef = chefRepository.findById(request.getReportedChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "ReportedUser not found"));
        Review review = reviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Review not found"));
        if(!Objects.equals(reporter.getId(), review.getBooking().getCustomer().getId())){
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "You not in booking.");
        }
        if(!Objects.equals(reportedChef.getId(), review.getBooking().getChef().getId())){
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef not in booking.");
        }
        Report report = new Report();
        report.setReportedBy(reporter);
        report.setReportedChef(reportedChef);
        report.setReasonDetail(request.getReasonDetail());
        report.setReason(request.getReason());
        report.setStatus("PENDING");
        report.setIsDeleted(false);
        report.setReview(review);
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
    public ReportsResponse getAllMyReports(Long userId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Report> reportsPage = reportRepository.findByReportedByIdAndIsDeletedFalse(reporter.getId(), pageable);

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
    public ReportsResponse getAllReportsPending(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Report> reportsPage = reportRepository.findByStatusAndIsDeletedFalse("PENDING",pageable);

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
    @Transactional
    public ReportDto updateReportStatus(Long reportId, ReportHandleRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Report not found with id: " + reportId));

        String normalizedStatus = request.getStatus().toUpperCase();
        if (!normalizedStatus.equals("HANDLED") && !normalizedStatus.equals("REJECTED")) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus() + ". Only 'HANDLED' or 'REJECTED' are allowed.");
        }

        BookingDetail bookingDetail = report.getBookingDetail();
        if (bookingDetail == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Report does not have an associated booking detail.");
        }

        if (normalizedStatus.equals("HANDLED")) {
            Chef chef = report.getReportedChef();
            if (chef == null) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Report does not have a reported chef.");
            }
            int deduction = request.getDeduction() != null ? request.getDeduction() : 1;
            chefService.updateReputation(chef, deduction);
            if (request.isLockChef()) {
                chef.setStatus("LOCKED");
            }
            chefRepository.save(chef);
            if(request.isRefundBooking()){
                bookingDetailService.refundBookingDetail(bookingDetail.getId());
            }
            NotificationRequest chefNotification = NotificationRequest.builder()
                    .userId(chef.getUser().getId())
                    .title("Report Handled")
                    .body("You have been penalized based on a customer report. Please check your account status.")
                    .bookingDetailId(bookingDetail.getId())
                    .screen("BookingDetail")
                    .build();
            notificationService.sendPushNotification(chefNotification);

            NotificationRequest customerNotification = NotificationRequest.builder()
                    .userId(report.getReportedBy().getId())
                    .title("Report Resolved - Refund Issued")
                    .body("Your report has been resolved. The chef was penalized and a refund has been processed for the affected session.")
                    .bookingDetailId(bookingDetail.getId())
                    .screen("BookingDetail")
                    .build();
            notificationService.sendPushNotification(customerNotification);
        } else if (normalizedStatus.equals("REJECTED")) {
            if (bookingDetail.getStatus().equalsIgnoreCase("LOCKED")) {
                bookingDetail.setStatus("WAITING_FOR_CONFIRMATION");
                bookingDetailRepository.save(bookingDetail);
            }
            NotificationRequest customerNotification = NotificationRequest.builder()
                    .userId(report.getReportedBy().getId())
                    .title("Report Rejected")
                    .body("Your report about the chef has been rejected. The booking has been marked as completed.")
                    .bookingDetailId(bookingDetail.getId())
                    .screen("BookingDetail")
                    .build();
            notificationService.sendPushNotification(customerNotification);
        }
        report.setStatus(normalizedStatus);
        report = reportRepository.save(report);
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
