package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Report;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ReportDto;
import com.spring2025.vietchefs.models.payload.requestModel.ReportRequest;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReportService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
        Report report = new Report();
        report.setReportedBy(reporter);
        report.setReportedChef(reportedChef);
        report.setReasonDetail(request.getReasonDetail());
        report.setReason(request.getReason());
        report.setStatus("PENDING");
        report.setIsDeleted(false);
        if (request.getReason().equalsIgnoreCase("CHEF_NO_SHOW")) {
            bookingDetail.setStatus("LOCKED");
            bookingDetailRepository.save(bookingDetail);
        }
//         else {
////            if (request.getReviewId() == null) {
////                throw new VchefApiException(HttpStatus.NOT_FOUND,"Cần cung cấp review cho loại báo cáo này.");
////            }
////            Review review = reviewRepository.findById(request.getReviewId())
////                    .orElseThrow(() -> new NotFoundException("Review không tồn tại"));
////            report.setReview(review);
//        }
        report = reportRepository.save(report);

        return modelMapper.map(report, ReportDto.class);

    }
}
