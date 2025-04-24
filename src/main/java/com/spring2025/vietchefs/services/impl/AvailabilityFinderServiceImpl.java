package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefBlockedDate;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.AvailableTimeSlotRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.repositories.BookingDetailRepository;
import com.spring2025.vietchefs.repositories.ChefBlockedDateRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.PackageRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.AvailabilityFinderService;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.impl.CalculateService;
import com.spring2025.vietchefs.services.impl.DistanceService;
import com.spring2025.vietchefs.services.impl.TimeZoneService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityFinderServiceImpl implements AvailabilityFinderService {

    // Các hằng số cho việc tìm kiếm khung giờ
    // private static final int DEFAULT_MIN_SLOT_DURATION_MINUTES = 120; // 2 giờ
    private static final int DEFAULT_PREP_TIME_MINUTES = 0; // 0 phút
    private static final int DEFAULT_CLEANUP_TIME_MINUTES = 0; // 0 phút
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
    private BookingConflictService bookingConflictService;
    
    @Autowired
    private CalculateService calculateService;
    
    @Autowired
    private PackageRepository packageRepository;
    
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    
    @Autowired
    private DistanceService distanceService;
    
    @Autowired
    private TimeZoneService timeZoneService;
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsForChef(
            Long chefId, LocalDate startDate, LocalDate endDate) {
        
        // Kiểm tra tham số đầu vào
        validateInputParameters(startDate, endDate);
        
        // Lấy thông tin chef
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, 
                    "Chef not found with id: " + chefId));
        
        // Wrap the result in filterValidTimeSlots
        return filterValidTimeSlots(findAvailableTimeSlots(chef, startDate, endDate));
    }
    
    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsForCurrentChef(
            LocalDate startDate, LocalDate endDate) {
        
        // Kiểm tra tham số đầu vào
        validateInputParameters(startDate, endDate);
        
        // Lấy thông tin chef hiện tại
        Chef chef = getCurrentChef();
        
        // Wrap the result in filterValidTimeSlots
        return filterValidTimeSlots(findAvailableTimeSlots(chef, startDate, endDate));
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
        
        // Wrap the result in filterValidTimeSlots
        return filterValidTimeSlots(findAvailableTimeSlots(chef, date, date));
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
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithInSingleDate(
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
        
        // Get chef and customer timezones
        String chefTimezone = timeZoneService.getTimezoneFromAddress(chefAddress);
        String customerTimezone = timeZoneService.getTimezoneFromAddress(customerLocation);
        
        System.out.println("Chef timezone: " + chefTimezone);
        System.out.println("Customer timezone: " + customerTimezone);
        
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
        
        // Tính tổng thời gian chuẩn bị (giờ)
        int totalPrepMinutes = (int)(cookTimeHours.floatValue() * 60) + (int)(travelTimeHours.floatValue() * 60);
        
        // Sử dụng minDuration null để lấy tất cả các khung giờ trống cơ bản
        // Các slots này đã loại bỏ các booking time
        List<AvailableTimeSlotResponse> availableSlots = findAvailableTimeSlots(chef, date, date);
        
        // Kết quả cuối cùng
        List<AvailableTimeSlotResponse> adjustedSlots = new ArrayList<>();
        
        // Kiểm tra từng khung giờ trống và áp dụng thời gian chuẩn bị
        for (AvailableTimeSlotResponse slot : availableSlots) {
            // Cần điều chỉnh thời gian bắt đầu để tính đến thời gian nấu ăn và di chuyển
            LocalTime adjustedStartTime = slot.getStartTime().plusMinutes(totalPrepMinutes);
            
            // Nếu thời gian bắt đầu điều chỉnh vẫn trước thời gian kết thúc slot
            if (adjustedStartTime.isBefore(slot.getEndTime())) {
                AvailableTimeSlotResponse adjustedSlot = new AvailableTimeSlotResponse();
                adjustedSlot.setChefId(slot.getChefId());
                adjustedSlot.setChefName(slot.getChefName());
                adjustedSlot.setDate(slot.getDate());
                adjustedSlot.setStartTime(adjustedStartTime); 
                adjustedSlot.setEndTime(slot.getEndTime());
                adjustedSlot.setDurationMinutes((int) Duration.between(adjustedStartTime, slot.getEndTime()).toMinutes());
                adjustedSlot.setNote("Adjusted for " + 
                        travelTimeHours.setScale(2, RoundingMode.HALF_UP).toString() + " hours travel time and " +
                        cookTimeHours.setScale(2, RoundingMode.HALF_UP).toString() + " hours cooking time");
                
                // Chỉ thêm slot nếu thời lượng hợp lệ (ít nhất 30 phút)
                if (adjustedSlot.getDurationMinutes() >= 30) {
                    adjustedSlots.add(adjustedSlot);
                }
            }
            // Nếu thời gian bắt đầu điều chỉnh sau thời gian kết thúc, bỏ qua slot này
        }
        
        // Sắp xếp kết quả theo thời gian bắt đầu
        Collections.sort(adjustedSlots, Comparator.comparing(AvailableTimeSlotResponse::getStartTime));
        
        // Debug current slots before filtering
        System.out.println("DEBUG: Before final filtering, found " + adjustedSlots.size() + " slots");
        for (AvailableTimeSlotResponse slot : adjustedSlots) {
            System.out.println("DEBUG: Pre-filter slot: " + slot.getStartTime() + " to " + slot.getEndTime());
        }
        
        // Final sanity check - ensure each slot is valid and within a schedule
        List<AvailableTimeSlotResponse> finalSlots = adjustedSlots.stream()
                .filter(slot -> {
                    Chef slotChef = chefRepository.findById(slot.getChefId()).orElse(null);
                    if (slotChef == null) {
                        System.out.println("DEBUG: Filtered out slot - chef not found");
                        return false;
                    }
                    
                    // Get all schedules for this day
                    int dayOfWeek = (date.getDayOfWeek().getValue() - 1);
                    List<ChefSchedule> schedules = scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(slotChef, dayOfWeek);
                    
                    // For debugging, print all schedules
                    System.out.println("DEBUG: Found " + schedules.size() + " schedules for dayOfWeek " + dayOfWeek);
                    for (ChefSchedule schedule : schedules) {
                        System.out.println("DEBUG: Schedule: " + schedule.getStartTime() + " to " + schedule.getEndTime());
                    }
                    
                    // Check if slot fits entirely within a single schedule
                    for (ChefSchedule schedule : schedules) {
                        boolean slotStartsInSchedule = !slot.getStartTime().isBefore(schedule.getStartTime());
                        boolean slotEndsInSchedule = !slot.getEndTime().isAfter(schedule.getEndTime());
                        boolean isValid = slotStartsInSchedule && slotEndsInSchedule;
                        
                        if (isValid) {
                            System.out.println("DEBUG: Slot " + slot.getStartTime() + "-" + slot.getEndTime() + 
                                              " is valid within schedule " + schedule.getStartTime() + "-" + schedule.getEndTime());
                            return true;
                        }
                    }
                    
                    System.out.println("DEBUG: Filtered out slot " + slot.getStartTime() + "-" + slot.getEndTime() + 
                                      " - not within any schedule");
                    return false;
                })
                .collect(Collectors.toList());
        
        System.out.println("DEBUG: After final filtering, found " + finalSlots.size() + " slots");
        
        // Convert time slots from chef's timezone to customer's timezone
        List<AvailableTimeSlotResponse> convertedSlots = new ArrayList<>();
        for (AvailableTimeSlotResponse slot : finalSlots) {
            // Create LocalDateTime objects for conversion
            LocalDateTime startDateTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
            LocalDateTime endDateTime = LocalDateTime.of(slot.getDate(), slot.getEndTime());
            
            // Convert from chef's timezone to customer's timezone
            LocalDateTime convertedStartDateTime = timeZoneService.convertBetweenTimezones(
                    startDateTime, chefTimezone, customerTimezone);
            LocalDateTime convertedEndDateTime = timeZoneService.convertBetweenTimezones(
                    endDateTime, chefTimezone, customerTimezone);
            
            // Create a new slot with converted times
            AvailableTimeSlotResponse convertedSlot = new AvailableTimeSlotResponse();
            convertedSlot.setChefId(slot.getChefId());
            convertedSlot.setChefName(slot.getChefName());
            convertedSlot.setDate(convertedStartDateTime.toLocalDate());
            convertedSlot.setStartTime(convertedStartDateTime.toLocalTime());
            convertedSlot.setEndTime(convertedEndDateTime.toLocalTime());
            convertedSlot.setDurationMinutes(slot.getDurationMinutes());
            convertedSlot.setNote(slot.getNote());
            
            convertedSlots.add(convertedSlot);
        }
        
        // Check for day boundary crossings and adjust
        List<AvailableTimeSlotResponse> adjustedConvertedSlots = new ArrayList<>();
        for (AvailableTimeSlotResponse slot : convertedSlots) {
            // If end time is earlier than start time, it means we've crossed a day boundary
            if (slot.getEndTime().isBefore(slot.getStartTime())) {
                // Create a slot for the first day (from start time to midnight)
                AvailableTimeSlotResponse firstDaySlot = new AvailableTimeSlotResponse();
                firstDaySlot.setChefId(slot.getChefId());
                firstDaySlot.setChefName(slot.getChefName());
                firstDaySlot.setDate(slot.getDate());
                firstDaySlot.setStartTime(slot.getStartTime());
                firstDaySlot.setEndTime(LocalTime.MAX); // 23:59:59.999999999
                firstDaySlot.setDurationMinutes((int) Duration.between(slot.getStartTime(), LocalTime.MAX).toMinutes());
                firstDaySlot.setNote(slot.getNote());
                
                // Create a slot for the next day (from midnight to end time)
                AvailableTimeSlotResponse nextDaySlot = new AvailableTimeSlotResponse();
                nextDaySlot.setChefId(slot.getChefId());
                nextDaySlot.setChefName(slot.getChefName());
                nextDaySlot.setDate(slot.getDate().plusDays(1));
                nextDaySlot.setStartTime(LocalTime.MIN); // 00:00
                nextDaySlot.setEndTime(slot.getEndTime());
                nextDaySlot.setDurationMinutes((int) Duration.between(LocalTime.MIN, slot.getEndTime()).toMinutes());
                nextDaySlot.setNote(slot.getNote());
                
                adjustedConvertedSlots.add(firstDaySlot);
                adjustedConvertedSlots.add(nextDaySlot);
            } else {
                // No day boundary crossed, just add the slot as is
                adjustedConvertedSlots.add(slot);
            }
        }
        
        // Sort the final slots by date and start time
        adjustedConvertedSlots.sort(Comparator
                .comparing(AvailableTimeSlotResponse::getDate)
                .thenComparing(AvailableTimeSlotResponse::getStartTime));
        
        System.out.println("DEBUG: After timezone conversion, returning " + adjustedConvertedSlots.size() + " slots");
        return adjustedConvertedSlots;
    }

    @Override
    public List<AvailableTimeSlotResponse> findAvailableTimeSlotsWithInMultipleDates(Long chefId, String customerLocation, int guestCount, int maxDishesPerMeal, List<AvailableTimeSlotRequest> requests) {
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

        // Get chef and customer timezones
        String chefTimezone = timeZoneService.getTimezoneFromAddress(chefAddress);
        String customerTimezone = timeZoneService.getTimezoneFromAddress(customerLocation);
        
        System.out.println("Chef timezone: " + chefTimezone);
        System.out.println("Customer timezone: " + customerTimezone);

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

            // Tính thời gian nấu ăn (giờ)
            BigDecimal cookTimeHours;
            if (menuId != null) {
                cookTimeHours = calculateService.calculateTotalCookTimeFromMenu(menuId, dishIds, guestCount);
            } else if (dishIds != null && !dishIds.isEmpty()) {
                cookTimeHours = calculateService.calculateTotalCookTime(dishIds, guestCount);
            } else {
                cookTimeHours = calculateService.calculateMaxCookTime(chefId, maxDishesPerMeal, guestCount);
            }
            
            // Tính tổng thời gian chuẩn bị (phút)
            int totalPrepMinutes = (int)(cookTimeHours.floatValue() * 60) + (int)(travelTimeHours.floatValue() * 60);

            // Lấy các slots đã loại bỏ bookings
            List<AvailableTimeSlotResponse> availableSlots = findAvailableTimeSlots(chef, date, date);
            
            // Kiểm tra từng khung giờ trống và áp dụng thời gian chuẩn bị
            for (AvailableTimeSlotResponse slot : availableSlots) {
                // Điều chỉnh thời gian bắt đầu dựa trên thời gian chuẩn bị
                LocalTime adjustedStartTime = slot.getStartTime().plusMinutes(totalPrepMinutes);
                
                // Nếu thời gian bắt đầu điều chỉnh vẫn trước thời gian kết thúc slot
                if (adjustedStartTime.isBefore(slot.getEndTime())) {
                    AvailableTimeSlotResponse adjustedSlot = new AvailableTimeSlotResponse();
                    adjustedSlot.setChefId(slot.getChefId());
                    adjustedSlot.setChefName(slot.getChefName());
                    adjustedSlot.setDate(slot.getDate());
                    adjustedSlot.setStartTime(adjustedStartTime);
                    adjustedSlot.setEndTime(slot.getEndTime());
                    adjustedSlot.setDurationMinutes((int) Duration.between(adjustedStartTime, slot.getEndTime()).toMinutes());
                    adjustedSlot.setNote("Adjusted for " + 
                            travelTimeHours.setScale(2, RoundingMode.HALF_UP).toString() + " hours travel time and " +
                            cookTimeHours.setScale(2, RoundingMode.HALF_UP).toString() + " hours cooking time");
                    
                    // Chỉ thêm slot nếu thời lượng hợp lệ (ít nhất 30 phút)
                    if (adjustedSlot.getDurationMinutes() >= 30) {
                        allAdjustedSlots.add(adjustedSlot);
                    }
                }
                // Nếu thời gian điều chỉnh vượt quá thời gian kết thúc, bỏ qua slot này
            }
        }

        // Sắp xếp slot theo ngày và thời gian bắt đầu
        Collections.sort(allAdjustedSlots, Comparator
                .comparing(AvailableTimeSlotResponse::getDate)
                .thenComparing(AvailableTimeSlotResponse::getStartTime));

        // Debug current slots before filtering
        System.out.println("DEBUG: Before final filtering, found " + allAdjustedSlots.size() + " slots");
        for (AvailableTimeSlotResponse slot : allAdjustedSlots) {
            System.out.println("DEBUG: Pre-filter slot: " + slot.getDate() + " " + slot.getStartTime() + " to " + slot.getEndTime());
        }
        
        // Final sanity check - ensure each slot is valid and within a schedule
        List<AvailableTimeSlotResponse> finalSlots = allAdjustedSlots.stream()
                .filter(slot -> {
                    Chef slotChef = chefRepository.findById(slot.getChefId()).orElse(null);
                    if (slotChef == null) {
                        System.out.println("DEBUG: Filtered out slot - chef not found");
                        return false;
                    }
                    
                    // Get all schedules for this day
                    int dayOfWeek = (slot.getDate().getDayOfWeek().getValue() - 1);
                    List<ChefSchedule> schedules = scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(slotChef, dayOfWeek);
                    
                    // For debugging, print all schedules
                    System.out.println("DEBUG: Found " + schedules.size() + " schedules for dayOfWeek " + dayOfWeek);
                    for (ChefSchedule schedule : schedules) {
                        System.out.println("DEBUG: Schedule: " + schedule.getStartTime() + " to " + schedule.getEndTime());
                    }
                    
                    // Check if slot fits entirely within a single schedule
                    for (ChefSchedule schedule : schedules) {
                        boolean slotStartsInSchedule = !slot.getStartTime().isBefore(schedule.getStartTime());
                        boolean slotEndsInSchedule = !slot.getEndTime().isAfter(schedule.getEndTime());
                        boolean isValid = slotStartsInSchedule && slotEndsInSchedule;
                        
                        if (isValid) {
                            System.out.println("DEBUG: Slot " + slot.getStartTime() + "-" + slot.getEndTime() + 
                                              " is valid within schedule " + schedule.getStartTime() + "-" + schedule.getEndTime());
                            return true;
                        }
                    }
                    
                    System.out.println("DEBUG: Filtered out slot " + slot.getStartTime() + "-" + slot.getEndTime() + 
                                      " - not within any schedule");
                    return false;
                })
                .collect(Collectors.toList());
        
        System.out.println("DEBUG: After final filtering, found " + finalSlots.size() + " slots");
        
        // Convert time slots from chef's timezone to customer's timezone
        List<AvailableTimeSlotResponse> convertedSlots = new ArrayList<>();
        for (AvailableTimeSlotResponse slot : finalSlots) {
            // Create LocalDateTime objects for conversion
            LocalDateTime startDateTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
            LocalDateTime endDateTime = LocalDateTime.of(slot.getDate(), slot.getEndTime());
            
            // Convert from chef's timezone to customer's timezone
            LocalDateTime convertedStartDateTime = timeZoneService.convertBetweenTimezones(
                    startDateTime, chefTimezone, customerTimezone);
            LocalDateTime convertedEndDateTime = timeZoneService.convertBetweenTimezones(
                    endDateTime, chefTimezone, customerTimezone);
            
            // Create a new slot with converted times
            AvailableTimeSlotResponse convertedSlot = new AvailableTimeSlotResponse();
            convertedSlot.setChefId(slot.getChefId());
            convertedSlot.setChefName(slot.getChefName());
            convertedSlot.setDate(convertedStartDateTime.toLocalDate());
            convertedSlot.setStartTime(convertedStartDateTime.toLocalTime());
            convertedSlot.setEndTime(convertedEndDateTime.toLocalTime());
            convertedSlot.setDurationMinutes(slot.getDurationMinutes());
            convertedSlot.setNote(slot.getNote());
            
            convertedSlots.add(convertedSlot);
        }
        
        // Check for day boundary crossings and adjust
        List<AvailableTimeSlotResponse> adjustedConvertedSlots = new ArrayList<>();
        for (AvailableTimeSlotResponse slot : convertedSlots) {
            // If end time is earlier than start time, it means we've crossed a day boundary
            if (slot.getEndTime().isBefore(slot.getStartTime())) {
                // Create a slot for the first day (from start time to midnight)
                AvailableTimeSlotResponse firstDaySlot = new AvailableTimeSlotResponse();
                firstDaySlot.setChefId(slot.getChefId());
                firstDaySlot.setChefName(slot.getChefName());
                firstDaySlot.setDate(slot.getDate());
                firstDaySlot.setStartTime(slot.getStartTime());
                firstDaySlot.setEndTime(LocalTime.MAX); // 23:59:59.999999999
                firstDaySlot.setDurationMinutes((int) Duration.between(slot.getStartTime(), LocalTime.MAX).toMinutes());
                firstDaySlot.setNote(slot.getNote());
                
                // Create a slot for the next day (from midnight to end time)
                AvailableTimeSlotResponse nextDaySlot = new AvailableTimeSlotResponse();
                nextDaySlot.setChefId(slot.getChefId());
                nextDaySlot.setChefName(slot.getChefName());
                nextDaySlot.setDate(slot.getDate().plusDays(1));
                nextDaySlot.setStartTime(LocalTime.MIN); // 00:00
                nextDaySlot.setEndTime(slot.getEndTime());
                nextDaySlot.setDurationMinutes((int) Duration.between(LocalTime.MIN, slot.getEndTime()).toMinutes());
                nextDaySlot.setNote(slot.getNote());
                
                adjustedConvertedSlots.add(firstDaySlot);
                adjustedConvertedSlots.add(nextDaySlot);
            } else {
                // No day boundary crossed, just add the slot as is
                adjustedConvertedSlots.add(slot);
            }
        }
        
        // Sort the final slots by date and start time
        adjustedConvertedSlots.sort(Comparator
                .comparing(AvailableTimeSlotResponse::getDate)
                .thenComparing(AvailableTimeSlotResponse::getStartTime));
        
        System.out.println("DEBUG: After timezone conversion, returning " + adjustedConvertedSlots.size() + " slots");
        return adjustedConvertedSlots;
    }

    /**
     * Tìm tất cả các khung giờ trống cho một chef trong khoảng ngày
     */
    private List<AvailableTimeSlotResponse> findAvailableTimeSlots(
            Chef chef, LocalDate startDate, LocalDate endDate) {
        
        // Using default preparation and cleanup times directly
        int prepTimeMinutes = DEFAULT_PREP_TIME_MINUTES;
        int cleanupTimeMinutes = DEFAULT_CLEANUP_TIME_MINUTES;
        
        List<AvailableTimeSlotResponse> availableSlots = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        // Duyệt qua từng ngày trong khoảng
        while (!currentDate.isAfter(endDate)) {
            // Lấy lịch làm việc của chef vào ngày hiện tại
            // Chuyển đổi dayOfWeek từ 1-7 (Monday-Sunday) sang 0-6 (Monday-Sunday)
            int dayOfWeek = (currentDate.getDayOfWeek().getValue() - 1);
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
    public List<AvailableTimeSlotResponse> findAvailableSlotsInSchedule(
            Chef chef, LocalDate date, ChefSchedule schedule, 
            List<ChefBlockedDate> blockedDates, int prepTimeMinutes, int cleanupTimeMinutes) {
        
        List<AvailableTimeSlotResponse> availableSlots = new ArrayList<>();
        
        // Get start and end times from schedule
        LocalTime scheduleStart = schedule.getStartTime();
        LocalTime scheduleEnd = schedule.getEndTime();
        
        // Check if schedule time is blocked
        boolean isBlocked = blockedDates.stream()
                .anyMatch(blockedDate -> 
                    hasTimeOverlap(scheduleStart, scheduleEnd, 
                                 blockedDate.getStartTime(), blockedDate.getEndTime()));
        
        if (isBlocked) {
            return availableSlots;
        }
        
        // Get all bookings for this date
        List<BookingDetail> bookings = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date);
        
        // Filter to only consider active bookings that overlap with this schedule's time range
        List<BookingDetail> activeBookings = bookings.stream()
                .filter(detail -> {
                    if (detail == null) return false;
                    
                    Booking booking = detail.getBooking();
                    if (booking == null) return false;
                    
                    // Check if booking is active
                    boolean isActive = !booking.getIsDeleted() && 
                           !List.of("CANCELED", "OVERDUE", "REJECTED").contains(booking.getStatus()) &&
                           !detail.getIsDeleted() && 
                           !List.of("CANCELED", "OVERDUE", "REJECTED").contains(detail.getStatus());
                    
                    if (!isActive) return false;
                    
                    // Check if booking overlaps with this schedule time range
                    LocalTime bookingStart = detail.getTimeBeginTravel();
                    LocalTime bookingEnd = detail.getStartTime();
                    
                    // Check if the booking time range and schedule time range overlap
                    // A booking overlaps with the schedule if:
                    // 1. The booking starts within the schedule time range, or
                    // 2. The booking ends within the schedule time range, or
                    // 3. The booking spans the entire schedule time range
                    return (bookingStart.compareTo(scheduleStart) >= 0 && bookingStart.compareTo(scheduleEnd) < 0) || // Booking starts in schedule 
                           (bookingEnd.compareTo(scheduleStart) > 0 && bookingEnd.compareTo(scheduleEnd) <= 0) ||     // Booking ends in schedule
                           (bookingStart.compareTo(scheduleStart) <= 0 && bookingEnd.compareTo(scheduleEnd) >= 0);    // Booking spans entire schedule
                })
                .sorted(Comparator.comparing(BookingDetail::getTimeBeginTravel))
                .collect(Collectors.toList());
        
        // Nếu không có booking nào, trả về một slot cho toàn bộ lịch làm việc
        if (activeBookings.isEmpty()) {
            AvailableTimeSlotResponse slot = new AvailableTimeSlotResponse();
            slot.setChefId(chef.getId());
            slot.setChefName(chef.getUser().getFullName());
            slot.setDate(date);
            slot.setStartTime(scheduleStart);
            slot.setEndTime(scheduleEnd);
            slot.setDurationMinutes((int) Duration.between(scheduleStart, scheduleEnd).toMinutes());
            availableSlots.add(slot);
            return availableSlots;
        }
        
        // Thời gian hiện tại bắt đầu từ thời gian bắt đầu lịch làm việc
        LocalTime currentTime = scheduleStart;
        
        // Process each booking
        for (BookingDetail booking : activeBookings) {
            // Thời gian bắt đầu booking là thời gian bắt đầu di chuyển
            LocalTime bookingStartTime = booking.getTimeBeginTravel();
            
            // If there's a gap between current time and booking start, it's a potential slot
            if (currentTime.isBefore(bookingStartTime)) {
                // Tạo slot từ currentTime đến bookingStartTime
                AvailableTimeSlotResponse slot = new AvailableTimeSlotResponse();
                slot.setChefId(chef.getId());
                slot.setChefName(chef.getUser().getFullName());
                slot.setDate(date);
                slot.setStartTime(currentTime);
                slot.setEndTime(bookingStartTime);
                slot.setDurationMinutes((int) Duration.between(currentTime, bookingStartTime).toMinutes());
                
                // Chỉ thêm slot nếu thời lượng hợp lệ (ít nhất 30 phút)
                if (slot.getDurationMinutes() >= 30) {
                    availableSlots.add(slot);
                }
            }
            
            // Move current time to after this booking 
            // Thời gian kết thúc booking là startTime (thời điểm khách bắt đầu ăn)
            // Cộng thêm 30 phút làm thời gian nghỉ
            currentTime = booking.getStartTime().plusMinutes(30);
            
            // If current time is now beyond the schedule's end time, we're done with this schedule
            if (currentTime.isAfter(scheduleEnd)) {
                break;
            }
        }
        
        // Check for one last slot after the last booking
        if (currentTime.isBefore(scheduleEnd)) {
            AvailableTimeSlotResponse slot = new AvailableTimeSlotResponse();
            slot.setChefId(chef.getId());
            slot.setChefName(chef.getUser().getFullName());
            slot.setDate(date);
            slot.setStartTime(currentTime);
            slot.setEndTime(scheduleEnd);
            slot.setDurationMinutes((int) Duration.between(currentTime, scheduleEnd).toMinutes());
            
            // Chỉ thêm slot nếu thời lượng hợp lệ (ít nhất 30 phút)
            if (slot.getDurationMinutes() >= 30) {
                availableSlots.add(slot);
            }
        }
        
        return availableSlots;
    }
    
    /**
     * Kiểm tra xem một khung giờ có nằm trong lịch làm việc của chef hay không
     */
    private boolean isWithinChefSchedule(Chef chef, LocalDate date, LocalTime startTime, LocalTime endTime) {
        int dayOfWeek = (date.getDayOfWeek().getValue() - 1);
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
     * Lọc các khung giờ không hợp lệ (thời gian bắt đầu sau thời gian kết thúc)
     */
    private List<AvailableTimeSlotResponse> filterValidTimeSlots(List<AvailableTimeSlotResponse> slots) {
        // First filter out slots with invalid time ranges
        List<AvailableTimeSlotResponse> validTimeSlots = slots.stream()
                .filter(slot -> {
                    if (slot.getStartTime() == null || slot.getEndTime() == null) {
                        return false;
                    }
                    return !slot.getStartTime().isAfter(slot.getEndTime()) && slot.getDurationMinutes() > 0;
                })
                .collect(Collectors.toList());
        
        // Now filter to ensure slots are within chef's schedules
        return validTimeSlots.stream()
                .filter(slot -> {
                    // Skip slots without chef information
                    if (slot.getChefId() == null) {
                        return false;
                    }
                    
                    // Find the chef
                    Chef chef = chefRepository.findById(slot.getChefId())
                            .orElse(null);
                    if (chef == null) {
                        return false;
                    }
                    
                    // Check if the slot is within chef's schedules
                    return isWithinChefSchedule(chef, slot.getDate(), slot.getStartTime(), slot.getEndTime());
                })
                .collect(Collectors.toList());
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
} 