package com.spring2025.vietchefs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefBlockedDateResponse;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.ChefBlockedDateService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChefBlockedDateController.class)
public class ChefBlockedDateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChefBlockedDateService blockedDateService;
    
    // Security-related mock beans
    @MockBean
    private UserDetailsService userDetailsService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    private ChefBlockedDateRequest blockedDateRequest;
    private ChefBlockedDateResponse blockedDateResponse;
    private ChefBlockedDateUpdateRequest updateRequest;
    private List<ChefBlockedDateResponse> blockedDateResponseList;

    @BeforeEach
    void setUp() {
        // Setup test data
        blockedDateRequest = new ChefBlockedDateRequest();
        blockedDateRequest.setBlockedDate(LocalDate.of(2023, 12, 25));
        blockedDateRequest.setStartTime(LocalTime.of(9, 0));
        blockedDateRequest.setEndTime(LocalTime.of(18, 0));
        blockedDateRequest.setReason("Christmas Holiday");

        blockedDateResponse = new ChefBlockedDateResponse();
        blockedDateResponse.setBlockId(1L);
        blockedDateResponse.setBlockedDate(LocalDate.of(2023, 12, 25));
        blockedDateResponse.setStartTime(LocalTime.of(9, 0));
        blockedDateResponse.setEndTime(LocalTime.of(18, 0));
        blockedDateResponse.setReason("Christmas Holiday");

        updateRequest = new ChefBlockedDateUpdateRequest();
        updateRequest.setBlockId(1L);
        updateRequest.setBlockedDate(LocalDate.of(2023, 12, 25));
        updateRequest.setStartTime(LocalTime.of(10, 0));
        updateRequest.setEndTime(LocalTime.of(20, 0));
        updateRequest.setReason("Extended Christmas Holiday");

        ChefBlockedDateResponse response2 = new ChefBlockedDateResponse();
        response2.setBlockId(2L);
        response2.setBlockedDate(LocalDate.of(2024, 1, 1));
        response2.setStartTime(LocalTime.of(9, 0));
        response2.setEndTime(LocalTime.of(18, 0));
        response2.setReason("New Year's Day");

        blockedDateResponseList = Arrays.asList(blockedDateResponse, response2);
    }

    @Test
    @DisplayName("Should create a blocked date when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldCreateBlockedDateWhenAuthenticated() throws Exception {
        when(blockedDateService.createBlockedDateForCurrentChef(any(ChefBlockedDateRequest.class)))
                .thenReturn(blockedDateResponse);

        mockMvc.perform(post("/api/v1/chef-blocked-dates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedDateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockId", is(1)))
                .andExpect(jsonPath("$.blockedDate", is("2023-12-25")))
                .andExpect(jsonPath("$.startTime", is("09:00:00")))
                .andExpect(jsonPath("$.endTime", is("18:00:00")))
                .andExpect(jsonPath("$.reason", is("Christmas Holiday")));

        verify(blockedDateService, times(1)).createBlockedDateForCurrentChef(any(ChefBlockedDateRequest.class));
    }

    @Test
    @DisplayName("Should get blocked date by ID when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldGetBlockedDateByIdWhenAuthenticated() throws Exception {
        when(blockedDateService.getBlockedDateById(1L)).thenReturn(blockedDateResponse);

        mockMvc.perform(get("/api/v1/chef-blocked-dates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockId", is(1)))
                .andExpect(jsonPath("$.blockedDate", is("2023-12-25")))
                .andExpect(jsonPath("$.startTime", is("09:00:00")))
                .andExpect(jsonPath("$.endTime", is("18:00:00")))
                .andExpect(jsonPath("$.reason", is("Christmas Holiday")));

        verify(blockedDateService, times(1)).getBlockedDateById(1L);
    }

    @Test
    @DisplayName("Should get all blocked dates for current chef when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldGetAllBlockedDatesForCurrentChefWhenAuthenticated() throws Exception {
        when(blockedDateService.getBlockedDatesForCurrentChef()).thenReturn(blockedDateResponseList);

        mockMvc.perform(get("/api/v1/chef-blocked-dates/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].blockId", is(1)))
                .andExpect(jsonPath("$[0].blockedDate", is("2023-12-25")))
                .andExpect(jsonPath("$[1].blockId", is(2)))
                .andExpect(jsonPath("$[1].blockedDate", is("2024-01-01")));

        verify(blockedDateService, times(1)).getBlockedDatesForCurrentChef();
    }

    @Test
    @DisplayName("Should get blocked dates in date range when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldGetBlockedDatesInRangeWhenAuthenticated() throws Exception {
        LocalDate startDate = LocalDate.of(2023, 12, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        when(blockedDateService.getBlockedDatesForCurrentChefBetween(startDate, endDate))
                .thenReturn(blockedDateResponseList);

        mockMvc.perform(get("/api/v1/chef-blocked-dates/me/range")
                .param("startDate", "2023-12-01")
                .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].blockId", is(1)))
                .andExpect(jsonPath("$[0].blockedDate", is("2023-12-25")))
                .andExpect(jsonPath("$[1].blockId", is(2)))
                .andExpect(jsonPath("$[1].blockedDate", is("2024-01-01")));

        verify(blockedDateService, times(1)).getBlockedDatesForCurrentChefBetween(startDate, endDate);
    }

    @Test
    @DisplayName("Should get blocked dates for specific date when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldGetBlockedDatesByDateWhenAuthenticated() throws Exception {
        LocalDate date = LocalDate.of(2023, 12, 25);
        List<ChefBlockedDateResponse> singleDateResponse = List.of(blockedDateResponse);
        
        when(blockedDateService.getBlockedDatesForCurrentChefByDate(date))
                .thenReturn(singleDateResponse);

        mockMvc.perform(get("/api/v1/chef-blocked-dates/me/date")
                .param("date", "2023-12-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].blockId", is(1)))
                .andExpect(jsonPath("$[0].blockedDate", is("2023-12-25")))
                .andExpect(jsonPath("$[0].reason", is("Christmas Holiday")));

        verify(blockedDateService, times(1)).getBlockedDatesForCurrentChefByDate(date);
    }

    @Test
    @DisplayName("Should update blocked date when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldUpdateBlockedDateWhenAuthenticated() throws Exception {
        ChefBlockedDateResponse updatedResponse = new ChefBlockedDateResponse();
        updatedResponse.setBlockId(1L);
        updatedResponse.setBlockedDate(LocalDate.of(2023, 12, 25));
        updatedResponse.setStartTime(LocalTime.of(10, 0));
        updatedResponse.setEndTime(LocalTime.of(20, 0));
        updatedResponse.setReason("Extended Christmas Holiday");

        when(blockedDateService.updateBlockedDate(any(ChefBlockedDateUpdateRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/chef-blocked-dates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockId", is(1)))
                .andExpect(jsonPath("$.blockedDate", is("2023-12-25")))
                .andExpect(jsonPath("$.startTime", is("10:00:00")))
                .andExpect(jsonPath("$.endTime", is("20:00:00")))
                .andExpect(jsonPath("$.reason", is("Extended Christmas Holiday")));

        verify(blockedDateService, times(1)).updateBlockedDate(any(ChefBlockedDateUpdateRequest.class));
    }

    @Test
    @DisplayName("Should delete blocked date when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldDeleteBlockedDateWhenAuthenticated() throws Exception {
        doNothing().when(blockedDateService).deleteBlockedDate(1L);

        mockMvc.perform(delete("/api/v1/chef-blocked-dates/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Blocked date deleted successfully"));

        verify(blockedDateService, times(1)).deleteBlockedDate(1L);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when not authenticated as CHEF")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403WhenNotChef() throws Exception {
        mockMvc.perform(get("/api/v1/chef-blocked-dates/me"))
                .andExpect(status().isForbidden());

        verify(blockedDateService, never()).getBlockedDatesForCurrentChef();
    }
} 