package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefBlockedDate;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.ChefTimeSettings;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.AvailableTimeSlotRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.AvailabilityFinderService;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.impl.CalculateService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AvailabilityFinderServiceImpl implements AvailabilityFinderService {

    // Các hằng số cho việc tìm kiếm khung giờ
    private static final LocalTime DEFAULT_MIN_WORK_HOUR = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_MAX_WORK_HOUR = LocalTime.of(22, 0);
    private static final int DEFAULT_MIN_SLOT_DURATION_MINUTES = 120; // 2 giờ
    private static final int DEFAULT_PREP_TIME_MINUTES = 0; // 30 phút
    private static final int DEFAULT_CLEANUP_TIME_MINUTES = 0; // 30 phút
    private static final int MAX_DAYS_TO_SEARCH = 60; // Giới hạn tìm kiếm 60 ngày
    
    @Autowired
    private ChefRepository chefRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChefScheduleRepository scheduleRepository;
    
    @Autowired
    private ChefBlockedDateRepository blockedDateRepository;
    
    @Autowired
    private ChefTimeSettingsRepository timeSettingsRepository;
    
    @Autowired
    private BookingConflictService bookingConflictService;
    
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private PackageRepository packageRepository;
    
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    
    @Autowired
    private DistanceService distanceService;
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsForChef(
            Long chefId, LocalDate startDate, LocalDate endDate) {
        
        // Kiểm tra tham số đầu vào
        validateInputParameters(startDate, endDate);
        
        // Lấy thông tin chef
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, 
                    "Chef not found with id: " + chefId));
        
        return findAvailableTimeSlots(chef, startDate, endDate, null);
    }
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsForCurrentChef(
            LocalDate startDate, LocalDate endDate) {
        
        // Kiểm tra tham số đầu vào
        validateInputParameters(startDate, endDate);
        
        // Lấy thông tin chef hiện tại
        Chef chef = getCurrentChef();
        
        return findAvailableTimeSlots(chef, startDate, endDate, null);
    }
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsForChefByDate(
            Long chefId, LocalDate date) {
        
        // Kiểm tra tham số đầu vào
        if (date == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Date cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot search for dates in the past");
        }
        
        // Lấy thông tin chef
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, 
                    "Chef not found with id: " + chefId));
        
        return findAvailableTimeSlots(chef, date, date, null);
    }
    
    @Override
    public boolean isTimeSlotAvailable(Long chefId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        // Kiểm tra tham số đầu vào
        if (date == null || startTime == null || endTime == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Date, start time, and end time cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot check availability for dates in the past");
        }
        if (!startTime.isBefore(endTime)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }
        
        // Lấy thông tin chef
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, 
                    "Chef not found with id: " + chefId));
        
        // Kiểm tra xem khung giờ có thuộc lịch làm việc của chef
        if (!isWithinChefSchedule(chef, date, startTime, endTime)) {
            return false;
        }
        
        // Kiểm tra xem khung giờ có xung đột với các ngày bị chặn
        if (hasBlockedDateConflict(chef, date, startTime, endTime)) {
            return false;
        }
        
        // Kiểm tra xem khung giờ có xung đột với các đơn đặt hàng hiện có
        return !bookingConflictService.hasBookingConflict(chef, date, startTime, endTime);
    }
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithCookingTime(
            Long chefId, LocalDate date, Long menuId, List<Long> dishIds, int guestCount, int maxDishesPerMeal) {
        
        // Kiểm tra tham số đầu vào
        if (date == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Date cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot search for dates in the past");
        }
        
        // Lấy thông tin chef
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, 
                    "Chef not found with id: " + chefId));
        
        // Tính toán thời gian nấu (giờ)
        BigDecimal cookTimeHours = BigDecimal.ZERO;
        if (menuId != null) {
            cookTimeHours = calculateService.calculateTotalCookTimeFromMenu(menuId, dishIds, guestCount);
        } else {
            cookTimeHours = calculateService.calculateTotalCookTime(dishIds, guestCount);
        }
        // Lấy danh sách khung giờ trống
        List<AvailableTimeSlotResponse> availableSlots = findAvailableTimeSlots(chef, date, date, null);
        
        // Điều chỉnh các khung giờ dựa trên thời gian nấu
        return adjustTimeSlotsByCookingTime(availableSlots, cookTimeHours);
    }
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithCookingTimeForCurrentChef(
            LocalDate date, Long menuId, List<Long> dishIds, int guestCount) {
        
        // Kiểm tra tham số đầu vào
        if (date == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Date cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot search for dates in the past");
        }
        
        // Lấy thông tin chef hiện tại
        Chef chef = getCurrentChef();
        
        // Tính toán thời gian nấu (giờ)
        BigDecimal cookTimeHours = calculateService.calculateTotalCookTimeFromMenu(menuId, dishIds, guestCount);
        
        // Lấy danh sách khung giờ trống
        List<AvailableTimeSlotResponse> availableSlots = findAvailableTimeSlots(chef, date, date, null);
        
        // Điều chỉnh các khung giờ dựa trên thời gian nấu
        return adjustTimeSlotsByCookingTime(availableSlots, cookTimeHours);
    }
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithLocationConstraints(
            Long chefId, LocalDate date, String customerLocation, 
            Long menuId, List<Long> dishIds, int guestCount, 
            int maxDishesPerMeal) {
        
        // Kiểm tra tham số đầu vào
        if (date == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Date cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot search for dates in the past");
        }
        if (customerLocation == null || customerLocation.trim().isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Customer location cannot be empty");
        }
        
        // Lấy thông tin chef
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, 
                    "Chef not found with id: " + chefId));
        
        String chefAddress = chef.getAddress();
        if (chefAddress == null || chefAddress.trim().isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef address not found");
        }
        
        // Tính thời gian di chuyển từ vị trí của chef đến vị trí khách hàng
        DistanceResponse distanceResponse = distanceService.calculateDistanceAndTime(chefAddress, customerLocation);
        BigDecimal travelTimeHours = distanceResponse.getDurationHours();
        
        // Nếu không thể tính toán thời gian di chuyển
        if (travelTimeHours.compareTo(BigDecimal.ZERO) == 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Could not calculate travel time between locations");
        }
        
        // Tính thời gian nấu ăn (giờ)
        BigDecimal cookTimeHours;
        if (menuId != null) {
            cookTimeHours = calculateService.calculateTotalCookTimeFromMenu(menuId, dishIds, guestCount);
        } else if (dishIds != null && !dishIds.isEmpty()) {
            cookTimeHours = calculateService.calculateTotalCookTime(dishIds, guestCount);
        } else {
            cookTimeHours = calculateService.calculateMaxCookTime(chefId, maxDishesPerMeal, guestCount);
        }
        
        // Sử dụng minDuration null để lấy tất cả các khung giờ trống cơ bản
        List<AvailableTimeSlotResponse> availableSlots = findAvailableTimeSlots(chef, date, date, null);
        
        // Lấy danh sách các booking hiện có của chef vào ngày đó
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date);
        List<BookingDetail> activeBookings = bookingDetails.stream()
                .filter(detail -> {
                    Booking booking = detail.getBooking();
                    return !booking.getIsDeleted() && 
                           !List.of("CANCELED", "OVERDUE").contains(booking.getStatus()) &&
                           !detail.getIsDeleted() && 
                           !List.of("CANCELED", "OVERDUE").contains(detail.getStatus());
                })
                .sorted(Comparator.comparing(BookingDetail::getStartTime))
                .collect(Collectors.toList());
        
        // Kết quả cuối cùng
        List<AvailableTimeSlotResponse> adjustedSlots = new ArrayList<>();
        
        // Kiểm tra từng khung giờ trống với các booking hiện có
        for (AvailableTimeSlotResponse slot : availableSlots) {
            boolean isSlotValid = true;
            LocalTime earliestStartTime = null;
            
            for (BookingDetail booking : activeBookings) {
                // Thời gian kết thúc của booking hiện tại
                LocalTime existingBookingEndTime = booking.getStartTime(); // startTime là thời gian khách bắt đầu ăn
                
                // Kiểm tra nếu khung giờ trống hiện tại nằm sau booking hiện có
                if (slot.getStartTime().isAfter(existingBookingEndTime) || 
                    slot.getStartTime().equals(existingBookingEndTime)) {
                    
                    // Tính thời gian khả dụng sớm nhất sau booking hiện có
                    // = Thời gian kết thúc của booking + 30 phút nghỉ + thời gian di chuyển + thời gian nấu
                    LocalTime availableAfterExisting = existingBookingEndTime
                            .plusMinutes(30) // Thêm 30 phút nghỉ bắt buộc
                            .plusSeconds((int)(travelTimeHours.doubleValue() * 3600)) // Thêm thời gian di chuyển
                            .plusSeconds((int)(cookTimeHours.doubleValue() * 3600)); // Thêm thời gian nấu
                    
                    // Cập nhật thời gian sớm nhất có thể bắt đầu
                    if (earliestStartTime == null || availableAfterExisting.isAfter(earliestStartTime)) {
                        earliestStartTime = availableAfterExisting;
                    }
                }
                
                // Kiểm tra nếu khung giờ trống hiện tại trùng với booking hiện có
                if (slot.getStartTime().isBefore(existingBookingEndTime) && 
                    slot.getEndTime().isAfter(existingBookingEndTime.minusMinutes(30))) {
                    isSlotValid = false;
                    break;
                }
            }
            
            // Nếu khung giờ hợp lệ và có thời gian sớm nhất được tính toán
            if (isSlotValid && earliestStartTime != null) {
                // Nếu thời gian sớm nhất nằm trong khung giờ trống
                if (earliestStartTime.isBefore(slot.getEndTime())) {
                    
                    AvailableTimeSlotResponse adjustedSlot = new AvailableTimeSlotResponse();
                    adjustedSlot.setChefId(slot.getChefId());
                    adjustedSlot.setChefName(slot.getChefName());
                    adjustedSlot.setDate(slot.getDate());
                    adjustedSlot.setStartTime(earliestStartTime);
                    adjustedSlot.setEndTime(slot.getEndTime());
                    adjustedSlot.setDurationMinutes((int) Duration.between(earliestStartTime, slot.getEndTime()).toMinutes());
                    adjustedSlot.setNote("Adjusted for 30min break, " + 
                                       travelTimeHours.setScale(2).toString() + " hours travel time and " + 
                                       cookTimeHours.setScale(2).toString() + " hours cooking time");
                    
                    adjustedSlots.add(adjustedSlot);
                }
            }
            // Nếu không có booking trước đó nhưng khung giờ vẫn hợp lệ
            else if (isSlotValid && earliestStartTime == null) {
                // Cần cộng thêm thời gian nấu vào thời gian bắt đầu
                LocalTime adjustedStartTime = slot.getStartTime()
                        .plusSeconds((int)(cookTimeHours.doubleValue() * 3600)); // Thêm thời gian nấu
                
                if (adjustedStartTime.isBefore(slot.getEndTime())) {
                    
                    AvailableTimeSlotResponse adjustedSlot = new AvailableTimeSlotResponse();
                    adjustedSlot.setChefId(slot.getChefId());
                    adjustedSlot.setChefName(slot.getChefName());
                    adjustedSlot.setDate(slot.getDate());
                    adjustedSlot.setStartTime(adjustedStartTime);
                    adjustedSlot.setEndTime(slot.getEndTime());
                    adjustedSlot.setDurationMinutes((int) Duration.between(adjustedStartTime, slot.getEndTime()).toMinutes());
                    adjustedSlot.setNote("Adjusted for " + cookTimeHours.setScale(2).toString() + " hours cooking time");
                    
                    adjustedSlots.add(adjustedSlot);
                }
            }
        }
        
        // Sắp xếp kết quả theo thời gian bắt đầu
        Collections.sort(adjustedSlots, Comparator.comparing(AvailableTimeSlotResponse::getStartTime));
        
        return adjustedSlots;
    }

    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithLocationConstraints(Long chefId, String customerLocation, int guestCount, int maxDishesPerMeal, List<AvailableTimeSlotRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Requests list cannot be empty");
        }

        if (customerLocation == null || customerLocation.trim().isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Customer location cannot be empty");
        }

        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,
                        "Chef not found with id: " + chefId));

        String chefAddress = chef.getAddress();
        if (chefAddress == null || chefAddress.trim().isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef address not found");
        }

        DistanceResponse distanceResponse = distanceService.calculateDistanceAndTime(chefAddress, customerLocation);
        BigDecimal travelTimeHours = distanceResponse.getDurationHours();

        if (travelTimeHours.compareTo(BigDecimal.ZERO) == 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Could not calculate travel time between locations");
        }

        List<AvailableTimeSlotResponse> allAdjustedSlots = new ArrayList<>();

        for (AvailableTimeSlotRequest request : requests) {
            LocalDate date = request.getSessionDate();
            if (date == null || date.isBefore(LocalDate.now())) {
                continue; // bỏ qua nếu ngày không hợp lệ
            }

            List<Long> dishIds = request.getDishIds();
            Long menuId = request.getMenuId();

            BigDecimal cookTimeHours;
            if (menuId != null) {
                cookTimeHours = calculateService.calculateTotalCookTimeFromMenu(menuId, dishIds, guestCount);
            } else if (dishIds != null && !dishIds.isEmpty()) {
                cookTimeHours = calculateService.calculateTotalCookTime(dishIds, guestCount);
            } else {
                cookTimeHours = calculateService.calculateMaxCookTime(chefId, maxDishesPerMeal, guestCount);
            }

            List<AvailableTimeSlotResponse> availableSlots = findAvailableTimeSlots(chef, date, date, null);

            List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date);
            List<BookingDetail> activeBookings = bookingDetails.stream()
                    .filter(detail -> {
                        Booking booking = detail.getBooking();
                        return !booking.getIsDeleted() &&
                                !List.of("CANCELED", "OVERDUE").contains(booking.getStatus()) &&
                                !detail.getIsDeleted() &&
                                !List.of("CANCELED", "OVERDUE").contains(detail.getStatus());
                    })
                    .sorted(Comparator.comparing(BookingDetail::getStartTime))
                    .collect(Collectors.toList());

            for (AvailableTimeSlotResponse slot : availableSlots) {
                boolean isSlotValid = true;
                LocalTime earliestStartTime = null;

                for (BookingDetail booking : activeBookings) {
                    LocalTime existingBookingEndTime = booking.getStartTime();

                    if (slot.getStartTime().isAfter(existingBookingEndTime) ||
                            slot.getStartTime().equals(existingBookingEndTime)) {

                        LocalTime availableAfterExisting = existingBookingEndTime
                                .plusMinutes(30)
                                .plusSeconds((int) (travelTimeHours.doubleValue() * 3600))
                                .plusSeconds((int) (cookTimeHours.doubleValue() * 3600));

                        if (earliestStartTime == null || availableAfterExisting.isAfter(earliestStartTime)) {
                            earliestStartTime = availableAfterExisting;
                        }
                    }

                    if (slot.getStartTime().isBefore(existingBookingEndTime) &&
                            slot.getEndTime().isAfter(existingBookingEndTime.minusMinutes(30))) {
                        isSlotValid = false;
                        break;
                    }
                }

                if (isSlotValid && earliestStartTime != null) {
                    if (earliestStartTime.isBefore(slot.getEndTime())) {
                        AvailableTimeSlotResponse adjustedSlot = new AvailableTimeSlotResponse();
                        adjustedSlot.setChefId(slot.getChefId());
                        adjustedSlot.setChefName(slot.getChefName());
                        adjustedSlot.setDate(slot.getDate());
                        adjustedSlot.setStartTime(earliestStartTime);
                        adjustedSlot.setEndTime(slot.getEndTime());
                        adjustedSlot.setDurationMinutes((int) Duration.between(earliestStartTime, slot.getEndTime()).toMinutes());
                        adjustedSlot.setNote("Adjusted for 30min break, " +
                                travelTimeHours.setScale(2) + " hours travel time and " +
                                cookTimeHours.setScale(2) + " hours cooking time");

                        allAdjustedSlots.add(adjustedSlot);
                    }
                } else if (isSlotValid && earliestStartTime == null) {
                    LocalTime adjustedStartTime = slot.getStartTime()
                            .plusSeconds((int) (cookTimeHours.doubleValue() * 3600));

                    if (adjustedStartTime.isBefore(slot.getEndTime())) {
                        AvailableTimeSlotResponse adjustedSlot = new AvailableTimeSlotResponse();
                        adjustedSlot.setChefId(slot.getChefId());
                        adjustedSlot.setChefName(slot.getChefName());
                        adjustedSlot.setDate(slot.getDate());
                        adjustedSlot.setStartTime(adjustedStartTime);
                        adjustedSlot.setEndTime(slot.getEndTime());
                        adjustedSlot.setDurationMinutes((int) Duration.between(adjustedStartTime, slot.getEndTime()).toMinutes());
                        adjustedSlot.setNote("Adjusted for " + cookTimeHours.setScale(2) + " hours cooking time");

                        allAdjustedSlots.add(adjustedSlot);
                    }
                }
            }
        }

        Collections.sort(allAdjustedSlots, Comparator.comparing(AvailableTimeSlotResponse::getStartTime));
        return allAdjustedSlots;

    }

    /**
     * Tìm tất cả các khung giờ trống cho một chef trong khoảng ngày
     */
    private List<AvailableTimeSlotResponse> findAvailableTimeSlots(
            Chef chef, LocalDate startDate, LocalDate endDate, Integer minDuration) {
        
        // Lấy cài đặt thời gian của chef
        ChefTimeSettings timeSettings = getChefTimeSettings(chef);
        
        // Áp dụng cài đặt thời gian
        int prepTimeMinutes = timeSettings.getStandardPrepTime() != null ? 
                timeSettings.getStandardPrepTime() : DEFAULT_PREP_TIME_MINUTES;
        int cleanupTimeMinutes = timeSettings.getStandardCleanupTime() != null ? 
                timeSettings.getStandardCleanupTime() : DEFAULT_CLEANUP_TIME_MINUTES;
        
        List<AvailableTimeSlotResponse> availableSlots = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        // Duyệt qua từng ngày trong khoảng
        while (!currentDate.isAfter(endDate)) {
            // Lấy lịch làm việc của chef vào ngày hiện tại
            int dayOfWeek = currentDate.getDayOfWeek().getValue() % 7;
            List<ChefSchedule> schedules = scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek);
            
            // Nếu không có lịch làm việc vào ngày này, bỏ qua
            if (schedules.isEmpty()) {
                currentDate = currentDate.plusDays(1);
                continue;
            }
            
            // Lấy danh sách các ngày bị chặn vào ngày hiện tại
            List<ChefBlockedDate> blockedDates = blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(
                    chef, currentDate);
            
            // Xử lý từng khung giờ làm việc
            for (ChefSchedule schedule : schedules) {
                // Tìm các khung giờ trống trong lịch làm việc
                List<AvailableTimeSlotResponse> slotsInSchedule = findAvailableSlotsInSchedule(
                        chef, currentDate, schedule, blockedDates, prepTimeMinutes, cleanupTimeMinutes);
                
                availableSlots.addAll(slotsInSchedule);
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        // Sắp xếp kết quả theo ngày và thời gian bắt đầu
        Collections.sort(availableSlots, Comparator
                .comparing(AvailableTimeSlotResponse::getDate)
                .thenComparing(AvailableTimeSlotResponse::getStartTime));
        
        return availableSlots;
    }
    
    /**
     * Tìm các khung giờ trống trong một lịch làm việc cụ thể
     */
    private List<AvailableTimeSlotResponse> findAvailableSlotsInSchedule(
            Chef chef, 
            LocalDate date, 
            ChefSchedule schedule, 
            List<ChefBlockedDate> blockedDates, 
            int prepTimeMinutes,
            int cleanupTimeMinutes) {
        
        List<AvailableTimeSlotResponse> availableSlots = new ArrayList<>();
        
        // Thời gian bắt đầu và kết thúc của lịch làm việc
        LocalTime scheduleStart = schedule.getStartTime();
        LocalTime scheduleEnd = schedule.getEndTime();
        
        // Danh sách các khoảng thời gian không khả dụng (bị chặn hoặc đã có booking)
        List<TimeRange> unavailableRanges = new ArrayList<>();
        
        // Thêm các khoảng thời gian bị chặn
        for (ChefBlockedDate blockedDate : blockedDates) {
            // Nếu thời gian bị chặn nằm trong lịch làm việc
            if (hasTimeOverlap(scheduleStart, scheduleEnd, 
                    blockedDate.getStartTime(), blockedDate.getEndTime())) {
                
                // Thêm vào danh sách không khả dụng
                unavailableRanges.add(new TimeRange(
                        blockedDate.getStartTime(), 
                        blockedDate.getEndTime()));
            }
        }
        
        // Lấy danh sách các BookingDetail của chef trong ngày này
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date);
        
        // Lọc ra các booking còn active
        List<BookingDetail> activeBookings = bookingDetails.stream()
                .filter(detail -> {
                    // Booking vẫn còn active
                    Booking booking = detail.getBooking();
                    return !booking.getIsDeleted() &&
                           !List.of("CANCELED", "OVERDUE").contains(booking.getStatus()) &&
                           !detail.getIsDeleted() && 
                           !List.of("CANCELED", "OVERDUE").contains(detail.getStatus());
                })
                .collect(Collectors.toList());
        
        // Thêm các khoảng thời gian đã có booking, bao gồm cả 1 giờ trước đó cho việc di chuyển
        for (BookingDetail booking : activeBookings) {
            // Lấy thời gian bắt đầu nấu và thời gian kết thúc dịch vụ
            LocalTime bookingStartTime = booking.getStartTime();
            LocalTime bookingTravelTime = booking.getTimeBeginTravel();
            
            // Skip if required fields are null
            if (bookingStartTime == null || bookingTravelTime == null) {
                continue;
            }
            
            // Thêm vào danh sách không khả dụng
            // Phạm vi không khả dụng: từ thời gian bắt đầu di chuyển đến thời gian kết thúc dịch vụ
            if (hasTimeOverlap(scheduleStart, scheduleEnd, bookingTravelTime, bookingStartTime)) {
                unavailableRanges.add(new TimeRange(bookingTravelTime, bookingStartTime));
            }
        }
        
        // Nếu không có thông tin chi tiết về bookings, sử dụng phương pháp đơn giản hơn
        if (activeBookings.isEmpty() && bookingConflictService.hasBookingConflict(chef, date, scheduleStart, scheduleEnd)) {
            // TODO: Improve this to get exact booking conflict times
            // For now, we'll use a simplified approach - checking every 30 minute interval
            LocalTime current = scheduleStart;
            while (current.isBefore(scheduleEnd)) {
                LocalTime slotEnd = current.plusMinutes(30); // Check for 30-min slots
                if (slotEnd.isAfter(scheduleEnd)) {
                    break;
                }
                
                // Kiểm tra xung đột với booking
                if (bookingConflictService.hasBookingConflict(chef, date, current, slotEnd)) {
                    // Sử dụng slotEnd cho thời gian bắt đầu của booking, nhưng thêm 1 giờ cho việc di chuyển
                    LocalTime bookingStartTime = slotEnd;
                    LocalTime travelStartTime = bookingStartTime.minusHours(1);
                    
                    // Thêm vào danh sách không khả dụng với buffer trước 1 giờ
                    if (travelStartTime.isAfter(current)) {
                        unavailableRanges.add(new TimeRange(travelStartTime, bookingStartTime));
                    } else {
                        unavailableRanges.add(new TimeRange(current, bookingStartTime));
                    }
                }
                
                current = current.plusMinutes(30); // Kiểm tra mỗi 30 phút
            }
        }
        
        // Sắp xếp các khoảng thời gian không khả dụng theo thời gian bắt đầu
        Collections.sort(unavailableRanges, Comparator.comparing(TimeRange::getStart));
        
        // Hợp nhất các khoảng thời gian không khả dụng chồng chéo
        List<TimeRange> mergedUnavailableRanges = mergeOverlappingRanges(unavailableRanges);
        
        // Tìm các khoảng thời gian trống giữa các khoảng không khả dụng
        List<TimeRange> availableRanges = findAvailableRanges(
                scheduleStart, scheduleEnd, mergedUnavailableRanges);
        
        // Chuyển đổi các khoảng thời gian trống thành các khoảng đặt hàng có thể
        for (TimeRange availableRange : availableRanges) {
            LocalTime bookingStart = availableRange.getStart().plusMinutes(prepTimeMinutes);
            LocalTime bookingEnd = availableRange.getEnd().minusMinutes(cleanupTimeMinutes);
            
            // Kiểm tra nếu có booking tiếp theo, thêm 1 giờ cho việc di chuyển
            LocalTime adjustedBookingEnd = bookingEnd;
            
            // Tìm booking gần nhất sau khoảng thời gian này
            Optional<LocalTime> nextBookingStartTravel = findNextBookingTravelTime(activeBookings, bookingEnd);
            if (nextBookingStartTravel.isPresent()) {
                // Điều chỉnh thời gian kết thúc để đảm bảo có 1 giờ di chuyển trước booking tiếp theo
                LocalTime requiredEndTime = nextBookingStartTravel.get();
                if (requiredEndTime.isBefore(bookingEnd) || 
                        Duration.between(bookingEnd, requiredEndTime).toMinutes() < 60) {
                    // Điều chỉnh thời gian kết thúc đến 1 giờ trước thời gian di chuyển của booking tiếp theo
                    adjustedBookingEnd = requiredEndTime.minusHours(1);
                    
                    // Nếu thời gian kết thúc điều chỉnh sớm hơn thời gian bắt đầu, bỏ qua khoảng thời gian này
                    if (adjustedBookingEnd.isBefore(bookingStart) || 
                            adjustedBookingEnd.equals(bookingStart)) {
                        continue;
                    }
                }
            }
            
            // Tính thời lượng của khoảng thời gian này
            int durationMinutes = (int) Duration.between(bookingStart, adjustedBookingEnd).toMinutes();
            
            // Tạo response
            AvailableTimeSlotResponse slot = AvailableTimeSlotResponse.builder()
                    .chefId(chef.getId())
                    .chefName(chef.getUser().getFullName())
                    .date(date)
                    .startTime(bookingStart)
                    .endTime(adjustedBookingEnd)
                    .durationMinutes(durationMinutes)
                    .note("Available slot includes " + prepTimeMinutes + " min prep time and " + 
                            cleanupTimeMinutes + " min cleanup time" + 
                            (adjustedBookingEnd != bookingEnd ? " with 1 hour travel buffer before next booking" : ""))
                    .build();
            
            availableSlots.add(slot);
        }
        
        return availableSlots;
    }
    
    /**
     * Tìm thời gian bắt đầu di chuyển cho booking tiếp theo
     * 
     * @param bookings Danh sách các booking
     * @param afterTime Thời gian sau đó
     * @return Thời gian bắt đầu di chuyển của booking tiếp theo
     */
    private Optional<LocalTime> findNextBookingTravelTime(List<BookingDetail> bookings, LocalTime afterTime) {
        return bookings.stream()
                .filter(b -> b.getTimeBeginTravel() != null && b.getTimeBeginTravel().isAfter(afterTime))
                .map(BookingDetail::getTimeBeginTravel)
                .min(LocalTime::compareTo);
    }
    
    /**
     * Tìm các khoảng thời gian trống giữa các khoảng không khả dụng
     */
    private List<TimeRange> findAvailableRanges(
            LocalTime scheduleStart, 
            LocalTime scheduleEnd, 
            List<TimeRange> unavailableRanges) {
        
        List<TimeRange> availableRanges = new ArrayList<>();
        
        // Nếu không có khoảng thời gian không khả dụng, toàn bộ lịch làm việc là khả dụng
        if (unavailableRanges.isEmpty()) {
            availableRanges.add(new TimeRange(scheduleStart, scheduleEnd));
            return availableRanges;
        }
        
        // Kiểm tra trước khoảng không khả dụng đầu tiên
        if (scheduleStart.isBefore(unavailableRanges.get(0).getStart())) {
            LocalTime availableEnd = unavailableRanges.get(0).getStart();
            availableRanges.add(new TimeRange(scheduleStart, availableEnd));
        }
        
        // Kiểm tra giữa các khoảng không khả dụng
        for (int i = 0; i < unavailableRanges.size() - 1; i++) {
            LocalTime availableStart = unavailableRanges.get(i).getEnd();
            LocalTime availableEnd = unavailableRanges.get(i + 1).getStart();
            
            availableRanges.add(new TimeRange(availableStart, availableEnd));
        }
        
        // Kiểm tra sau khoảng không khả dụng cuối cùng
        if (unavailableRanges.get(unavailableRanges.size() - 1).getEnd().isBefore(scheduleEnd)) {
            LocalTime availableStart = unavailableRanges.get(unavailableRanges.size() - 1).getEnd();
            availableRanges.add(new TimeRange(availableStart, scheduleEnd));
        }
        
        return availableRanges;
    }
    
    /**
     * Hợp nhất các khoảng thời gian chồng chéo
     */
    private List<TimeRange> mergeOverlappingRanges(List<TimeRange> ranges) {
        if (ranges.isEmpty()) {
            return ranges;
        }
        
        List<TimeRange> mergedRanges = new ArrayList<>();
        TimeRange current = ranges.get(0);
        
        for (int i = 1; i < ranges.size(); i++) {
            TimeRange next = ranges.get(i);
            
            // Nếu hai khoảng chồng chéo, hợp nhất chúng
            if (hasTimeOverlap(current.getStart(), current.getEnd(), next.getStart(), next.getEnd())) {
                current = new TimeRange(
                        current.getStart().isBefore(next.getStart()) ? current.getStart() : next.getStart(),
                        current.getEnd().isAfter(next.getEnd()) ? current.getEnd() : next.getEnd()
                );
            } else {
                // Nếu không chồng chéo, thêm khoảng hiện tại vào kết quả và di chuyển đến khoảng tiếp theo
                mergedRanges.add(current);
                current = next;
            }
        }
        
        // Thêm khoảng cuối cùng
        mergedRanges.add(current);
        
        return mergedRanges;
    }
    
    /**
     * Kiểm tra xem một khung giờ có nằm trong lịch làm việc của chef hay không
     */
    private boolean isWithinChefSchedule(Chef chef, LocalDate date, LocalTime startTime, LocalTime endTime) {
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        List<ChefSchedule> schedules = scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek);
        
        for (ChefSchedule schedule : schedules) {
            // Nếu thời gian cần kiểm tra nằm trong một lịch làm việc
            if (!startTime.isBefore(schedule.getStartTime()) && !endTime.isAfter(schedule.getEndTime())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Kiểm tra xem một khung giờ có xung đột với các ngày bị chặn hay không
     */
    private boolean hasBlockedDateConflict(Chef chef, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<ChefBlockedDate> blockedDates = blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(
                chef, date);
        
        for (ChefBlockedDate blockedDate : blockedDates) {
            // Kiểm tra xung đột thời gian
            if (hasTimeOverlap(startTime, endTime, blockedDate.getStartTime(), blockedDate.getEndTime())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Kiểm tra xem hai khoảng thời gian có chồng chéo nhau hay không
     */
    private boolean hasTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
    
    /**
     * Lấy cài đặt thời gian của chef
     */
    private ChefTimeSettings getChefTimeSettings(Chef chef) {
        return timeSettingsRepository.findByChef(chef)
                .orElse(createDefaultTimeSettings(chef));
    }
    
    /**
     * Tạo cài đặt thời gian mặc định cho chef
     */
    private ChefTimeSettings createDefaultTimeSettings(Chef chef) {
        ChefTimeSettings settings = new ChefTimeSettings();
        settings.setChef(chef);
        settings.setStandardPrepTime(DEFAULT_PREP_TIME_MINUTES);
        settings.setStandardCleanupTime(DEFAULT_CLEANUP_TIME_MINUTES);
        return settings;
    }
    
    /**
     * Validate các tham số đầu vào
     */
    private void validateInputParameters(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Start date and end date cannot be null");
        }
        
        if (startDate.isBefore(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot search for dates in the past");
        }
        
        if (endDate.isBefore(startDate)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "End date must be after start date");
        }
        
        if (startDate.plusDays(MAX_DAYS_TO_SEARCH).isBefore(endDate)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                "Date range too large. Maximum range is " + MAX_DAYS_TO_SEARCH + " days");
        }
    }
    
    /**
     * Lấy thông tin chef hiện tại
     */
    private Chef getCurrentChef() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        return chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));
    }
    
    /**
     * Điều chỉnh các khung giờ trống dựa trên thời gian nấu
     * 
     * @param availableSlots Danh sách khung giờ trống
     * @param cookTimeHours Thời gian nấu (giờ)
     * @return Danh sách khung giờ trống đã điều chỉnh
     */
    private List<AvailableTimeSlotResponse> adjustTimeSlotsByCookingTime(
            List<AvailableTimeSlotResponse> availableSlots, BigDecimal cookTimeHours) {
        
        List<AvailableTimeSlotResponse> adjustedSlots = new ArrayList<>();
        
        // Chuyển thời gian nấu từ giờ sang phút
        int cookTimeMinutes = cookTimeHours.multiply(BigDecimal.valueOf(60)).intValue();
        
        for (AvailableTimeSlotResponse slot : availableSlots) {
            // Thời gian bắt đầu mới = thời gian bắt đầu cũ + thời gian nấu
            LocalTime newStartTime = slot.getStartTime().plusMinutes(cookTimeMinutes);
            
            // Nếu thời gian bắt đầu mới vẫn trước thời gian kết thúc và vẫn có đủ thời gian tối thiểu
            if (newStartTime.isBefore(slot.getEndTime()) && 
                    Duration.between(newStartTime, slot.getEndTime()).toMinutes() >= DEFAULT_MIN_SLOT_DURATION_MINUTES) {
                
                // Tạo khung giờ mới đã điều chỉnh
                AvailableTimeSlotResponse adjustedSlot = new AvailableTimeSlotResponse();
                adjustedSlot.setChefId(slot.getChefId());
                adjustedSlot.setChefName(slot.getChefName());
                adjustedSlot.setDate(slot.getDate());
                adjustedSlot.setStartTime(newStartTime);
                adjustedSlot.setEndTime(slot.getEndTime());
                adjustedSlot.setDurationMinutes((int) Duration.between(newStartTime, slot.getEndTime()).toMinutes());
                adjustedSlot.setNote("Cooking starts at " + slot.getStartTime() + ", service begins at " + newStartTime);
                
                adjustedSlots.add(adjustedSlot);
            }
        }
        
        return adjustedSlots;
    }
    
    /**
     * Class đại diện cho một khoảng thời gian
     */
    private static class TimeRange {
        private final LocalTime start;
        private final LocalTime end;
        
        public TimeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
        
        public LocalTime getStart() {
            return start;
        }
        
        public LocalTime getEnd() {
            return end;
        }
    }
} 