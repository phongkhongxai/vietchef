package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefBlockedDate;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRangeRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefBlockedDateResponse;
import com.spring2025.vietchefs.repositories.ChefBlockedDateRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.ChefBlockedDateService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChefBlockedDateServiceImpl implements ChefBlockedDateService {

    // Thời gian làm việc tối thiểu/tối đa mà chef có thể chặn
    private static final LocalTime MIN_WORK_HOUR = LocalTime.of(8, 0);
    private static final LocalTime MAX_WORK_HOUR = LocalTime.of(22, 0);

    @Autowired
    private ChefBlockedDateRepository blockedDateRepository;

    @Autowired
    private ChefRepository chefRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChefScheduleRepository scheduleRepository;

    @Autowired
    private BookingConflictService bookingConflictService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ChefBlockedDateResponse getBlockedDateById(Long blockId) {
        ChefBlockedDate blockedDate = blockedDateRepository.findById(blockId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Blocked date not found with id: " + blockId));
        return modelMapper.map(blockedDate, ChefBlockedDateResponse.class);
    }

    @Override
    public ChefBlockedDateResponse updateBlockedDate(ChefBlockedDateUpdateRequest request) {
        ChefBlockedDate blockedDate = blockedDateRepository.findById(request.getBlockId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Blocked date not found with id: " + request.getBlockId()));

        // Kiểm tra xem người dùng hiện tại có phải là chef sở hữu của blocked date này không
        Chef currentChef = getCurrentChef();
        if (!blockedDate.getChef().getId().equals(currentChef.getId())) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "You do not have permission to update this blocked date");
        }

        // Cập nhật giá trị nếu được cung cấp
        if (request.getBlockedDate() != null) {
            blockedDate.setBlockedDate(request.getBlockedDate());
        }
        if (request.getStartTime() != null) {
            blockedDate.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            blockedDate.setEndTime(request.getEndTime());
        }
        if (request.getReason() != null) {
            blockedDate.setReason(request.getReason());
        }

        // Validate các ràng buộc thời gian
        validateTimeConstraints(blockedDate.getStartTime(), blockedDate.getEndTime());
        
        // Kiểm tra xung đột với các blocked date khác
        validateNoBlockedDateConflict(currentChef, blockedDate.getBlockedDate(), blockedDate.getStartTime(), blockedDate.getEndTime(), blockedDate.getBlockId());

        // Kiểm tra xung đột với các đơn đặt hàng hiện có
        validateNoBookingConflict(currentChef, blockedDate.getBlockedDate(), blockedDate.getStartTime(), blockedDate.getEndTime());

        ChefBlockedDate updatedBlockedDate = blockedDateRepository.save(blockedDate);
        return modelMapper.map(updatedBlockedDate, ChefBlockedDateResponse.class);
    }

    @Override
    public void deleteBlockedDate(Long blockId) {
        ChefBlockedDate blockedDate = blockedDateRepository.findById(blockId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Blocked date not found with id: " + blockId));

        // Kiểm tra xem người dùng hiện tại có phải là chef sở hữu của blocked date này không
        Chef currentChef = getCurrentChef();
        if (!blockedDate.getChef().getId().equals(currentChef.getId())) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "You do not have permission to delete this blocked date");
        }

        blockedDate.setIsDeleted(true);
        blockedDateRepository.save(blockedDate);
    }

    @Override
    public ChefBlockedDateResponse createBlockedDateForCurrentChef(ChefBlockedDateRequest request) {
        Chef chef = getCurrentChef();

        // Validate các ràng buộc thời gian
        validateTimeConstraints(request.getStartTime(), request.getEndTime());
        
        // Kiểm tra xung đột với các blocked date khác
        validateNoBlockedDateConflict(chef, request.getBlockedDate(), request.getStartTime(), request.getEndTime(), null);
        
        // Kiểm tra xung đột với các đơn đặt hàng hiện có
        validateNoBookingConflict(chef, request.getBlockedDate(), request.getStartTime(), request.getEndTime());

        ChefBlockedDate blockedDate = new ChefBlockedDate();
        blockedDate.setChef(chef);
        blockedDate.setBlockedDate(request.getBlockedDate());
        blockedDate.setStartTime(request.getStartTime());
        blockedDate.setEndTime(request.getEndTime());
        blockedDate.setReason(request.getReason());
        blockedDate.setIsDeleted(false);

        ChefBlockedDate savedBlockedDate = blockedDateRepository.save(blockedDate);
        return modelMapper.map(savedBlockedDate, ChefBlockedDateResponse.class);
    }

    @Override
    public List<ChefBlockedDateResponse> getBlockedDatesForCurrentChef() {
        Chef chef = getCurrentChef();
        List<ChefBlockedDate> blockedDates = blockedDateRepository.findByChefAndIsDeletedFalse(chef);
        return blockedDates.stream()
                .map(date -> modelMapper.map(date, ChefBlockedDateResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChefBlockedDateResponse> getBlockedDatesForCurrentChefBetween(LocalDate startDate, LocalDate endDate) {
        Chef chef = getCurrentChef();
        List<ChefBlockedDate> blockedDates = blockedDateRepository.findByChefAndBlockedDateBetweenAndIsDeletedFalse(chef, startDate, endDate);
        return blockedDates.stream()
                .map(date -> modelMapper.map(date, ChefBlockedDateResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChefBlockedDateResponse> getBlockedDatesForCurrentChefByDate(LocalDate date) {
        Chef chef = getCurrentChef();
        List<ChefBlockedDate> blockedDates = blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date);
        return blockedDates.stream()
                .map(date1 -> modelMapper.map(date1, ChefBlockedDateResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChefBlockedDateResponse> createBlockedDateRangeForCurrentChef(ChefBlockedDateRangeRequest request) {
        Chef chef = getCurrentChef();
        
        // Validate start and end times are within working hours
        validateTimeConstraints(request.getStartTime(), request.getEndTime());
        
        // Validate date range
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Start date must be before or equal to end date");
        }
        
        List<ChefBlockedDateResponse> createdBlockedDates = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        
        // Iterate through each date in the range
        while (!currentDate.isAfter(request.getEndDate())) {
            // Determine start and end time for current date
            LocalTime blockStartTime;
            LocalTime blockEndTime;
            
            // First day: use the requested start time
            if (currentDate.isEqual(request.getStartDate())) {
                blockStartTime = request.getStartTime();
                // If it's a single day request, use requested end time
                if (currentDate.isEqual(request.getEndDate())) {
                    blockEndTime = request.getEndTime();
                } else {
                    blockEndTime = MAX_WORK_HOUR;
                }
            } 
            // Last day: use the requested end time
            else if (currentDate.isEqual(request.getEndDate())) {
                blockStartTime = MIN_WORK_HOUR;
                blockEndTime = request.getEndTime();
            }
            // Middle days: block the full working day
            else {
                blockStartTime = MIN_WORK_HOUR;
                blockEndTime = MAX_WORK_HOUR;
            }
            
            // Validate no conflicts for each date
            validateNoBlockedDateConflict(chef, currentDate, blockStartTime, blockEndTime, null);
            validateNoBookingConflict(chef, currentDate, blockStartTime, blockEndTime);
            
            // Create blocked date for current date
            ChefBlockedDate blockedDate = new ChefBlockedDate();
            blockedDate.setChef(chef);
            blockedDate.setBlockedDate(currentDate);
            blockedDate.setStartTime(blockStartTime);
            blockedDate.setEndTime(blockEndTime);
            blockedDate.setReason(request.getReason());
            blockedDate.setIsDeleted(false);
            
            ChefBlockedDate savedBlockedDate = blockedDateRepository.save(blockedDate);
            createdBlockedDates.add(modelMapper.map(savedBlockedDate, ChefBlockedDateResponse.class));
            
            // Move to next date
            currentDate = currentDate.plusDays(1);
        }
        
        return createdBlockedDates;
    }

    /**
     * Lấy chef từ người dùng hiện tại
     */
    private Chef getCurrentChef() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        return chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));
    }

    /**
     * Kiểm tra các ràng buộc về thời gian:
     * - Thời gian bắt đầu và kết thúc phải nằm trong khoảng 8:00-22:00
     * - Thời gian kết thúc phải sau thời gian bắt đầu
     */
    private void validateTimeConstraints(LocalTime startTime, LocalTime endTime) {
        // Kiểm tra thời gian bắt đầu và kết thúc nằm trong khoảng cho phép
        if (startTime.isBefore(MIN_WORK_HOUR) || endTime.isAfter(MAX_WORK_HOUR)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                "Blocked time must be between " + MIN_WORK_HOUR + " and " + MAX_WORK_HOUR);
        }

        // Kiểm tra thời gian kết thúc phải sau thời gian bắt đầu
        if (!startTime.isBefore(endTime)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    /**
     * Kiểm tra xung đột với các đơn đặt hàng
     */
    private void validateNoBookingConflict(Chef chef, LocalDate blockedDate, LocalTime startTime, LocalTime endTime) {
        boolean hasConflict = bookingConflictService.hasBookingConflict(chef, blockedDate, startTime, endTime);
        
        if (hasConflict) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                "Cannot block this date and time as you have existing bookings. Please check your booking calendar.");
        }
    }
    

    /**
     * Kiểm tra xung đột với các blocked date khác
     * @param chef Chef cần kiểm tra
     * @param blockedDate Ngày bị chặn
     * @param startTime Thời gian bắt đầu
     * @param endTime Thời gian kết thúc
     * @param excludeId ID của blocked date cần loại trừ (dùng khi update)
     */
    private void validateNoBlockedDateConflict(Chef chef, LocalDate blockedDate, LocalTime startTime, LocalTime endTime, Long excludeId) {
        // Lấy danh sách các blocked date trong ngày đó của chef
        List<ChefBlockedDate> existingBlockedDates = blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, blockedDate);
        
        for (ChefBlockedDate existing : existingBlockedDates) {
            // Bỏ qua nếu là blocked date đang cập nhật
            if (excludeId != null && existing.getBlockId().equals(excludeId)) {
                continue;
            }
            
            // Kiểm tra xem có chồng chéo không
            if (isTimeOverlap(startTime, endTime, existing.getStartTime(), existing.getEndTime())) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, 
                    "Time slot conflicts with an existing blocked date at " + existing.getStartTime() + " - " + existing.getEndTime());
            }
        }
    }
    
    /**
     * Kiểm tra xem hai khoảng thời gian có chồng chéo nhau không
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        // Hai khoảng thời gian chồng chéo nếu:
        // 1. Thời điểm bắt đầu của khung 1 < thời điểm kết thúc của khung 2 VÀ
        // 2. Thời điểm kết thúc của khung 1 > thời điểm bắt đầu của khung 2
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
} 