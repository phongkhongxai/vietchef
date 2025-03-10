package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefScheduleResponse;
import java.util.List;

public interface ChefScheduleService {
    // Phương thức cũ: dựa theo scheduleId
    ChefScheduleResponse getScheduleById(Long scheduleId);

    // Phương thức cập nhật lịch
    ChefScheduleResponse updateSchedule(ChefScheduleUpdateRequest request);

    // Phương thức xóa mềm lịch
    void deleteSchedule(Long scheduleId);

    // Phương thức dành cho chef hiện tại:
    ChefScheduleResponse createScheduleForCurrentChef(ChefScheduleRequest request);
    List<ChefScheduleResponse> getSchedulesForCurrentChef();
}