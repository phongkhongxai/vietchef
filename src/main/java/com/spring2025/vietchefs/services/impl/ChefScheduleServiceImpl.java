package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefScheduleResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ChefScheduleService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChefScheduleServiceImpl implements ChefScheduleService {

    @Autowired
    private ChefScheduleRepository chefScheduleRepository;

    @Autowired
    private ChefRepository chefRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ChefScheduleResponse getScheduleById(Long scheduleId) {
        ChefSchedule schedule = chefScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef schedule not found with id: " + scheduleId));
        return modelMapper.map(schedule, ChefScheduleResponse.class);
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

        // Kiểm tra conflict với các lịch khác của chef, loại trừ lịch hiện tại
        checkScheduleConflict(schedule.getChef(), schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(), schedule.getId());

        ChefSchedule updatedSchedule = chefScheduleRepository.save(schedule);
        return modelMapper.map(updatedSchedule, ChefScheduleResponse.class);
    }


    @Override
    public void deleteSchedule(Long scheduleId) {
        ChefSchedule schedule = chefScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef schedule not found with id: " + scheduleId));
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
        return schedules.stream()
                .map(schedule -> modelMapper.map(schedule, ChefScheduleResponse.class))
                .collect(Collectors.toList());
    }

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
}