package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefScheduleResponse;
import com.spring2025.vietchefs.repositories.ChefBlockedDateRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChefScheduleServiceImplTest {

    @Mock
    private ChefScheduleRepository chefScheduleRepository;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChefBlockedDateRepository chefBlockedDateRepository;

    @Mock
    private BookingConflictService bookingConflictService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ChefScheduleServiceImpl chefScheduleService;

    private User testUser;
    private Chef testChef;
    private ChefSchedule testSchedule;
    private ChefScheduleRequest scheduleRequest;
    private ChefScheduleUpdateRequest updateRequest;
    private ChefScheduleResponse scheduleResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test Chef User");

        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);

        testSchedule = new ChefSchedule();
        testSchedule.setId(1L);
        testSchedule.setChef(testChef);
        testSchedule.setDayOfWeek(1); // Monday
        testSchedule.setStartTime(LocalTime.of(10, 0)); // 10:00 AM
        testSchedule.setEndTime(LocalTime.of(14, 0)); // 2:00 PM
        testSchedule.setIsDeleted(false);

        scheduleRequest = new ChefScheduleRequest();
        scheduleRequest.setDayOfWeek(2); // Tuesday
        scheduleRequest.setStartTime(LocalTime.of(12, 0)); // 12:00 PM
        scheduleRequest.setEndTime(LocalTime.of(15, 0)); // 3:00 PM

        updateRequest = new ChefScheduleUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setDayOfWeek(1); // Monday
        updateRequest.setStartTime(LocalTime.of(11, 0)); // 11:00 AM
        updateRequest.setEndTime(LocalTime.of(15, 0)); // 3:00 PM

        scheduleResponse = new ChefScheduleResponse();
        scheduleResponse.setId(1L);
        scheduleResponse.setDayOfWeek(1);
        scheduleResponse.setStartTime(LocalTime.of(10, 0));
        scheduleResponse.setEndTime(LocalTime.of(14, 0));
    }

    @Test
    void getScheduleById_ShouldReturnSchedule_WhenScheduleExists() {
        // Arrange
        when(chefScheduleRepository.findById(anyLong())).thenReturn(Optional.of(testSchedule));
        when(modelMapper.map(any(ChefSchedule.class), eq(ChefScheduleResponse.class))).thenReturn(scheduleResponse);

        // Act
        ChefScheduleResponse result = chefScheduleService.getScheduleById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(scheduleResponse.getId(), result.getId());
        assertEquals(scheduleResponse.getDayOfWeek(), result.getDayOfWeek());
        verify(chefScheduleRepository).findById(1L);
    }

    @Test
    void getScheduleById_ShouldThrowException_WhenScheduleNotFound() {
        // Arrange
        when(chefScheduleRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.getScheduleById(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateSchedule_ShouldReturnUpdatedSchedule_WhenValid() {
        // Arrange
        when(chefScheduleRepository.findById(anyLong())).thenReturn(Optional.of(testSchedule));
        when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                .thenReturn(List.of());
        when(chefScheduleRepository.save(any(ChefSchedule.class))).thenReturn(testSchedule);
        when(modelMapper.map(any(ChefSchedule.class), eq(ChefScheduleResponse.class))).thenReturn(scheduleResponse);
        lenient().when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);

        // Act
        ChefScheduleResponse result = chefScheduleService.updateSchedule(updateRequest);

        // Assert
        assertNotNull(result);
        verify(chefScheduleRepository).save(any(ChefSchedule.class));
    }

    @Test
    void updateSchedule_ShouldThrowException_WhenScheduleNotFound() {
        // Arrange
        when(chefScheduleRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.updateSchedule(updateRequest);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void deleteSchedule_ShouldMarkScheduleAsDeleted() {
        // Arrange
        when(chefScheduleRepository.findById(anyLong())).thenReturn(Optional.of(testSchedule));
        when(chefScheduleRepository.save(any(ChefSchedule.class))).thenReturn(testSchedule);

        // Act
        chefScheduleService.deleteSchedule(1L);

        // Assert
        assertTrue(testSchedule.getIsDeleted());
        verify(chefScheduleRepository).save(testSchedule);
    }

    @Test
    void deleteSchedule_ShouldThrowException_WhenScheduleNotFound() {
        // Arrange
        when(chefScheduleRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.deleteSchedule(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void createScheduleForCurrentChef_ShouldReturnCreatedSchedule_WhenValid() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                    .thenReturn(List.of());
            when(chefScheduleRepository.save(any(ChefSchedule.class))).thenReturn(testSchedule);
            when(modelMapper.map(any(ChefScheduleRequest.class), eq(ChefSchedule.class))).thenReturn(testSchedule);
            when(modelMapper.map(any(ChefSchedule.class), eq(ChefScheduleResponse.class))).thenReturn(scheduleResponse);
            lenient().when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);

            // Act
            ChefScheduleResponse result = chefScheduleService.createScheduleForCurrentChef(scheduleRequest);

            // Assert
            assertNotNull(result);
            verify(chefScheduleRepository).save(any(ChefSchedule.class));
        }
    }

    @Test
    void createScheduleForCurrentChef_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.createScheduleForCurrentChef(scheduleRequest);
            });
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }

    @Test
    void createScheduleForCurrentChef_ShouldThrowException_WhenChefNotFound() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.empty());

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.createScheduleForCurrentChef(scheduleRequest);
            });
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }

    @Test
    void getSchedulesForCurrentChef_ShouldReturnScheduleList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(chefScheduleRepository.findByChefAndIsDeletedFalse(any(Chef.class)))
                    .thenReturn(Arrays.asList(testSchedule));
            when(modelMapper.map(any(ChefSchedule.class), eq(ChefScheduleResponse.class))).thenReturn(scheduleResponse);

            // Act
            List<ChefScheduleResponse> result = chefScheduleService.getSchedulesForCurrentChef();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(chefScheduleRepository).findByChefAndIsDeletedFalse(testChef);
        }
    }

    @Test
    void getSchedulesForCurrentChef_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.getSchedulesForCurrentChef();
            });
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }
}