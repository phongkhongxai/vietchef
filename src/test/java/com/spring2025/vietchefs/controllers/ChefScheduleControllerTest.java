package com.spring2025.vietchefs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.requestModel.ChefMultipleScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefScheduleResponse;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.ChefScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChefScheduleController.class)
public class ChefScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChefScheduleService chefScheduleService;
    
    // Security-related mock beans
    @MockBean
    private UserDetailsService userDetailsService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    private ChefScheduleRequest scheduleRequest;
    private ChefScheduleResponse scheduleResponse;
    private ChefScheduleUpdateRequest updateRequest;
    private ChefMultipleScheduleRequest multipleScheduleRequest;
    private List<ChefScheduleResponse> scheduleResponseList;

    @BeforeEach
    void setUp() {
        // Setup test data
        scheduleRequest = new ChefScheduleRequest();
        scheduleRequest.setDayOfWeek(1); // Monday
        scheduleRequest.setStartTime(LocalTime.of(9, 0));
        scheduleRequest.setEndTime(LocalTime.of(12, 0));

        scheduleResponse = new ChefScheduleResponse();
        scheduleResponse.setId(1L);
        scheduleResponse.setDayOfWeek(1);
        scheduleResponse.setStartTime(LocalTime.of(9, 0));
        scheduleResponse.setEndTime(LocalTime.of(12, 0));

        updateRequest = new ChefScheduleUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setDayOfWeek(2); // Tuesday
        updateRequest.setStartTime(LocalTime.of(10, 0));
        updateRequest.setEndTime(LocalTime.of(13, 0));

        ChefMultipleScheduleRequest.ScheduleTimeSlot slot1 = new ChefMultipleScheduleRequest.ScheduleTimeSlot();
        slot1.setStartTime(LocalTime.of(9, 0));
        slot1.setEndTime(LocalTime.of(12, 0));

        ChefMultipleScheduleRequest.ScheduleTimeSlot slot2 = new ChefMultipleScheduleRequest.ScheduleTimeSlot();
        slot2.setStartTime(LocalTime.of(14, 0));
        slot2.setEndTime(LocalTime.of(17, 0));

        multipleScheduleRequest = new ChefMultipleScheduleRequest();
        multipleScheduleRequest.setDayOfWeek(3); // Wednesday
        multipleScheduleRequest.setTimeSlots(Arrays.asList(slot1, slot2));

        ChefScheduleResponse response2 = new ChefScheduleResponse();
        response2.setId(2L);
        response2.setDayOfWeek(3);
        response2.setStartTime(LocalTime.of(14, 0));
        response2.setEndTime(LocalTime.of(17, 0));

        scheduleResponseList = Arrays.asList(scheduleResponse, response2);
    }

    @Test
    @DisplayName("Should create a chef schedule when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldCreateScheduleWhenAuthenticated() throws Exception {
        when(chefScheduleService.createScheduleForCurrentChef(any(ChefScheduleRequest.class))).thenReturn(scheduleResponse);

        mockMvc.perform(post("/api/v1/chef-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.dayOfWeek", is(1)))
                .andExpect(jsonPath("$.startTime", is("09:00:00")))
                .andExpect(jsonPath("$.endTime", is("12:00:00")));

        verify(chefScheduleService, times(1)).createScheduleForCurrentChef(any(ChefScheduleRequest.class));
    }

    @Test
    @DisplayName("Should create multiple chef schedules when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldCreateMultipleSchedulesWhenAuthenticated() throws Exception {
        when(chefScheduleService.createMultipleSchedulesForCurrentChef(any(ChefMultipleScheduleRequest.class)))
                .thenReturn(scheduleResponseList);

        mockMvc.perform(post("/api/v1/chef-schedules/multiple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(multipleScheduleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(chefScheduleService, times(1)).createMultipleSchedulesForCurrentChef(any(ChefMultipleScheduleRequest.class));
    }

    @Test
    @DisplayName("Should get chef schedule by ID when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldGetScheduleByIdWhenAuthenticated() throws Exception {
        when(chefScheduleService.getScheduleById(1L)).thenReturn(scheduleResponse);

        mockMvc.perform(get("/api/v1/chef-schedules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.dayOfWeek", is(1)))
                .andExpect(jsonPath("$.startTime", is("09:00:00")))
                .andExpect(jsonPath("$.endTime", is("12:00:00")));

        verify(chefScheduleService, times(1)).getScheduleById(1L);
    }

    @Test
    @DisplayName("Should get schedules for current chef when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldGetSchedulesForCurrentChefWhenAuthenticated() throws Exception {
        when(chefScheduleService.getSchedulesForCurrentChef()).thenReturn(scheduleResponseList);

        mockMvc.perform(get("/api/v1/chef-schedules/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(chefScheduleService, times(1)).getSchedulesForCurrentChef();
    }

    @Test
    @DisplayName("Should update chef schedule when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldUpdateScheduleWhenAuthenticated() throws Exception {
        ChefScheduleResponse updatedResponse = new ChefScheduleResponse();
        updatedResponse.setId(1L);
        updatedResponse.setDayOfWeek(2);
        updatedResponse.setStartTime(LocalTime.of(10, 0));
        updatedResponse.setEndTime(LocalTime.of(13, 0));

        when(chefScheduleService.updateSchedule(any(ChefScheduleUpdateRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/chef-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.dayOfWeek", is(2)))
                .andExpect(jsonPath("$.startTime", is("10:00:00")))
                .andExpect(jsonPath("$.endTime", is("13:00:00")));

        verify(chefScheduleService, times(1)).updateSchedule(any(ChefScheduleUpdateRequest.class));
    }

    @Test
    @DisplayName("Should delete chef schedule when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldDeleteScheduleWhenAuthenticated() throws Exception {
        doNothing().when(chefScheduleService).deleteSchedule(1L);

        mockMvc.perform(delete("/api/v1/chef-schedules/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Chef schedule deleted successfully"));

        verify(chefScheduleService, times(1)).deleteSchedule(1L);
    }

    @Test
    @DisplayName("Should delete schedules by day of week when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldDeleteSchedulesByDayOfWeekWhenAuthenticated() throws Exception {
        doNothing().when(chefScheduleService).deleteSchedulesByDayOfWeek(1);

        mockMvc.perform(delete("/api/v1/chef-schedules/day/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("All chef schedules for day of week 1 deleted successfully"));

        verify(chefScheduleService, times(1)).deleteSchedulesByDayOfWeek(1);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when not authenticated as CHEF")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403WhenNotChef() throws Exception {
        mockMvc.perform(get("/api/v1/chef-schedules/me"))
                .andExpect(status().isForbidden());

        verify(chefScheduleService, never()).getSchedulesForCurrentChef();
    }
} 