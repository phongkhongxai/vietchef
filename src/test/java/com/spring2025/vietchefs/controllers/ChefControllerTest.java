package com.spring2025.vietchefs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefsResponse;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ChefControllerTest {

    @Mock
    private ChefService chefService;

    @InjectMocks
    private ChefController chefController;

    private MockMvc mockMvc;
    private ChefsResponse chefsResponse;
    private List<ChefResponseDto> chefList;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(chefController).build();
        objectMapper = new ObjectMapper();

        // Setup test data
        chefList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ChefResponseDto chef = new ChefResponseDto();
            chef.setId((long) i);
            chef.setBio("Chef Bio " + i);
            chef.setDescription("Chef Description " + i);
            chef.setStatus("ACTIVE");
            chef.setAverageRating(BigDecimal.valueOf(i + 2.5)); // Different ratings for each chef
            chefList.add(chef);
        }

        chefsResponse = new ChefsResponse();
        chefsResponse.setContent(chefList);
        chefsResponse.setPageNo(0);
        chefsResponse.setPageSize(10);
        chefsResponse.setTotalElements(3);
        chefsResponse.setTotalPages(1);
        chefsResponse.setLast(true);
    }

    // @Test
    // public void getAllChefs_ShouldReturnChefsList() throws Exception {
    //     // Arrange
    //     when(chefService.getAllChefs(anyInt(), anyInt(), anyString(), anyString())).thenReturn(chefsResponse);

    //     // Act & Assert
    //     MvcResult result = mockMvc.perform(get("/api/v1/chefs")
    //             .contentType(MediaType.APPLICATION_JSON))
    //             .andDo(print()) // Print the response for debugging
    //             .andExpect(status().isOk())
    //             .andReturn();
        
    //     // Get the response as string for verification
    //     String responseBody = result.getResponse().getContentAsString();
    //     System.out.println("Response body: " + responseBody);
        
    //     // Just check if the response contains expected content 
    //     // instead of parsing it with Jackson
    //     assertTrue(responseBody.contains("\"content\""));
    //     assertTrue(responseBody.contains("\"pageNo\":0"));
    //     assertTrue(responseBody.contains("\"totalElements\":3"));
        
    //     // Verify service call with default parameters
    //     verify(chefService).getAllChefs(
    //             Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER), 
    //             Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE), 
    //             AppConstants.DEFAULT_SORT_BY, 
    //             AppConstants.DEFAULT_SORT_DIRECTION);
    // }

    @Test
    public void getAllChefs_WithSortByRating_ShouldUseCorrectParameters() throws Exception {
        // Arrange
        when(chefService.getAllChefs(anyInt(), anyInt(), anyString(), anyString())).thenReturn(chefsResponse);

        // Act
        mockMvc.perform(get("/api/v1/chefs")
                .param("sortBy", "rating")
                .param("sortDir", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify service call with rating sort parameters
        verify(chefService).getAllChefs(
                Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER), 
                Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE), 
                "rating", 
                "desc");
    }

    @Test
    public void getAllChefs_WithRatingDescConstant_ShouldUseCorrectParameters() throws Exception {
        // Arrange
        when(chefService.getAllChefs(anyInt(), anyInt(), anyString(), anyString())).thenReturn(chefsResponse);

        // Act
        mockMvc.perform(get("/api/v1/chefs")
                .param("sortDir", AppConstants.DEFAULT_SORT_RATING_DESC)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify service call with rating_desc constant
        verify(chefService).getAllChefs(
                Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER), 
                Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE), 
                AppConstants.DEFAULT_SORT_BY, 
                AppConstants.DEFAULT_SORT_RATING_DESC);
    }

    // @Test
    // public void getAllChefsNearBy_ShouldReturnNearbyChefsWithCorrectParameters() throws Exception {
    //     // Arrange
    //     double lat = 10.0;
    //     double lng = 106.0;
    //     double distance = 10.0;
    //     when(chefService.getAllChefsNearBy(anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(chefsResponse);

    //     // Act
    //     MvcResult result = mockMvc.perform(get("/api/v1/chefs/nearby")
    //             .param("customerLat", String.valueOf(lat))
    //             .param("customerLng", String.valueOf(lng))
    //             .param("distance", String.valueOf(distance))
    //             .param("sortDir", AppConstants.DEFAULT_SORT_RATING_DESC)
    //             .contentType(MediaType.APPLICATION_JSON))
    //             .andDo(print()) // Print the response for debugging
    //             .andExpect(status().isOk())
    //             .andReturn();
                
    //     // Get the response as string for verification
    //     String responseBody = result.getResponse().getContentAsString();
        
    //     // Just check if the response contains expected content
    //     assertTrue(responseBody.contains("\"content\""));
    //     assertTrue(responseBody.contains("\"pageNo\":0"));
    //     assertTrue(responseBody.contains("\"totalElements\":3"));

    //     // Verify service call with correct parameters
    //     verify(chefService).getAllChefsNearBy(
    //             lat,
    //             lng,
    //             distance,
    //             Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER), 
    //             Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE), 
    //             AppConstants.DEFAULT_SORT_BY, 
    //             AppConstants.DEFAULT_SORT_RATING_DESC);
    // }
} 