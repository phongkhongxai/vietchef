package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefMultipleScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefScheduleResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.ChefScheduleService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChefScheduleServiceImpl implements ChefScheduleService {

    // Các hằng số cho việc validation
    private static final LocalTime MIN_WORK_HOUR = LocalTime.of(8, 0);
    private static final LocalTime MAX_WORK_HOUR = LocalTime.of(22, 0);
    private static final int MIN_SLOT_DURATION_MINUTES = 120; // 2 giờ
    private static final int MIN_SLOT_GAP_MINUTES = 60; // 1 giờ
    private static final int MAX_SESSIONS_PER_DAY = 3;
    private static final int DAYS_TO_CHECK_FOR_BOOKINGS = 60; // Kiểm tra 60 ngày tới cho đơn hàng

    @Autowired
    private ChefScheduleRepository chefScheduleRepository;

    @Autowired
    private ChefRepository chefRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookingConflictService bookingConflictService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TimeZoneService timeZoneService;

    @Override
    public ChefScheduleResponse getScheduleById(Long scheduleId) {
        ChefSchedule schedule = chefScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef schedule not found with id: " + scheduleId));
        ChefScheduleResponse response = modelMapper.map(schedule, ChefScheduleResponse.class);
        response.setTimezone(timeZoneService.getTimezoneFromAddress(schedule.getChef().getAddress()));
        return response;
    }

    @Override
    public ChefScheduleResponse updateSchedule(ChefScheduleUpdateRequest request) {
        ChefSchedule schedule = chefScheduleRepository.findById(request.getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef schedule not found with id: " + request.getId()));

        // Nếu request cung cấp các giá trị mới, cập nhật chúng
        if (request.getDayOfWeek() != null) {
            schedule.setDayOfWeek(request.getDayOfWeek());
        }
        if (request.getStartTime() != null) {
            schedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            schedule.setEndTime(request.getEndTime());
        }

        // Validate các ràng buộc thời gian
        validateScheduleAvailableDayConstraint(request.getDayOfWeek());
        validateScheduleTimeConstraints(schedule.getStartTime(), schedule.getEndTime());

        // Kiểm tra số buổi tối đa trong ngày
        validateMaxSessionsPerDay(schedule.getChef(), schedule.getDayOfWeek(), schedule.getId());

        // Kiểm tra khoảng cách giữa các khung giờ
        validateTimeSlotGaps(schedule.getChef(), schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(), schedule.getId());

        // Kiểm tra conflict với các lịch khác của chef, loại trừ lịch hiện tại
        checkScheduleConflict(schedule.getChef(), schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(), schedule.getId());
        
        // Kiểm tra xung đột với các đơn đặt hàng hiện có (chỉ cần cho UPDATE)
        validateNoBookingConflict(schedule.getChef(), schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime());

        ChefSchedule updatedSchedule = chefScheduleRepository.save(schedule);
        return modelMapper.map(updatedSchedule, ChefScheduleResponse.class);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        ChefSchedule schedule = chefScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef schedule not found with id: " + scheduleId));
        
        // Kiểm tra xem có đơn hàng nào cho schedule này chưa
        boolean hasConflict = bookingConflictService.hasBookingConflictOnDayOfWeek(
                schedule.getChef(), schedule.getDayOfWeek(), 
                schedule.getStartTime(), schedule.getEndTime(), DAYS_TO_CHECK_FOR_BOOKINGS);
        
        if (hasConflict) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                    "Cannot delete this schedule due to existing bookings. Please check your booking calendar.");
        }
        
        schedule.setIsDeleted(true);
        chefScheduleRepository.save(schedule);
    }

    @Override
    public ChefScheduleResponse createScheduleForCurrentChef(ChefScheduleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        Chef chef = chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));

        // Validate các ràng buộc thời gian
        validateScheduleAvailableDayConstraint(request.getDayOfWeek());
        validateScheduleTimeConstraints(request.getStartTime(), request.getEndTime());

        // Kiểm tra số buổi tối đa trong ngày
        validateMaxSessionsPerDay(chef, request.getDayOfWeek(), null);

        // Kiểm tra khoảng cách giữa các khung giờ
        validateTimeSlotGaps(chef, request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), null);

        // Kiểm tra conflict trước khi tạo lịch
        checkScheduleConflict(chef, request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), null);

        ChefSchedule schedule = modelMapper.map(request, ChefSchedule.class);
        schedule.setChef(chef);
        ChefSchedule savedSchedule = chefScheduleRepository.save(schedule);
        return modelMapper.map(savedSchedule, ChefScheduleResponse.class);
    }

    @Override
    public List<ChefScheduleResponse> getSchedulesForCurrentChef() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        Chef chef = chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));
        List<ChefSchedule> schedules = chefScheduleRepository.findByChefAndIsDeletedFalse(chef);
        String timezone = timeZoneService.getTimezoneFromAddress(chef.getAddress());
        return schedules.stream()
                .map(schedule -> {
                    ChefScheduleResponse response = modelMapper.map(schedule, ChefScheduleResponse.class);
                    response.setTimezone(timezone);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void validateScheduleAvailableDayConstraint(Integer dayOfWeek) {
        if(dayOfWeek<0 || dayOfWeek>6) throw new VchefApiException(HttpStatus.BAD_REQUEST, "Day of week must between 0-6");
    }

    /**
     * Kiểm tra xung đột giữa khung giờ mới và các khung giờ hiện có
     */
    private void checkScheduleConflict(Chef chef, Integer dayOfWeek, LocalTime newStart, LocalTime newEnd, Long excludeScheduleId) {
        List<ChefSchedule> schedules = chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek);
        for (ChefSchedule existing : schedules) {
            if (excludeScheduleId == null || !existing.getId().equals(excludeScheduleId)) {
                if (newStart.isBefore(existing.getEndTime()) && newEnd.isAfter(existing.getStartTime())) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Schedule time conflicts with an existing schedule");
                }
            }
        }
    }

    /**
     * Kiểm tra các ràng buộc về thời gian: 
     * - Thời gian bắt đầu và kết thúc phải nằm trong khoảng 8:00-22:00
     * - Độ dài khung giờ phải ít nhất 2 giờ
     */
    private void validateScheduleTimeConstraints(LocalTime startTime, LocalTime endTime) {
        // Kiểm tra thời gian bắt đầu và kết thúc nằm trong khoảng cho phép
        if (startTime.isBefore(MIN_WORK_HOUR) || endTime.isAfter(MAX_WORK_HOUR)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                "Schedule time must be between " + MIN_WORK_HOUR + " and " + MAX_WORK_HOUR);
        }

        // Kiểm tra độ dài khung giờ tối thiểu (2 giờ)
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes < MIN_SLOT_DURATION_MINUTES) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                "Schedule duration must be at least " + MIN_SLOT_DURATION_MINUTES + " minutes");
        }
    }

    /**
     * Kiểm tra số lượng buổi tối đa trong một ngày
     */
    private void validateMaxSessionsPerDay(Chef chef, Integer dayOfWeek, Long excludeScheduleId) {
        List<ChefSchedule> schedules = chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek);
        
        // Nếu đang cập nhật, loại trừ lịch hiện tại
        if (excludeScheduleId != null) {
            schedules = schedules.stream()
                .filter(schedule -> !schedule.getId().equals(excludeScheduleId))
                .collect(Collectors.toList());
        }
        
        if (schedules.size() >= MAX_SESSIONS_PER_DAY) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                "Maximum number of sessions per day (" + MAX_SESSIONS_PER_DAY + ") exceeded");
        }
    }

    /**
     * Kiểm tra khoảng cách giữa các khung giờ (ít nhất 1 giờ)
     */
    private void validateTimeSlotGaps(Chef chef, Integer dayOfWeek, LocalTime newStart, LocalTime newEnd, Long excludeScheduleId) {
        List<ChefSchedule> schedules = chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek);
        
        for (ChefSchedule existing : schedules) {
            if (excludeScheduleId == null || !existing.getId().equals(excludeScheduleId)) {
                // Kiểm tra khoảng cách giữa khung giờ mới và khung giờ hiện có
                if (!newStart.isBefore(existing.getEndTime()) && 
                    Duration.between(existing.getEndTime(), newStart).toMinutes() < MIN_SLOT_GAP_MINUTES) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                        "Schedule must have at least " + MIN_SLOT_GAP_MINUTES + " minutes gap from previous schedule");
                }
                
                if (!existing.getStartTime().isBefore(newEnd) && 
                    Duration.between(newEnd, existing.getStartTime()).toMinutes() < MIN_SLOT_GAP_MINUTES) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                        "Schedule must have at least " + MIN_SLOT_GAP_MINUTES + " minutes gap from next schedule");
                }
            }
        }
    }
    
    /**
     * Kiểm tra xung đột với các đơn đặt hàng (chỉ sử dụng cho UPDATE)
     */
    private void validateNoBookingConflict(Chef chef, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        // Sử dụng BookingConflictService để kiểm tra xung đột
        boolean hasConflict = bookingConflictService.hasBookingConflictOnDayOfWeek(
                chef, dayOfWeek, startTime, endTime, DAYS_TO_CHECK_FOR_BOOKINGS);
        
        if (hasConflict) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                "Schedule conflicts with existing bookings. Please check your booking calendar and choose a different time slot.");
        }
    }

    @Override
    public List<ChefScheduleResponse> createMultipleSchedulesForCurrentChef(ChefMultipleScheduleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        Chef chef = chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));

        // Validate số lượng slots
        if (request.getTimeSlots().size() > MAX_SESSIONS_PER_DAY) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Maximum number of sessions per day (" + MAX_SESSIONS_PER_DAY + ") exceeded");
        }

        // Validate và sắp xếp time slots theo thứ tự tăng dần của startTime
        List<ChefMultipleScheduleRequest.ScheduleTimeSlot> sortedTimeSlots = request.getTimeSlots().stream()
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        // Validate từng slot
        for (ChefMultipleScheduleRequest.ScheduleTimeSlot slot : sortedTimeSlots) {
            validateScheduleTimeConstraints(slot.getStartTime(), slot.getEndTime());
        }

        // Validate khoảng cách giữa các slots
        for (int i = 0; i < sortedTimeSlots.size() - 1; i++) {
            ChefMultipleScheduleRequest.ScheduleTimeSlot current = sortedTimeSlots.get(i);
            ChefMultipleScheduleRequest.ScheduleTimeSlot next = sortedTimeSlots.get(i + 1);
            
            if (Duration.between(current.getEndTime(), next.getStartTime()).toMinutes() < MIN_SLOT_GAP_MINUTES) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST,
                        "Schedule slots must have at least " + MIN_SLOT_GAP_MINUTES + " minutes gap between them");
            }
        }

        // Validate xung đột với các lịch hiện có (chỉ check schedule conflict, không check booking/blocked date)
        for (ChefMultipleScheduleRequest.ScheduleTimeSlot slot : sortedTimeSlots) {
            checkScheduleConflict(chef, request.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(), null);
        }

        // Lưu các lịch mới
        List<ChefSchedule> savedSchedules = new ArrayList<>();
        for (ChefMultipleScheduleRequest.ScheduleTimeSlot slot : sortedTimeSlots) {
            ChefSchedule schedule = new ChefSchedule();
            schedule.setChef(chef);
            schedule.setDayOfWeek(request.getDayOfWeek());
            schedule.setStartTime(slot.getStartTime());
            schedule.setEndTime(slot.getEndTime());
            savedSchedules.add(chefScheduleRepository.save(schedule));
        }

        // Trả về kết quả
        return savedSchedules.stream()
                .map(schedule -> modelMapper.map(schedule, ChefScheduleResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSchedulesByDayOfWeek(Integer dayOfWeek) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        Chef chef = chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));

        List<ChefSchedule> schedules = chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek);
        if (schedules.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "No schedules found for day of week: " + dayOfWeek);
        }

        // Kiểm tra xem có đơn hàng nào cho ngày này chưa (giữ nguyên cho DELETE)
        boolean hasConflicts = bookingConflictService.hasActiveBookingsForDayOfWeek(chef.getId(), dayOfWeek);
        if (hasConflicts) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                    "Cannot delete schedules due to existing bookings on this day of week");
        }

        // Xóa mềm tất cả lịch
        for (ChefSchedule schedule : schedules) {
            schedule.setIsDeleted(true);
            chefScheduleRepository.save(schedule);
        }
    }
}