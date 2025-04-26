package com.spring2025.vietchefs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.dto.FavoriteChefDto;
import com.spring2025.vietchefs.models.payload.responseModel.FavoriteChefsResponse;
import com.spring2025.vietchefs.services.FavoriteChefService;
import com.spring2025.vietchefs.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteChefControllerTest {

    @Mock
    private FavoriteChefService favoriteChefService;

    @InjectMocks
    private FavoriteChefController favoriteChefController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private FavoriteChefDto favoriteChefDto;
    private FavoriteChefsResponse favoriteChefsResponse;
    private final Long userId = 1L;
    private final Long chefId = 2L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(favoriteChefController).build();
        objectMapper = new ObjectMapper();

        // Setup test data
        favoriteChefDto = new FavoriteChefDto();
        favoriteChefDto.setId(1L);
        favoriteChefDto.setUserId(userId);
        favoriteChefDto.setChefId(chefId);
        favoriteChefDto.setChefName("Test Chef");
        favoriteChefDto.setChefAvatar("test-avatar.jpg");
        favoriteChefDto.setChefSpecialization("Vietnamese Cuisine");
        favoriteChefDto.setChefAddress("123 Test Street");
        favoriteChefDto.setCreatedAt(LocalDateTime.now());

        List<FavoriteChefDto> favoriteChefDtoList = new ArrayList<>();
        favoriteChefDtoList.add(favoriteChefDto);

        favoriteChefsResponse = new FavoriteChefsResponse();
        favoriteChefsResponse.setContent(favoriteChefDtoList);
        favoriteChefsResponse.setPageNo(0);
        favoriteChefsResponse.setPageSize(10);
        favoriteChefsResponse.setTotalElements(1L);
        favoriteChefsResponse.setTotalPages(1);
        favoriteChefsResponse.setLast(true);
    }

    @Test
    void addFavoriteChef_Success() throws Exception {
        // Arrange
        when(favoriteChefService.addFavoriteChef(anyLong(), anyLong())).thenReturn(favoriteChefDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(favoriteChefDto.getId()))
                .andExpect(jsonPath("$.userId").value(favoriteChefDto.getUserId()))
                .andExpect(jsonPath("$.chefId").value(favoriteChefDto.getChefId()))
                .andExpect(jsonPath("$.chefName").value(favoriteChefDto.getChefName()));

        verify(favoriteChefService).addFavoriteChef(userId, chefId);
    }

    @Test
    void removeFavoriteChef_Success() throws Exception {
        // Arrange
        doNothing().when(favoriteChefService).removeFavoriteChef(anyLong(), anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Chef removed from favorites successfully"));

        verify(favoriteChefService).removeFavoriteChef(userId, chefId);
    }

    @Test
    void getFavoriteChefs_Success() throws Exception {
        // Arrange
        when(favoriteChefService.getFavoriteChefs(anyLong(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(favoriteChefsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/favorite-chefs/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(favoriteChefDto.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(favoriteChefDto.getUserId()))
                .andExpect(jsonPath("$.content[0].chefId").value(favoriteChefDto.getChefId()))
                .andExpect(jsonPath("$.pageNo").value(favoriteChefsResponse.getPageNo()))
                .andExpect(jsonPath("$.totalElements").value(favoriteChefsResponse.getTotalElements()));

        verify(favoriteChefService).getFavoriteChefs(
                eq(userId),
                eq(Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER)),
                eq(Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE)),
                eq("id"),
                eq("asc"));
    }

    @Test
    void isChefFavorite_ReturnTrue() throws Exception {
        // Arrange
        when(favoriteChefService.isChefFavorite(anyLong(), anyLong())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(favoriteChefService).isChefFavorite(userId, chefId);
    }

    @Test
    void isChefFavorite_ReturnFalse() throws Exception {
        // Arrange
        when(favoriteChefService.isChefFavorite(anyLong(), anyLong())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(favoriteChefService).isChefFavorite(userId, chefId);
    }
} 