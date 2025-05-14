package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.repositories.BookingDetailRepository;
import com.spring2025.vietchefs.repositories.BookingRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingConflictServiceImpl implements BookingConflictService {

    private static final List<String> NOT_ACTIVE_BOOKING_STATUSES = List.of("OVERDUE", "CANCELED", "REJECTED");
    private static final List<String> NOT_ACTIVE_DETAIL_STATUSES = List.of("OVERDUE", "CANCELED");
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    
    @Autowired
    private ChefRepository chefRepository;

    @Override
    public boolean hasBookingConflict(Chef chef, LocalDate date, LocalTime startTime, LocalTime endTime) {
        // Lấy tất cả booking detail của chef vào ngày cụ thể
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date);
        
        // Lọc ra những booking còn active
        bookingDetails = filterActiveBookingDetails(bookingDetails);
        
        // Kiểm tra xem có booking detail nào trùng giờ không
        return bookingDetails.stream()
                .anyMatch(detail -> 
                    timeRangesOverlap(startTime, endTime, detail.getTimeBeginTravel(), detail.getStartTime()));
    }

    @Override
    public boolean hasBookingConflictOnDayOfWeek(Chef chef, Integer dayOfWeek, LocalTime startTime, LocalTime endTime, Integer daysToCheck) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysToCheck);
        
        // Lọc ra tất cả các ngày trong khoảng thời gian có cùng thứ trong tuần
        List<LocalDate> datesToCheck = today.datesUntil(endDate.plusDays(1))
                .filter(date -> {
                    // Điều chỉnh dayOfWeek để phù hợp với DayOfWeek.getValue() (1-7 với 1 là Thứ 2)
                    int adjustedDayOfWeek = (dayOfWeek % 7) + 1;
                    if (adjustedDayOfWeek == 8) adjustedDayOfWeek = 1; // Chủ nhật
                    
                    return date.getDayOfWeek().getValue() == adjustedDayOfWeek;
                })
                .collect(Collectors.toList());
        
        // Kiểm tra xem có booking detail nào trùng giờ vào các ngày này không
        for (LocalDate date : datesToCheck) {
            List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date);
            List<BookingDetail> activeDetails = filterActiveBookingDetails(bookingDetails);
            
            for (BookingDetail detail : activeDetails) {
                if (timeRangesOverlap(startTime, endTime, detail.getTimeBeginTravel(), detail.getStartTime())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean hasActiveBookingsForDayOfWeek(Long chefId, Integer dayOfWeek) {
        // Lấy thông tin Chef từ ID
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found with id: " + chefId));
                
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(60); // Kiểm tra trong 60 ngày tới
        
        // Lọc ra tất cả các ngày trong khoảng thời gian có cùng thứ trong tuần
        List<LocalDate> datesToCheck = today.datesUntil(endDate.plusDays(1))
                .filter(date -> {
                    // Điều chỉnh dayOfWeek để phù hợp với DayOfWeek.getValue() (1-7 với 1 là Thứ 2)
                    int adjustedDayOfWeek = (dayOfWeek % 7) + 1;
                    if (adjustedDayOfWeek == 8) adjustedDayOfWeek = 1; // Chủ nhật
                    
                    return date.getDayOfWeek().getValue() == adjustedDayOfWeek;
                })
                .collect(Collectors.toList());
        
        // Kiểm tra xem có booking detail nào active vào các ngày này không
        for (LocalDate date : datesToCheck) {
            List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date);
            List<BookingDetail> activeDetails = filterActiveBookingDetails(bookingDetails);
            
            if (!activeDetails.isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Lọc danh sách booking detail để lấy ra các booking còn active
     */
    private List<BookingDetail> filterActiveBookingDetails(List<BookingDetail> bookingDetails) {
        return bookingDetails.stream()
                .filter(detail -> {
                    // Booking vẫn còn active (không nằm trong danh sách NOT_ACTIVE)
                    Booking booking = detail.getBooking();
                    if (booking.getIsDeleted() || NOT_ACTIVE_BOOKING_STATUSES.contains(booking.getStatus())) {
                        return false;
                    }
                    
                    // BookingDetail vẫn còn active (không nằm trong danh sách NOT_ACTIVE)
                    return !detail.getIsDeleted() && !NOT_ACTIVE_DETAIL_STATUSES.contains(detail.getStatus());
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Kiểm tra xem hai khoảng thời gian có chồng lấn không
     */
    private boolean timeRangesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
} 