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
        if (request.getDayOfWeek() != null) {
            schedule.setDayOfWeek(request.getDayOfWeek());
        }
        if (request.getStartTime() != null) {
            schedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            schedule.setEndTime(request.getEndTime());
        }
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
        // Lấy userId của người dùng hiện tại
        Long userId = SecurityUtils.getCurrentUserId();

        // Lấy User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        // Lấy hồ sơ Chef của user đó
        Chef chef = chefRepository.findByUser(user)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef profile not found for user id: " + userId));

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
}