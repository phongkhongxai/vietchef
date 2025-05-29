package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefBlockedDate;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefMultipleScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefScheduleUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefScheduleResponse;
import com.spring2025.vietchefs.repositories.ChefBlockedDateRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.impl.ChefScheduleServiceImpl;
import com.spring2025.vietchefs.services.impl.TimeZoneService;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChefScheduleServiceTest {

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

    @Mock
    private TimeZoneService timeZoneService;

    @InjectMocks
    private ChefScheduleServiceImpl chefScheduleService;

    private User testUser;
    private Chef testChef;
    private ChefSchedule testSchedule1;
    private ChefSchedule testSchedule2;
    private ChefScheduleRequest testScheduleRequest;
    private ChefScheduleUpdateRequest testUpdateRequest;
    private ChefMultipleScheduleRequest testMultipleScheduleRequest;
    private ChefScheduleResponse testScheduleResponse1;
    private ChefScheduleResponse testScheduleResponse2;
    private ChefBlockedDate testBlockedDate;

    @BeforeEach
    void setUp() {
        // Setup mocks for lenient behavior for all tests
        Mockito.lenient().when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                .thenReturn(Collections.emptyList());
        Mockito.lenient().when(timeZoneService.getTimezoneFromAddress(anyString())).thenReturn("Asia/Ho_Chi_Minh");
        Mockito.lenient().when(bookingConflictService.hasBookingConflictOnDayOfWeek(any(Chef.class), anyInt(), any(LocalTime.class), any(LocalTime.class), anyInt())).thenReturn(false);
        Mockito.lenient().when(bookingConflictService.hasActiveBookingsForDayOfWeek(anyLong(), anyInt())).thenReturn(false);
                
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testchef");
        testUser.setEmail("testchef@example.com");

        // Setup test chef
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);
        testChef.setAddress("123 Test Street, Ho Chi Minh City, Vietnam");
        
        // Setup test schedule 1
        testSchedule1 = new ChefSchedule();
        testSchedule1.setId(1L);
        testSchedule1.setChef(testChef);
        testSchedule1.setDayOfWeek(1); // Monday
        testSchedule1.setStartTime(LocalTime.of(8, 0));
        testSchedule1.setEndTime(LocalTime.of(12, 0));
        testSchedule1.setIsDeleted(false);
        
        // Setup test schedule 2
        testSchedule2 = new ChefSchedule();
        testSchedule2.setId(2L);
        testSchedule2.setChef(testChef);
        testSchedule2.setDayOfWeek(2); // Tuesday
        testSchedule2.setStartTime(LocalTime.of(14, 0));
        testSchedule2.setEndTime(LocalTime.of(18, 0));
        testSchedule2.setIsDeleted(false);
        
        // Setup test schedule response 1
        testScheduleResponse1 = new ChefScheduleResponse();
        testScheduleResponse1.setId(1L);
        testScheduleResponse1.setDayOfWeek(1);
        testScheduleResponse1.setStartTime(LocalTime.of(8, 0));
        testScheduleResponse1.setEndTime(LocalTime.of(12, 0));
        testScheduleResponse1.setTimezone("Asia/Ho_Chi_Minh");
        
        // Setup test schedule response 2
        testScheduleResponse2 = new ChefScheduleResponse();
        testScheduleResponse2.setId(2L);
        testScheduleResponse2.setDayOfWeek(2);
        testScheduleResponse2.setStartTime(LocalTime.of(14, 0));
        testScheduleResponse2.setEndTime(LocalTime.of(18, 0));
        testScheduleResponse2.setTimezone("Asia/Ho_Chi_Minh");
        
        // Setup schedule request
        testScheduleRequest = new ChefScheduleRequest();
        testScheduleRequest.setDayOfWeek(1);
        testScheduleRequest.setStartTime(LocalTime.of(8, 0));
        testScheduleRequest.setEndTime(LocalTime.of(12, 0));
        
        // Setup update request
        testUpdateRequest = new ChefScheduleUpdateRequest();
        testUpdateRequest.setId(1L);
        testUpdateRequest.setDayOfWeek(1);
        testUpdateRequest.setStartTime(LocalTime.of(9, 0));
        testUpdateRequest.setEndTime(LocalTime.of(13, 0));
        
        // Setup multiple schedule request
        testMultipleScheduleRequest = new ChefMultipleScheduleRequest();
        testMultipleScheduleRequest.setDayOfWeek(1);
        
        // Setup time slots
        List<ChefMultipleScheduleRequest.ScheduleTimeSlot> timeSlots = new ArrayList<>();
        
        // Time slot 1
        ChefMultipleScheduleRequest.ScheduleTimeSlot timeSlot1 = new ChefMultipleScheduleRequest.ScheduleTimeSlot();
        timeSlot1.setStartTime(LocalTime.of(8, 0));
        timeSlot1.setEndTime(LocalTime.of(12, 0));
        timeSlots.add(timeSlot1);
        
        // Time slot 2
        ChefMultipleScheduleRequest.ScheduleTimeSlot timeSlot2 = new ChefMultipleScheduleRequest.ScheduleTimeSlot();
        timeSlot2.setStartTime(LocalTime.of(14, 0));
        timeSlot2.setEndTime(LocalTime.of(18, 0));
        timeSlots.add(timeSlot2);
        
        testMultipleScheduleRequest.setTimeSlots(timeSlots);
        
        // Setup blocked date
        testBlockedDate = new ChefBlockedDate();
        testBlockedDate.setBlockId(1L);
        testBlockedDate.setChef(testChef);
        testBlockedDate.setBlockedDate(LocalDate.now());
        testBlockedDate.setStartTime(LocalTime.of(8, 0));
        testBlockedDate.setEndTime(LocalTime.of(12, 0));
        testBlockedDate.setReason("Test reason");
        testBlockedDate.setIsDeleted(false);
    }

    // ==================== getScheduleById Tests ====================
    
    @Test
    @DisplayName("Test 1: getScheduleById with valid ID should return schedule")
    void getScheduleById_WhenValidId_ShouldReturnSchedule() {
        // Arrange
        when(chefScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule1));
        when(modelMapper.map(testSchedule1, ChefScheduleResponse.class)).thenReturn(testScheduleResponse1);
        
        // Act
        ChefScheduleResponse response = chefScheduleService.getScheduleById(1L);
        
        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1, response.getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), response.getStartTime());
        assertEquals(LocalTime.of(12, 0), response.getEndTime());
        assertEquals("Asia/Ho_Chi_Minh", response.getTimezone());
        
        verify(chefScheduleRepository).findById(1L);
        verify(modelMapper).map(testSchedule1, ChefScheduleResponse.class);
        verify(timeZoneService).getTimezoneFromAddress(testChef.getAddress());
    }
    
    @Test
    @DisplayName("Test 2: getScheduleById with invalid ID should throw exception")
    void getScheduleById_WhenInvalidId_ShouldThrowException() {
        // Arrange
        when(chefScheduleRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.getScheduleById(99L);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("not found"));
        verify(chefScheduleRepository).findById(99L);
    }
    
    @Test
    @DisplayName("Test 3: getScheduleById with null ID should throw exception")
    void getScheduleById_WhenNullId_ShouldThrowException() {
        // Act & Assert
        assertThrows(VchefApiException.class, () -> {
            chefScheduleService.getScheduleById(null);
        });
    }
    
    @Test
    @DisplayName("Test 4: getScheduleById should map schedule correctly")
    void getScheduleById_ShouldMapScheduleCorrectly() {
        // Arrange
        when(chefScheduleRepository.findById(2L)).thenReturn(Optional.of(testSchedule2));
        when(modelMapper.map(testSchedule2, ChefScheduleResponse.class)).thenReturn(testScheduleResponse2);
        
        // Act
        ChefScheduleResponse response = chefScheduleService.getScheduleById(2L);
        
        // Assert
        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals(2, response.getDayOfWeek());
        assertEquals(LocalTime.of(14, 0), response.getStartTime());
        assertEquals(LocalTime.of(18, 0), response.getEndTime());
        assertEquals("Asia/Ho_Chi_Minh", response.getTimezone());
        
        verify(chefScheduleRepository).findById(2L);
        verify(modelMapper).map(testSchedule2, ChefScheduleResponse.class);
        verify(timeZoneService).getTimezoneFromAddress(testChef.getAddress());
    }
    
    // ==================== updateSchedule Tests ====================
    
    @Test
    @DisplayName("Test 1: updateSchedule with valid data should update schedule")
    void updateSchedule_WithValidData_ShouldUpdateSchedule() {
        // Arrange
        when(chefScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule1));
        when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                .thenReturn(Collections.emptyList());
        when(chefScheduleRepository.save(any(ChefSchedule.class))).thenReturn(testSchedule1);
        when(modelMapper.map(any(ChefSchedule.class), eq(ChefScheduleResponse.class))).thenReturn(testScheduleResponse1);
        
        // Act
        ChefScheduleResponse response = chefScheduleService.updateSchedule(testUpdateRequest);
        
        // Assert
        assertNotNull(response);
        verify(chefScheduleRepository).findById(1L);
        verify(chefScheduleRepository).save(any(ChefSchedule.class));
        verify(modelMapper).map(any(ChefSchedule.class), eq(ChefScheduleResponse.class));
        verify(bookingConflictService).hasBookingConflictOnDayOfWeek(any(Chef.class), eq(1), any(LocalTime.class), any(LocalTime.class), anyInt());
    }
    
    @Test
    @DisplayName("Test 2: updateSchedule with non-existent ID should throw exception")
    void updateSchedule_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(chefScheduleRepository.findById(99L)).thenReturn(Optional.empty());
        
        ChefScheduleUpdateRequest invalidRequest = new ChefScheduleUpdateRequest();
        invalidRequest.setId(99L);
        invalidRequest.setDayOfWeek(1);
        invalidRequest.setStartTime(LocalTime.of(9, 0));
        invalidRequest.setEndTime(LocalTime.of(13, 0));
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.updateSchedule(invalidRequest);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("not found"));
        verify(chefScheduleRepository).findById(99L);
    }
    
    @Test
    @DisplayName("Test 3: updateSchedule with invalid time range should throw exception")
    void updateSchedule_WithInvalidTimeRange_ShouldThrowException() {
        // Arrange
        when(chefScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule1));
        
        ChefScheduleUpdateRequest invalidTimeRequest = new ChefScheduleUpdateRequest();
        invalidTimeRequest.setId(1L);
        invalidTimeRequest.setDayOfWeek(1);
        invalidTimeRequest.setStartTime(LocalTime.of(10, 0));
        invalidTimeRequest.setEndTime(LocalTime.of(11, 0)); // Less than minimum duration (2 hours)
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.updateSchedule(invalidTimeRequest);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("duration"));
        verify(chefScheduleRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Test 4: updateSchedule with schedule conflict should throw exception")
    void updateSchedule_WithScheduleConflict_ShouldThrowException() {
        // Arrange
        when(chefScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule1));
        
        // Create a conflicting schedule
        ChefSchedule conflictingSchedule = new ChefSchedule();
        conflictingSchedule.setId(3L);
        conflictingSchedule.setChef(testChef);
        conflictingSchedule.setDayOfWeek(1);
        conflictingSchedule.setStartTime(LocalTime.of(11, 0));
        conflictingSchedule.setEndTime(LocalTime.of(15, 0));
        conflictingSchedule.setIsDeleted(false);
        
        when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), eq(1)))
                .thenReturn(Arrays.asList(conflictingSchedule));
        
        ChefScheduleUpdateRequest conflictRequest = new ChefScheduleUpdateRequest();
        conflictRequest.setId(1L);
        conflictRequest.setDayOfWeek(1);
        conflictRequest.setStartTime(LocalTime.of(9, 0));
        conflictRequest.setEndTime(LocalTime.of(13, 0)); // Overlaps with conflictingSchedule
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.updateSchedule(conflictRequest);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("conflicts"));
        verify(chefScheduleRepository).findById(1L);
        verify(chefScheduleRepository, times(3)).findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), eq(1));
    }

    @Test
    @DisplayName("Test 5: updateSchedule with invalid day of week should throw exception")
    void updateSchedule_WithInvalidDayOfWeek_ShouldThrowException() {
        // Arrange
        when(chefScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule1));
        
        ChefScheduleUpdateRequest invalidDayRequest = new ChefScheduleUpdateRequest();
        invalidDayRequest.setId(1L);
        invalidDayRequest.setDayOfWeek(7); // Invalid day (should be 0-6)
        invalidDayRequest.setStartTime(LocalTime.of(9, 0));
        invalidDayRequest.setEndTime(LocalTime.of(13, 0));
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.updateSchedule(invalidDayRequest);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Day of week must between 0-6"));
        verify(chefScheduleRepository).findById(1L);
    }
    
    // ==================== deleteSchedule Tests ====================
    
    @Test
    @DisplayName("Test 1: deleteSchedule with valid ID should mark schedule as deleted")
    void deleteSchedule_WithValidId_ShouldMarkScheduleAsDeleted() {
        // Arrange
        when(chefScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule1));
        
        // Capture the schedule being saved to verify isDeleted is set to true
        when(chefScheduleRepository.save(any(ChefSchedule.class))).thenAnswer(invocation -> {
            ChefSchedule savedSchedule = invocation.getArgument(0);
            assertTrue(savedSchedule.getIsDeleted());
            return savedSchedule;
        });
        
        // Act
        chefScheduleService.deleteSchedule(1L);
        
        // Assert
        verify(chefScheduleRepository).findById(1L);
        verify(chefScheduleRepository).save(any(ChefSchedule.class));
        verify(bookingConflictService).hasBookingConflictOnDayOfWeek(
                eq(testChef), eq(1), eq(LocalTime.of(8, 0)), eq(LocalTime.of(12, 0)), eq(60));
    }
    
    @Test
    @DisplayName("Test 2: deleteSchedule with non-existent ID should throw exception")
    void deleteSchedule_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(chefScheduleRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.deleteSchedule(99L);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("not found"));
        verify(chefScheduleRepository).findById(99L);
        verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
    }
    
    @Test
    @DisplayName("Test 3: deleteSchedule with null ID should throw exception")
    void deleteSchedule_WithNullId_ShouldThrowException() {
        // Act & Assert
        assertThrows(VchefApiException.class, () -> {
            chefScheduleService.deleteSchedule(null);
        });
        
        verify(chefScheduleRepository, never()).findById(anyLong());
        verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
    }
    
    @Test
    @DisplayName("Test 4: deleteSchedule with existing bookings should throw exception")
    void deleteSchedule_WithExistingBookings_ShouldThrowException() {
        // Arrange
        when(chefScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule1));
        when(bookingConflictService.hasBookingConflictOnDayOfWeek(
                any(Chef.class), anyInt(), any(LocalTime.class), any(LocalTime.class), anyInt()))
                .thenReturn(true);
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefScheduleService.deleteSchedule(1L);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("existing bookings"));
        verify(chefScheduleRepository).findById(1L);
        verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
    }
    
    // ==================== createScheduleForCurrentChef Tests ====================
    
    @Test
    @DisplayName("Test 1: createScheduleForCurrentChef with valid data should create schedule")
    void createScheduleForCurrentChef_WithValidData_ShouldCreateSchedule() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(modelMapper.map(any(ChefScheduleRequest.class), eq(ChefSchedule.class))).thenReturn(testSchedule1);
            when(chefScheduleRepository.save(any(ChefSchedule.class))).thenReturn(testSchedule1);
            when(modelMapper.map(testSchedule1, ChefScheduleResponse.class)).thenReturn(testScheduleResponse1);
            
            // Act
            ChefScheduleResponse response = chefScheduleService.createScheduleForCurrentChef(testScheduleRequest);
            
            // Assert
            assertNotNull(response);
            assertEquals(testScheduleResponse1.getId(), response.getId());
            assertEquals(testScheduleResponse1.getDayOfWeek(), response.getDayOfWeek());
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository).save(any(ChefSchedule.class));
            verify(modelMapper).map(testSchedule1, ChefScheduleResponse.class);
        }
    }
    
    @Test
    @DisplayName("Test 2: createScheduleForCurrentChef with user not found should throw exception")
    void createScheduleForCurrentChef_WithUserNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);
            
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.createScheduleForCurrentChef(testScheduleRequest);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("User not found"));
            
            verify(userRepository).findById(99L);
            verify(chefRepository, never()).findByUser(any(User.class));
            verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
        }
    }
    
    @Test
    @DisplayName("Test 3: createScheduleForCurrentChef with chef not found should throw exception")
    void createScheduleForCurrentChef_WithChefNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.createScheduleForCurrentChef(testScheduleRequest);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("Chef profile not found"));
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
        }
    }
    
    @Test
    @DisplayName("Test 4: createScheduleForCurrentChef with invalid time range should throw exception")
    void createScheduleForCurrentChef_WithInvalidTimeRange_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Create request with invalid time range
            ChefScheduleRequest invalidRequest = new ChefScheduleRequest();
            invalidRequest.setDayOfWeek(1);
            invalidRequest.setStartTime(LocalTime.of(7, 0)); // Before 8:00 (MIN_WORK_HOUR)
            invalidRequest.setEndTime(LocalTime.of(12, 0));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.createScheduleForCurrentChef(invalidRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Schedule time must be between"));
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
        }
    }

    // ==================== getSchedulesForCurrentChef Tests ====================
    
    @Test
    @DisplayName("Test 1: getSchedulesForCurrentChef should return schedules with timezone")
    void getSchedulesForCurrentChef_ShouldReturnSchedulesWithTimezone() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(chefScheduleRepository.findByChefAndIsDeletedFalse(testChef))
                    .thenReturn(Arrays.asList(testSchedule1, testSchedule2));
            when(modelMapper.map(any(ChefSchedule.class), eq(ChefScheduleResponse.class)))
                    .thenReturn(testScheduleResponse1, testScheduleResponse2);
            
            // Act
            List<ChefScheduleResponse> responses = chefScheduleService.getSchedulesForCurrentChef();
            
            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
            verify(timeZoneService).getTimezoneFromAddress(testChef.getAddress());
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository).findByChefAndIsDeletedFalse(testChef);
        }
    }
    
    // ==================== createMultipleSchedulesForCurrentChef Tests ====================
    
    @Test
    @DisplayName("Test 1: createMultipleSchedulesForCurrentChef with valid data should create schedules")
    void createMultipleSchedulesForCurrentChef_WithValidData_ShouldCreateSchedules() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            ChefSchedule schedule1 = new ChefSchedule();
            schedule1.setId(10L);
            schedule1.setChef(testChef);
            schedule1.setDayOfWeek(1);
            schedule1.setStartTime(LocalTime.of(8, 0));
            schedule1.setEndTime(LocalTime.of(12, 0));
            schedule1.setIsDeleted(false);
            
            ChefSchedule schedule2 = new ChefSchedule();
            schedule2.setId(11L);
            schedule2.setChef(testChef);
            schedule2.setDayOfWeek(1);
            schedule2.setStartTime(LocalTime.of(14, 0));
            schedule2.setEndTime(LocalTime.of(18, 0));
            schedule2.setIsDeleted(false);
            
            ChefScheduleResponse response1 = new ChefScheduleResponse();
            response1.setId(10L);
            response1.setDayOfWeek(1);
            response1.setStartTime(LocalTime.of(8, 0));
            response1.setEndTime(LocalTime.of(12, 0));
            
            ChefScheduleResponse response2 = new ChefScheduleResponse();
            response2.setId(11L);
            response2.setDayOfWeek(1);
            response2.setStartTime(LocalTime.of(14, 0));
            response2.setEndTime(LocalTime.of(18, 0));
            
            when(chefScheduleRepository.save(any(ChefSchedule.class)))
                    .thenReturn(schedule1, schedule2);
            when(modelMapper.map(any(ChefSchedule.class), eq(ChefScheduleResponse.class)))
                    .thenReturn(response1, response2);
            
            // Act
            List<ChefScheduleResponse> responses = chefScheduleService.createMultipleSchedulesForCurrentChef(testMultipleScheduleRequest);
            
            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
            assertEquals(10L, responses.get(0).getId());
            assertEquals(11L, responses.get(1).getId());
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository, times(2)).save(any(ChefSchedule.class));
        }
    }
    
    @Test
    @DisplayName("Test 2: createMultipleSchedulesForCurrentChef with too many sessions should throw exception")
    void createMultipleSchedulesForCurrentChef_WithTooManySessions_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Create request with too many time slots (more than MAX_SESSIONS_PER_DAY = 3)
            ChefMultipleScheduleRequest invalidRequest = new ChefMultipleScheduleRequest();
            invalidRequest.setDayOfWeek(1);
            
            List<ChefMultipleScheduleRequest.ScheduleTimeSlot> timeSlots = new ArrayList<>();
            for (int i = 0; i < 4; i++) { // 4 sessions > MAX_SESSIONS_PER_DAY
                ChefMultipleScheduleRequest.ScheduleTimeSlot slot = new ChefMultipleScheduleRequest.ScheduleTimeSlot();
                slot.setStartTime(LocalTime.of(8 + i * 3, 0));
                slot.setEndTime(LocalTime.of(10 + i * 3, 0));
                timeSlots.add(slot);
            }
            invalidRequest.setTimeSlots(timeSlots);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.createMultipleSchedulesForCurrentChef(invalidRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Maximum number of sessions per day"));
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
        }
    }
    
    @Test
    @DisplayName("Test 3: createMultipleSchedulesForCurrentChef with insufficient gap should throw exception")
    void createMultipleSchedulesForCurrentChef_WithInsufficientGap_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Create request with insufficient gap between sessions
            ChefMultipleScheduleRequest invalidRequest = new ChefMultipleScheduleRequest();
            invalidRequest.setDayOfWeek(1);
            
            List<ChefMultipleScheduleRequest.ScheduleTimeSlot> timeSlots = new ArrayList<>();
            
            ChefMultipleScheduleRequest.ScheduleTimeSlot slot1 = new ChefMultipleScheduleRequest.ScheduleTimeSlot();
            slot1.setStartTime(LocalTime.of(8, 0));
            slot1.setEndTime(LocalTime.of(12, 0));
            timeSlots.add(slot1);
            
            ChefMultipleScheduleRequest.ScheduleTimeSlot slot2 = new ChefMultipleScheduleRequest.ScheduleTimeSlot();
            slot2.setStartTime(LocalTime.of(12, 30)); // Only 30 minutes gap, need 60
            slot2.setEndTime(LocalTime.of(16, 30));
            timeSlots.add(slot2);
            
            invalidRequest.setTimeSlots(timeSlots);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.createMultipleSchedulesForCurrentChef(invalidRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("must have at least"));
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
        }
    }
    
    // ==================== deleteSchedulesByDayOfWeek Tests ====================
    
    @Test
    @DisplayName("Test 1: deleteSchedulesByDayOfWeek with valid day should delete all schedules")
    void deleteSchedulesByDayOfWeek_WithValidDay_ShouldDeleteAllSchedules() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Create test schedules
            ChefSchedule schedule1 = new ChefSchedule();
            schedule1.setId(1L);
            schedule1.setChef(testChef);
            schedule1.setDayOfWeek(1);
            schedule1.setStartTime(LocalTime.of(8, 0));
            schedule1.setEndTime(LocalTime.of(12, 0));
            schedule1.setIsDeleted(false);
            
            ChefSchedule schedule2 = new ChefSchedule();
            schedule2.setId(2L);
            schedule2.setChef(testChef);
            schedule2.setDayOfWeek(1);
            schedule2.setStartTime(LocalTime.of(14, 0));
            schedule2.setEndTime(LocalTime.of(18, 0));
            schedule2.setIsDeleted(false);
            
            List<ChefSchedule> schedules = Arrays.asList(schedule1, schedule2);
            
            when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(testChef, 1))
                    .thenReturn(schedules);
            
            // Act
            chefScheduleService.deleteSchedulesByDayOfWeek(1);
            
            // Assert
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository).findByChefAndDayOfWeekAndIsDeletedFalse(testChef, 1);
            verify(chefScheduleRepository, times(2)).save(any(ChefSchedule.class));
            verify(bookingConflictService).hasActiveBookingsForDayOfWeek(testChef.getId(), 1);
        }
    }
    
    @Test
    @DisplayName("Test 2: deleteSchedulesByDayOfWeek with no schedules should throw exception")
    void deleteSchedulesByDayOfWeek_WithNoSchedules_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(testChef, 3))
                    .thenReturn(Collections.emptyList());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.deleteSchedulesByDayOfWeek(3);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("No schedules found for day of week: 3"));
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository).findByChefAndDayOfWeekAndIsDeletedFalse(testChef, 3);
            verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
        }
    }
    
    @Test
    @DisplayName("Test 3: deleteSchedulesByDayOfWeek with existing bookings should throw exception")
    void deleteSchedulesByDayOfWeek_WithExistingBookings_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(chefScheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(testChef, 1))
                    .thenReturn(Arrays.asList(testSchedule1));
            when(bookingConflictService.hasActiveBookingsForDayOfWeek(testChef.getId(), 1))
                    .thenReturn(true);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefScheduleService.deleteSchedulesByDayOfWeek(1);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("existing bookings"));
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(chefScheduleRepository).findByChefAndDayOfWeekAndIsDeletedFalse(testChef, 1);
            verify(bookingConflictService).hasActiveBookingsForDayOfWeek(testChef.getId(), 1);
            verify(chefScheduleRepository, never()).save(any(ChefSchedule.class));
        }
    }

    private <T> T eq(T value) {
        return Mockito.eq(value);
    }
} 