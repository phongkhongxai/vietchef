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
        
        // Lưu thời gian request hiện tại
        LocalDateTime requestTime = LocalDateTime.now();
        
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
        
        // Lọc các khung giờ theo business rule: phải sau thời gian request ít nhất 24h
        List<AvailableTimeSlotResponse> validTimeSlots = finalSlots.stream()
                .filter(slot -> {
                    // Tạo LocalDateTime từ date và startTime của slot
                    LocalDateTime slotStartDateTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
                    
                    // Kiểm tra xem slot có sau thời gian request ít nhất 24h không
                    return slotStartDateTime.isAfter(requestTime.plusHours(24));
                })
                .collect(Collectors.toList());
        
        System.out.println("DEBUG: After 24h business rule filtering, found " + validTimeSlots.size() + " slots");
        
        // Convert time slots from chef's timezone to customer's timezone
        List<AvailableTimeSlotResponse> convertedSlots = new ArrayList<>();
        for (AvailableTimeSlotResponse slot : validTimeSlots) {
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
        
        // Lưu thời gian request hiện tại
        LocalDateTime requestTime = LocalDateTime.now();

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
        
        // Lọc các khung giờ theo business rule: phải sau thời gian request ít nhất 24h
        List<AvailableTimeSlotResponse> validTimeSlots = finalSlots.stream()
                .filter(slot -> {
                    // Tạo LocalDateTime từ date và startTime của slot
                    LocalDateTime slotStartDateTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
                    
                    // Kiểm tra xem slot có sau thời gian request ít nhất 24h không
                    return slotStartDateTime.isAfter(requestTime.plusHours(24));
                })
                .collect(Collectors.toList());
        
        System.out.println("DEBUG: After 24h business rule filtering, found " + validTimeSlots.size() + " slots");
        
        // Convert time slots from chef's timezone to customer's timezone
        List<AvailableTimeSlotResponse> convertedSlots = new ArrayList<>();
        for (AvailableTimeSlotResponse slot : validTimeSlots) {
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
        
        // Filter blocked dates to only include those matching the given date
        List<ChefBlockedDate> filteredBlockedDates = blockedDates.stream()
                .filter(blockedDate -> blockedDate.getBlockedDate().equals(date))
                .collect(Collectors.toList());
        
        // Sort blocked dates by start time for easier processing
        filteredBlockedDates.sort(Comparator.comparing(ChefBlockedDate::getStartTime));
        
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
        
        // If no bookings, process the entire schedule with blocked dates
        if (activeBookings.isEmpty()) {
            // Start with the schedule start time
            LocalTime currentTime = scheduleStart;
            
            // Process each blocked date to create slots around them
            for (ChefBlockedDate blockedDate : filteredBlockedDates) {
                LocalTime blockedStart = blockedDate.getStartTime();
                LocalTime blockedEnd = blockedDate.getEndTime();
                
                // Only process if this blocked date overlaps with our schedule
                if (hasTimeOverlap(scheduleStart, scheduleEnd, blockedStart, blockedEnd)) {
                    // Create a slot before the blocked period if there's enough time
                    if (currentTime.isBefore(blockedStart)) {
                        createAndAddSlot(availableSlots, chef, date, currentTime, blockedStart);
                    }
                    
                    // Move current time to after this blocked date
                    currentTime = blockedEnd;
                    
                    // If current time is beyond schedule end, we're done
                    if (currentTime.isAfter(scheduleEnd)) {
                        break;
                    }
                }
            }
            
            // Add one final slot after the last blocked date (if any remaining time)
            if (currentTime.isBefore(scheduleEnd)) {
                createAndAddSlot(availableSlots, chef, date, currentTime, scheduleEnd);
            }
            
            return availableSlots;
        }
        
        // If we have both bookings and blocked dates, process them together
        // Start with the schedule start time
        LocalTime currentTime = scheduleStart;
        
        // Create a combined timeline of events (both bookings and blocked dates)
        List<TimelineEvent> timeline = new ArrayList<>();
        
        // Add bookings to timeline
        for (BookingDetail booking : activeBookings) {
            timeline.add(new TimelineEvent(booking.getTimeBeginTravel(), EventType.BOOKING_START, booking));
            timeline.add(new TimelineEvent(booking.getStartTime().plusMinutes(30), EventType.BOOKING_END, booking));
        }
        
        // Add blocked dates to timeline
        for (ChefBlockedDate blockedDate : filteredBlockedDates) {
            // Only add if it overlaps with our schedule
            if (hasTimeOverlap(scheduleStart, scheduleEnd, blockedDate.getStartTime(), blockedDate.getEndTime())) {
                timeline.add(new TimelineEvent(blockedDate.getStartTime(), EventType.BLOCKED_START, blockedDate));
                timeline.add(new TimelineEvent(blockedDate.getEndTime(), EventType.BLOCKED_END, blockedDate));
            }
        }
        
        // Sort timeline by time
        timeline.sort(Comparator.comparing(TimelineEvent::getTime));
        
        // Process timeline to create available slots
        int blockLevel = 0; // Track how many overlapping blocks we have (booking or blocked date)
        
        for (TimelineEvent event : timeline) {
            // Skip events outside our schedule time
            if (event.getTime().isBefore(scheduleStart) || !event.getTime().isBefore(scheduleEnd)) {
                continue;
            }
            
            if (event.getType() == EventType.BOOKING_START || event.getType() == EventType.BLOCKED_START) {
                // If we're going from available to blocked, create a slot
                if (blockLevel == 0 && currentTime.isBefore(event.getTime())) {
                    createAndAddSlot(availableSlots, chef, date, currentTime, event.getTime());
                }
                blockLevel++;
            } else { // BOOKING_END or BLOCKED_END
                blockLevel--;
                // If we're going back to available, update current time
                if (blockLevel == 0) {
                    currentTime = event.getTime();
                }
            }
        }
        
        // Add one final slot after the last event (if any remaining time and not blocked)
        if (blockLevel == 0 && currentTime.isBefore(scheduleEnd)) {
            createAndAddSlot(availableSlots, chef, date, currentTime, scheduleEnd);
        }
        
        return availableSlots;
    }
    
    // Helper class for timeline processing
    private class TimelineEvent {
        private LocalTime time;
        private EventType type;
        private Object data; // Either BookingDetail or ChefBlockedDate
        
        public TimelineEvent(LocalTime time, EventType type, Object data) {
            this.time = time;
            this.type = type;
            this.data = data;
        }
        
        public LocalTime getTime() {
            return time;
        }
        
        public EventType getType() {
            return type;
        }
        
        public Object getData() {
            return data;
        }
    }
    
    // Event types for timeline
    private enum EventType {
        BOOKING_START,
        BOOKING_END,
        BLOCKED_START,
        BLOCKED_END
    }
    
    // Helper method to create and add a slot if it's long enough
    private void createAndAddSlot(List<AvailableTimeSlotResponse> slots, Chef chef, LocalDate date, 
                                LocalTime startTime, LocalTime endTime) {
        // Create slot
        AvailableTimeSlotResponse slot = new AvailableTimeSlotResponse();
        slot.setChefId(chef.getId());
        slot.setChefName(chef.getUser().getFullName());
        slot.setDate(date);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setDurationMinutes((int) Duration.between(startTime, endTime).toMinutes());
        
        // Only add slot if it's at least 30 minutes
        if (slot.getDurationMinutes() >= 30) {
            slots.add(slot);
        }
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