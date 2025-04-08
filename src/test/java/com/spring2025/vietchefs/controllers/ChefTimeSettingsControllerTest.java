package com.spring2025.vietchefs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.requestModel.ChefTimeSettingsRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefTimeSettingsResponse;
import com.spring2025.vietchefs.repositories.AccessTokenRepository;
import com.spring2025.vietchefs.repositories.RefreshTokenRepository;
import com.spring2025.vietchefs.security.JwtAuthenticationFilter;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import com.spring2025.vietchefs.services.ChefTimeSettingsService;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChefTimeSettingsController.class)
public class ChefTimeSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChefTimeSettingsService timeSettingsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockBean
    private UserDetailsService userDetailsService;
    
    @MockBean
    private AccessTokenRepository accessTokenRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    private ChefTimeSettingsRequest timeSettingsRequest;
    private ChefTimeSettingsResponse timeSettingsResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        timeSettingsRequest = new ChefTimeSettingsRequest();
        timeSettingsRequest.setStandardPrepTime(30);
        timeSettingsRequest.setStandardCleanupTime(30);
        timeSettingsRequest.setTravelBufferPercentage(20);
        timeSettingsRequest.setCookingEfficiencyFactor(new BigDecimal("0.8"));
        timeSettingsRequest.setMinBookingNoticeHours(24);
        timeSettingsRequest.setMaxBookingDaysAhead(30);
        timeSettingsRequest.setMaxDishesPerSession(10);
        timeSettingsRequest.setMaxGuestsPerSession(20);
        timeSettingsRequest.setServiceRadiusKm(15);
        timeSettingsRequest.setMaxSessionsPerDay(3);

        timeSettingsResponse = new ChefTimeSettingsResponse();
        timeSettingsResponse.setSettingId(1L);
        timeSettingsResponse.setStandardPrepTime(30);
        timeSettingsResponse.setStandardCleanupTime(30);
        timeSettingsResponse.setTravelBufferPercentage(20);
        timeSettingsResponse.setCookingEfficiencyFactor(new BigDecimal("0.8"));
        timeSettingsResponse.setMinBookingNoticeHours(24);
        timeSettingsResponse.setMaxBookingDaysAhead(30);
        timeSettingsResponse.setMaxDishesPerSession(10);
        timeSettingsResponse.setMaxGuestsPerSession(20);
        timeSettingsResponse.setServiceRadiusKm(15);
        timeSettingsResponse.setMaxSessionsPerDay(3);
    }

    @Test
    @DisplayName("Should get time settings for current chef when authenticated as CHEF")
    @WithMockUser(roles = {"CHEF"})
    void shouldGetTimeSettingsForCurrentChefWhenAuthenticated() throws Exception {
        when(timeSettingsService.getTimeSettingsForCurrentChef()).thenReturn(timeSettingsResponse);
        

        mockMvc.perform(get("/api/v1/chef-time-settings/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingId", is(1)))
                .andExpect(jsonPath("$.standardPrepTime", is(30)))
                .andExpect(jsonPath("$.standardCleanupTime", is(30)))
                .andExpect(jsonPath("$.travelBufferPercentage", is(20)))
                .andExpect(jsonPath("$.cookingEfficiencyFactor", is(0.8)))
                .andExpect(jsonPath("$.minBookingNoticeHours", is(24)))
                .andExpect(jsonPath("$.maxBookingDaysAhead", is(30)))
                .andExpect(jsonPath("$.maxDishesPerSession", is(10)))
                .andExpect(jsonPath("$.maxGuestsPerSession", is(20)))
                .andExpect(jsonPath("$.serviceRadiusKm", is(15)))
                .andExpect(jsonPath("$.maxSessionsPerDay", is(3)));

        verify(timeSettingsService, times(1)).getTimeSettingsForCurrentChef();
    }

    @Test
    @DisplayName("Should update time settings for current chef when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldUpdateTimeSettingsForCurrentChefWhenAuthenticated() throws Exception {
        when(timeSettingsService.updateTimeSettingsForCurrentChef(any(ChefTimeSettingsRequest.class)))
                .thenReturn(timeSettingsResponse);

        mockMvc.perform(put("/api/v1/chef-time-settings/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(timeSettingsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingId", is(1)))
                .andExpect(jsonPath("$.standardPrepTime", is(30)))
                .andExpect(jsonPath("$.standardCleanupTime", is(30)))
                .andExpect(jsonPath("$.travelBufferPercentage", is(20)))
                .andExpect(jsonPath("$.cookingEfficiencyFactor", is(0.8)))
                .andExpect(jsonPath("$.minBookingNoticeHours", is(24)))
                .andExpect(jsonPath("$.maxBookingDaysAhead", is(30)))
                .andExpect(jsonPath("$.maxDishesPerSession", is(10)))
                .andExpect(jsonPath("$.maxGuestsPerSession", is(20)))
                .andExpect(jsonPath("$.serviceRadiusKm", is(15)))
                .andExpect(jsonPath("$.maxSessionsPerDay", is(3)));

        verify(timeSettingsService, times(1)).updateTimeSettingsForCurrentChef(any(ChefTimeSettingsRequest.class));
    }

    @Test
    @DisplayName("Should reset time settings to default when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldResetTimeSettingsToDefaultWhenAuthenticated() throws Exception {
        when(timeSettingsService.resetTimeSettingsToDefault()).thenReturn(timeSettingsResponse);

        mockMvc.perform(post("/api/v1/chef-time-settings/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingId", is(1)))
                .andExpect(jsonPath("$.standardPrepTime", is(30)))
                .andExpect(jsonPath("$.standardCleanupTime", is(30)))
                .andExpect(jsonPath("$.travelBufferPercentage", is(20)))
                .andExpect(jsonPath("$.cookingEfficiencyFactor", is(0.8)))
                .andExpect(jsonPath("$.minBookingNoticeHours", is(24)))
                .andExpect(jsonPath("$.maxBookingDaysAhead", is(30)))
                .andExpect(jsonPath("$.maxDishesPerSession", is(10)))
                .andExpect(jsonPath("$.maxGuestsPerSession", is(20)))
                .andExpect(jsonPath("$.serviceRadiusKm", is(15)))
                .andExpect(jsonPath("$.maxSessionsPerDay", is(3)));

        verify(timeSettingsService, times(1)).resetTimeSettingsToDefault();
    }

    @Test
    @DisplayName("Should get time settings by ID when authenticated as CHEF")
    @WithMockUser(roles = "CHEF")
    void shouldGetTimeSettingsByIdWhenAuthenticated() throws Exception {
        when(timeSettingsService.getTimeSettingsById(1L)).thenReturn(timeSettingsResponse);

        mockMvc.perform(get("/api/v1/chef-time-settings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingId", is(1)))
                .andExpect(jsonPath("$.standardPrepTime", is(30)))
                .andExpect(jsonPath("$.standardCleanupTime", is(30)))
                .andExpect(jsonPath("$.travelBufferPercentage", is(20)))
                .andExpect(jsonPath("$.cookingEfficiencyFactor", is(0.8)))
                .andExpect(jsonPath("$.minBookingNoticeHours", is(24)))
                .andExpect(jsonPath("$.maxBookingDaysAhead", is(30)))
                .andExpect(jsonPath("$.maxDishesPerSession", is(10)))
                .andExpect(jsonPath("$.maxGuestsPerSession", is(20)))
                .andExpect(jsonPath("$.serviceRadiusKm", is(15)))
                .andExpect(jsonPath("$.maxSessionsPerDay", is(3)));

        verify(timeSettingsService, times(1)).getTimeSettingsById(1L);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when not authenticated as CHEF")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403WhenNotChef() throws Exception {
        mockMvc.perform(get("/api/v1/chef-time-settings/me"))
                .andExpect(status().isForbidden());

        verify(timeSettingsService, never()).getTimeSettingsForCurrentChef();
    }
} 