package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefBlockedDate;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRangeRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefBlockedDateResponse;
import com.spring2025.vietchefs.repositories.ChefBlockedDateRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.impl.ChefBlockedDateServiceImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChefBlockedDateServiceTest {

    @Mock
    private ChefBlockedDateRepository blockedDateRepository;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChefScheduleRepository scheduleRepository;

    @Mock
    private BookingConflictService bookingConflictService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private TimeZoneService timeZoneService;

    @InjectMocks
    private ChefBlockedDateServiceImpl chefBlockedDateService;

    private User testUser;
    private Chef testChef;
    private ChefBlockedDate testBlockedDate1;
    private ChefBlockedDate testBlockedDate2;
    private ChefBlockedDateRequest testBlockedDateRequest;
    private ChefBlockedDateUpdateRequest testUpdateRequest;
    private ChefBlockedDateRangeRequest testRangeRequest;
    private ChefBlockedDateResponse testBlockedDateResponse1;
    private ChefBlockedDateResponse testBlockedDateResponse2;

    @BeforeEach
    void setUp() {
        // Setup lenient mocks
        Mockito.lenient().when(timeZoneService.getTimezoneFromAddress(anyString())).thenReturn("Asia/Ho_Chi_Minh");
        Mockito.lenient().when(bookingConflictService.hasBookingConflict(any(Chef.class), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class))).thenReturn(false);
        
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

        // Setup test blocked date 1
        testBlockedDate1 = new ChefBlockedDate();
        testBlockedDate1.setBlockId(1L);
        testBlockedDate1.setChef(testChef);
        testBlockedDate1.setBlockedDate(LocalDate.of(2024, 12, 25));
        testBlockedDate1.setStartTime(LocalTime.of(8, 0));
        testBlockedDate1.setEndTime(LocalTime.of(12, 0));
        testBlockedDate1.setReason("Holiday");
        testBlockedDate1.setIsDeleted(false);

        // Setup test blocked date 2
        testBlockedDate2 = new ChefBlockedDate();
        testBlockedDate2.setBlockId(2L);
        testBlockedDate2.setChef(testChef);
        testBlockedDate2.setBlockedDate(LocalDate.of(2024, 12, 26));
        testBlockedDate2.setStartTime(LocalTime.of(14, 0));
        testBlockedDate2.setEndTime(LocalTime.of(18, 0));
        testBlockedDate2.setReason("Personal");
        testBlockedDate2.setIsDeleted(false);

        // Setup blocked date request
        testBlockedDateRequest = new ChefBlockedDateRequest();
        testBlockedDateRequest.setBlockedDate(LocalDate.of(2024, 12, 27));
        testBlockedDateRequest.setStartTime(LocalTime.of(10, 0));
        testBlockedDateRequest.setEndTime(LocalTime.of(14, 0));
        testBlockedDateRequest.setReason("Vacation");

        // Setup update request
        testUpdateRequest = new ChefBlockedDateUpdateRequest();
        testUpdateRequest.setBlockId(1L);
        testUpdateRequest.setBlockedDate(LocalDate.of(2024, 12, 25));
        testUpdateRequest.setStartTime(LocalTime.of(9, 0));
        testUpdateRequest.setEndTime(LocalTime.of(13, 0));
        testUpdateRequest.setReason("Updated Holiday");

        // Setup range request
        testRangeRequest = new ChefBlockedDateRangeRequest();
        testRangeRequest.setStartDate(LocalDate.of(2024, 12, 28));
        testRangeRequest.setEndDate(LocalDate.of(2024, 12, 30));
        testRangeRequest.setStartTime(LocalTime.of(8, 0));
        testRangeRequest.setEndTime(LocalTime.of(18, 0));
        testRangeRequest.setReason("Year-end break");

        // Setup blocked date response 1
        testBlockedDateResponse1 = new ChefBlockedDateResponse();
        testBlockedDateResponse1.setBlockId(1L);
        testBlockedDateResponse1.setBlockedDate(LocalDate.of(2024, 12, 25));
        testBlockedDateResponse1.setStartTime(LocalTime.of(8, 0));
        testBlockedDateResponse1.setEndTime(LocalTime.of(12, 0));
        testBlockedDateResponse1.setReason("Holiday");
        testBlockedDateResponse1.setTimezone("Asia/Ho_Chi_Minh");

        // Setup blocked date response 2
        testBlockedDateResponse2 = new ChefBlockedDateResponse();
        testBlockedDateResponse2.setBlockId(2L);
        testBlockedDateResponse2.setBlockedDate(LocalDate.of(2024, 12, 26));
        testBlockedDateResponse2.setStartTime(LocalTime.of(14, 0));
        testBlockedDateResponse2.setEndTime(LocalTime.of(18, 0));
        testBlockedDateResponse2.setReason("Personal");
        testBlockedDateResponse2.setTimezone("Asia/Ho_Chi_Minh");
    }
    
    // ==================== getBlockedDateById Tests ====================
    
    @Test
    @DisplayName("Test 1: getBlockedDateById with valid ID should return blocked date")
    void getBlockedDateById_WhenValidId_ShouldReturnBlockedDate() {
        // Arrange
        when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(testBlockedDate1));
        when(modelMapper.map(testBlockedDate1, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse1);
        
        // Act
        ChefBlockedDateResponse response = chefBlockedDateService.getBlockedDateById(1L);
        
        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getBlockId());
        assertEquals(LocalDate.of(2024, 12, 25), response.getBlockedDate());
        assertEquals(LocalTime.of(8, 0), response.getStartTime());
        assertEquals(LocalTime.of(12, 0), response.getEndTime());
        assertEquals("Holiday", response.getReason());
        assertEquals("Asia/Ho_Chi_Minh", response.getTimezone());
        
        verify(blockedDateRepository).findById(1L);
        verify(modelMapper).map(testBlockedDate1, ChefBlockedDateResponse.class);
        verify(timeZoneService).getTimezoneFromAddress(testChef.getAddress());
    }
    
    @Test
    @DisplayName("Test 2: getBlockedDateById with invalid ID should throw exception")
    void getBlockedDateById_WhenInvalidId_ShouldThrowException() {
        // Arrange
        when(blockedDateRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefBlockedDateService.getBlockedDateById(99L);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("not found"));
        verify(blockedDateRepository).findById(99L);
    }
    
    @Test
    @DisplayName("Test 3: getBlockedDateById with null ID should throw exception")
    void getBlockedDateById_WhenNullId_ShouldThrowException() {
        // Act & Assert
        assertThrows(VchefApiException.class, () -> {
            chefBlockedDateService.getBlockedDateById(null);
        });
    }
    
    @Test
    @DisplayName("Test 4: getBlockedDateById should map blocked date correctly")
    void getBlockedDateById_ShouldMapBlockedDateCorrectly() {
        // Arrange
        when(blockedDateRepository.findById(2L)).thenReturn(Optional.of(testBlockedDate2));
        when(modelMapper.map(testBlockedDate2, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse2);
        
        // Act
        ChefBlockedDateResponse response = chefBlockedDateService.getBlockedDateById(2L);
        
        // Assert
        assertNotNull(response);
        assertEquals(2L, response.getBlockId());
        assertEquals(LocalDate.of(2024, 12, 26), response.getBlockedDate());
        assertEquals(LocalTime.of(14, 0), response.getStartTime());
        assertEquals(LocalTime.of(18, 0), response.getEndTime());
        assertEquals("Personal", response.getReason());
        assertEquals("Asia/Ho_Chi_Minh", response.getTimezone());
        
        verify(blockedDateRepository).findById(2L);
        verify(modelMapper).map(testBlockedDate2, ChefBlockedDateResponse.class);
        verify(timeZoneService).getTimezoneFromAddress(testChef.getAddress());
    }
    
    // ==================== updateBlockedDate Tests ====================
    
    @Test
    @DisplayName("Test 1: updateBlockedDate with valid data should update blocked date")
    void updateBlockedDate_WithValidData_ShouldUpdateBlockedDate() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(testBlockedDate1));
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(Chef.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(testBlockedDate1);
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(testBlockedDateResponse1);
            
            // Act
            ChefBlockedDateResponse response = chefBlockedDateService.updateBlockedDate(testUpdateRequest);
            
            // Assert
            assertNotNull(response);
            verify(blockedDateRepository).findById(1L);
            verify(blockedDateRepository).save(any(ChefBlockedDate.class));
            verify(modelMapper).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
            verify(bookingConflictService).hasBookingConflict(eq(testChef), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        }
    }
    
    @Test
    @DisplayName("Test 2: updateBlockedDate with non-existent ID should throw exception")
    void updateBlockedDate_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(blockedDateRepository.findById(99L)).thenReturn(Optional.empty());
        
        ChefBlockedDateUpdateRequest invalidRequest = new ChefBlockedDateUpdateRequest();
        invalidRequest.setBlockId(99L);
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefBlockedDateService.updateBlockedDate(invalidRequest);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("not found"));
        verify(blockedDateRepository).findById(99L);
    }
    
    @Test
    @DisplayName("Test 3: updateBlockedDate with invalid time range should throw exception")
    void updateBlockedDate_WithInvalidTimeRange_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(testBlockedDate1));
            
            ChefBlockedDateUpdateRequest invalidTimeRequest = new ChefBlockedDateUpdateRequest();
            invalidTimeRequest.setBlockId(1L);
            invalidTimeRequest.setStartTime(LocalTime.of(7, 0)); // Before MIN_WORK_HOUR
            invalidTimeRequest.setEndTime(LocalTime.of(12, 0));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.updateBlockedDate(invalidTimeRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Blocked time must be between"));
            
            verify(blockedDateRepository).findById(1L);
        }
    }
    
    @Test
    @DisplayName("Test 4: updateBlockedDate with wrong chef should throw permission exception")
    void updateBlockedDate_WithWrongChef_ShouldThrowPermissionException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Create blocked date with different chef
            Chef differentChef = new Chef();
            differentChef.setId(2L);
            
            ChefBlockedDate blockedDateWithDifferentChef = new ChefBlockedDate();
            blockedDateWithDifferentChef.setBlockId(1L);
            blockedDateWithDifferentChef.setChef(differentChef);
            
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(blockedDateWithDifferentChef));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.updateBlockedDate(testUpdateRequest);
            });
            
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertTrue(exception.getMessage().contains("permission"));
            
            verify(blockedDateRepository).findById(1L);
            verify(blockedDateRepository, never()).save(any(ChefBlockedDate.class));
        }
    }
    
    @Test
    @DisplayName("Test 5: updateBlockedDate with booking conflict should throw exception")
    void updateBlockedDate_WithBookingConflict_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(testBlockedDate1));
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(Chef.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(bookingConflictService.hasBookingConflict(any(Chef.class), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(true);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.updateBlockedDate(testUpdateRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("existing bookings"));
            
            verify(blockedDateRepository).findById(1L);
            verify(bookingConflictService).hasBookingConflict(eq(testChef), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
            verify(blockedDateRepository, never()).save(any(ChefBlockedDate.class));
        }
    }
    
    // ==================== deleteBlockedDate Tests ====================
    
    @Test
    @DisplayName("Test 1: deleteBlockedDate with valid ID should mark as deleted")
    void deleteBlockedDate_WithValidId_ShouldMarkAsDeleted() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(testBlockedDate1));
            
            // Act
            chefBlockedDateService.deleteBlockedDate(1L);
            
            // Assert
            assertTrue(testBlockedDate1.getIsDeleted());
            verify(blockedDateRepository).findById(1L);
            verify(blockedDateRepository).save(testBlockedDate1);
        }
    }
    
    @Test
    @DisplayName("Test 2: deleteBlockedDate with non-existent ID should throw exception")
    void deleteBlockedDate_WithNonExistentId_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(blockedDateRepository.findById(99L)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.deleteBlockedDate(99L);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("not found"));
        }
    }
    
    @Test
    @DisplayName("Test 3: deleteBlockedDate with null ID should throw exception")
    void deleteBlockedDate_WithNullId_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Act & Assert
            assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.deleteBlockedDate(null);
            });
        }
    }
    
    @Test
    @DisplayName("Test 4: deleteBlockedDate with wrong chef should throw permission exception")
    void deleteBlockedDate_WithWrongChef_ShouldThrowPermissionException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Create blocked date with different chef
            Chef differentChef = new Chef();
            differentChef.setId(2L);
            
            ChefBlockedDate blockedDateWithDifferentChef = new ChefBlockedDate();
            blockedDateWithDifferentChef.setBlockId(1L);
            blockedDateWithDifferentChef.setChef(differentChef);
            
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(blockedDateWithDifferentChef));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.deleteBlockedDate(1L);
            });
            
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
            assertTrue(exception.getMessage().contains("permission"));
            
            verify(blockedDateRepository).findById(1L);
            verify(blockedDateRepository, never()).save(any(ChefBlockedDate.class));
        }
    }
    
    // ==================== createBlockedDateForCurrentChef Tests ====================
    
    @Test
    @DisplayName("Test 1: createBlockedDateForCurrentChef with valid data should create blocked date")
    void createBlockedDateForCurrentChef_WithValidData_ShouldCreateBlockedDate() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
            when(bookingConflictService.hasBookingConflict(
                    eq(testChef), 
                    any(LocalDate.class), 
                    any(LocalTime.class), 
                    any(LocalTime.class))).thenReturn(false);
            
            // Create a proper blocked date that matches the request
            ChefBlockedDate savedBlockedDate = new ChefBlockedDate();
            savedBlockedDate.setBlockId(1L);
            savedBlockedDate.setChef(testChef);
            savedBlockedDate.setBlockedDate(LocalDate.of(2024, 12, 27)); // Match request date
            savedBlockedDate.setStartTime(LocalTime.of(10, 0));
            savedBlockedDate.setEndTime(LocalTime.of(14, 0));
            savedBlockedDate.setReason("Vacation");
            savedBlockedDate.setIsDeleted(false);
            
            // Create a proper response that matches the request
            ChefBlockedDateResponse expectedResponse = new ChefBlockedDateResponse();
            expectedResponse.setBlockId(1L);
            expectedResponse.setBlockedDate(LocalDate.of(2024, 12, 27)); // Match request date
            expectedResponse.setStartTime(LocalTime.of(10, 0));
            expectedResponse.setEndTime(LocalTime.of(14, 0));
            expectedResponse.setReason("Vacation");
            expectedResponse.setTimezone("Asia/Ho_Chi_Minh");
            
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(savedBlockedDate);
            when(modelMapper.map(savedBlockedDate, ChefBlockedDateResponse.class)).thenReturn(expectedResponse);
            
            // Act
            ChefBlockedDateResponse response = chefBlockedDateService.createBlockedDateForCurrentChef(testBlockedDateRequest);
            
            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getBlockId());
            assertEquals(LocalDate.of(2024, 12, 27), response.getBlockedDate());
            assertEquals(LocalTime.of(10, 0), response.getStartTime());
            assertEquals(LocalTime.of(14, 0), response.getEndTime());
            assertEquals("Vacation", response.getReason());
            
            verify(blockedDateRepository).save(any(ChefBlockedDate.class));
            verify(modelMapper).map(savedBlockedDate, ChefBlockedDateResponse.class);
        }
    }
    
    @Test
    @DisplayName("Test 2: createBlockedDateForCurrentChef with user not found should throw exception")
    void createBlockedDateForCurrentChef_WithUserNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);
            
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.createBlockedDateForCurrentChef(testBlockedDateRequest);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }
    
    @Test
    @DisplayName("Test 3: createBlockedDateForCurrentChef with chef not found should throw exception")
    void createBlockedDateForCurrentChef_WithChefNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.createBlockedDateForCurrentChef(testBlockedDateRequest);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("Chef profile not found"));
        }
    }
    
    @Test
    @DisplayName("Test 4: createBlockedDateForCurrentChef with time conflict should throw exception")
    void createBlockedDateForCurrentChef_WithTimeConflict_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Set invalid time (start time before 8:00)
            testBlockedDateRequest.setStartTime(LocalTime.of(7, 0));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.createBlockedDateForCurrentChef(testBlockedDateRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Blocked time must be between"));
        }
    }
    
    // ==================== getBlockedDatesForCurrentChef Tests ====================
    
    @Test
    @DisplayName("Test 1: getBlockedDatesForCurrentChef with existing dates should return list")
    void getBlockedDatesForCurrentChef_WithExistingDates_ShouldReturnList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            List<ChefBlockedDate> blockedDates = Arrays.asList(testBlockedDate1, testBlockedDate2);
            when(blockedDateRepository.findByChefAndIsDeletedFalse(testChef)).thenReturn(blockedDates);
            
            when(modelMapper.map(testBlockedDate1, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse1);
            when(modelMapper.map(testBlockedDate2, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse2);
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.getBlockedDatesForCurrentChef();
            
            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
            assertEquals(1L, responses.get(0).getBlockId());
            assertEquals(2L, responses.get(1).getBlockId());
            
            verify(blockedDateRepository).findByChefAndIsDeletedFalse(testChef);
            verify(modelMapper, times(2)).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
        }
    }
    
    @Test
    @DisplayName("Test 2: getBlockedDatesForCurrentChef with user not found should throw exception")
    void getBlockedDatesForCurrentChef_WithUserNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);
            
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.getBlockedDatesForCurrentChef();
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }
    
    @Test
    @DisplayName("Test 3: getBlockedDatesForCurrentChef with chef not found should throw exception")
    void getBlockedDatesForCurrentChef_WithChefNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.getBlockedDatesForCurrentChef();
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("Chef profile not found"));
        }
    }
    
    @Test
    @DisplayName("Test 4: getBlockedDatesForCurrentChef with no dates should return empty list")
    void getBlockedDatesForCurrentChef_WithNoDates_ShouldReturnEmptyList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findByChefAndIsDeletedFalse(testChef)).thenReturn(Collections.emptyList());
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.getBlockedDatesForCurrentChef();
            
            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
            
            verify(blockedDateRepository).findByChefAndIsDeletedFalse(testChef);
            verify(modelMapper, never()).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
        }
    }
    
    // ==================== createBlockedDateRangeForCurrentChef Tests ====================
    
    @Test
    @DisplayName("Test 1: createBlockedDateRangeForCurrentChef with valid data should create dates")
    void createBlockedDateRangeForCurrentChef_WithValidData_ShouldCreateDates() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(Chef.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(bookingConflictService.hasBookingConflict(any(Chef.class), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                    .thenReturn(false);
            
            // Mock save to return different blocked dates for each call
            ChefBlockedDate savedDate1 = new ChefBlockedDate();
            savedDate1.setBlockId(1L);
            savedDate1.setChef(testChef);
            savedDate1.setBlockedDate(LocalDate.of(2024, 12, 28));
            savedDate1.setStartTime(LocalTime.of(8, 0));
            savedDate1.setEndTime(LocalTime.of(22, 0)); // Full day for middle day
            savedDate1.setReason("Year-end break");
            savedDate1.setIsDeleted(false);
            
            ChefBlockedDate savedDate2 = new ChefBlockedDate();
            savedDate2.setBlockId(2L);
            savedDate2.setChef(testChef);
            savedDate2.setBlockedDate(LocalDate.of(2024, 12, 29));
            savedDate2.setStartTime(LocalTime.of(8, 0));
            savedDate2.setEndTime(LocalTime.of(22, 0)); // Full day for middle day
            savedDate2.setReason("Year-end break");
            savedDate2.setIsDeleted(false);
            
            ChefBlockedDate savedDate3 = new ChefBlockedDate();
            savedDate3.setBlockId(3L);
            savedDate3.setChef(testChef);
            savedDate3.setBlockedDate(LocalDate.of(2024, 12, 30));
            savedDate3.setStartTime(LocalTime.of(8, 0));
            savedDate3.setEndTime(LocalTime.of(18, 0)); // End time on last day
            savedDate3.setReason("Year-end break");
            savedDate3.setIsDeleted(false);
            
            when(blockedDateRepository.save(any(ChefBlockedDate.class)))
                    .thenReturn(savedDate1, savedDate2, savedDate3);
            
            ChefBlockedDateResponse response1 = new ChefBlockedDateResponse();
            response1.setBlockId(1L);
            response1.setBlockedDate(LocalDate.of(2024, 12, 28));
            response1.setStartTime(LocalTime.of(8, 0));
            response1.setEndTime(LocalTime.of(22, 0));
            response1.setReason("Year-end break");
            
            ChefBlockedDateResponse response2 = new ChefBlockedDateResponse();
            response2.setBlockId(2L);
            response2.setBlockedDate(LocalDate.of(2024, 12, 29));
            response2.setStartTime(LocalTime.of(8, 0));
            response2.setEndTime(LocalTime.of(22, 0));
            response2.setReason("Year-end break");
            
            ChefBlockedDateResponse response3 = new ChefBlockedDateResponse();
            response3.setBlockId(3L);
            response3.setBlockedDate(LocalDate.of(2024, 12, 30));
            response3.setStartTime(LocalTime.of(8, 0));
            response3.setEndTime(LocalTime.of(18, 0));
            response3.setReason("Year-end break");
            
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class)))
                    .thenReturn(response1, response2, response3);
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.createBlockedDateRangeForCurrentChef(testRangeRequest);
            
            // Assert
            assertNotNull(responses);
            assertEquals(3, responses.size()); // 28, 29, 30 December
            
            // Verify first day (start date with custom start time)
            assertEquals(LocalDate.of(2024, 12, 28), responses.get(0).getBlockedDate());
            assertEquals(LocalTime.of(8, 0), responses.get(0).getStartTime());
            assertEquals(LocalTime.of(22, 0), responses.get(0).getEndTime());
            
            // Verify middle day (full working day)
            assertEquals(LocalDate.of(2024, 12, 29), responses.get(1).getBlockedDate());
            assertEquals(LocalTime.of(8, 0), responses.get(1).getStartTime());
            assertEquals(LocalTime.of(22, 0), responses.get(1).getEndTime());
            
            // Verify last day (end date with custom end time)
            assertEquals(LocalDate.of(2024, 12, 30), responses.get(2).getBlockedDate());
            assertEquals(LocalTime.of(8, 0), responses.get(2).getStartTime());
            assertEquals(LocalTime.of(18, 0), responses.get(2).getEndTime());
            
            verify(blockedDateRepository, times(3)).save(any(ChefBlockedDate.class));
            verify(modelMapper, times(3)).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
            verify(bookingConflictService, times(3)).hasBookingConflict(any(Chef.class), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        }
    }
    
    @Test
    @DisplayName("Test 2: createBlockedDateRangeForCurrentChef with invalid date range should throw exception")
    void createBlockedDateRangeForCurrentChef_WithInvalidDateRange_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            // Set invalid date range (end before start)
            testRangeRequest.setStartDate(LocalDate.now().plusDays(5));
            testRangeRequest.setEndDate(LocalDate.now());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.createBlockedDateRangeForCurrentChef(testRangeRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Start date must be before or equal to end date"));
        }
    }
    
    @Test
    @DisplayName("Test 3: createBlockedDateRangeForCurrentChef with chef not found should throw exception")
    void createBlockedDateRangeForCurrentChef_WithChefNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.empty());
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.createBlockedDateRangeForCurrentChef(testRangeRequest);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("Chef profile not found"));
        }
    }
    
    @Test
    @DisplayName("Test 4: createBlockedDateRangeForCurrentChef with booking conflict should throw exception")
    void createBlockedDateRangeForCurrentChef_WithBookingConflict_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
                
            // Mock booking conflict on the first day of the range (2024-12-28)
            when(bookingConflictService.hasBookingConflict(
                    eq(testChef), 
                    eq(LocalDate.of(2024, 12, 28)), // First day of testRangeRequest
                    any(LocalTime.class), 
                    any(LocalTime.class))).thenReturn(true);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.createBlockedDateRangeForCurrentChef(testRangeRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("existing bookings"));
            
            verify(userRepository).findById(1L);
            verify(chefRepository).findByUser(testUser);
            verify(bookingConflictService).hasBookingConflict(
                eq(testChef), 
                eq(LocalDate.of(2024, 12, 28)), 
                any(LocalTime.class), 
                any(LocalTime.class));
        }
    }
    
    // ==================== getBlockedDatesForCurrentChefBetween Tests ====================
    
    @Test
    @DisplayName("Test 1: getBlockedDatesForCurrentChefBetween with existing dates should return list")
    void getBlockedDatesForCurrentChefBetween_WithExistingDates_ShouldReturnList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            
            List<ChefBlockedDate> blockedDates = Arrays.asList(testBlockedDate1, testBlockedDate2);
            when(blockedDateRepository.findByChefAndBlockedDateBetweenAndIsDeletedFalse(
                eq(testChef), eq(startDate), eq(endDate))).thenReturn(blockedDates);
            
            when(modelMapper.map(testBlockedDate1, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse1);
            when(modelMapper.map(testBlockedDate2, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse2);
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.getBlockedDatesForCurrentChefBetween(startDate, endDate);
            
            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
            assertEquals(1L, responses.get(0).getBlockId());
            assertEquals(2L, responses.get(1).getBlockId());
            
            verify(blockedDateRepository).findByChefAndBlockedDateBetweenAndIsDeletedFalse(
                eq(testChef), eq(startDate), eq(endDate));
            verify(modelMapper, times(2)).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
        }
    }
    
    @Test
    @DisplayName("Test 2: getBlockedDatesForCurrentChefBetween with user not found should throw exception")
    void getBlockedDatesForCurrentChefBetween_WithUserNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);
            
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.getBlockedDatesForCurrentChefBetween(startDate, endDate);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }
    
    @Test
    @DisplayName("Test 3: getBlockedDatesForCurrentChefBetween with chef not found should throw exception")
    void getBlockedDatesForCurrentChefBetween_WithChefNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.empty());
            
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.getBlockedDatesForCurrentChefBetween(startDate, endDate);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("Chef profile not found"));
        }
    }
    
    @Test
    @DisplayName("Test 4: getBlockedDatesForCurrentChefBetween with no dates should return empty list")
    void getBlockedDatesForCurrentChefBetween_WithNoDates_ShouldReturnEmptyList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            
            when(blockedDateRepository.findByChefAndBlockedDateBetweenAndIsDeletedFalse(
                eq(testChef), eq(startDate), eq(endDate))).thenReturn(Collections.emptyList());
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.getBlockedDatesForCurrentChefBetween(startDate, endDate);
            
            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
            
            verify(blockedDateRepository).findByChefAndBlockedDateBetweenAndIsDeletedFalse(
                eq(testChef), eq(startDate), eq(endDate));
            verify(modelMapper, never()).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
        }
    }
    
    // ==================== getBlockedDatesForCurrentChefByDate Tests ====================
    
    @Test
    @DisplayName("Test 1: getBlockedDatesForCurrentChefByDate with existing dates should return list")
    void getBlockedDatesForCurrentChefByDate_WithExistingDates_ShouldReturnList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            LocalDate date = LocalDate.now();
            
            List<ChefBlockedDate> blockedDates = Collections.singletonList(testBlockedDate1);
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(
                eq(testChef), eq(date))).thenReturn(blockedDates);
            
            when(modelMapper.map(testBlockedDate1, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse1);
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.getBlockedDatesForCurrentChefByDate(date);
            
            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(1L, responses.get(0).getBlockId());
            
            verify(blockedDateRepository).findByChefAndBlockedDateAndIsDeletedFalse(
                eq(testChef), eq(date));
            verify(modelMapper).map(testBlockedDate1, ChefBlockedDateResponse.class);
        }
    }
    
    @Test
    @DisplayName("Test 2: getBlockedDatesForCurrentChefByDate with user not found should throw exception")
    void getBlockedDatesForCurrentChefByDate_WithUserNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(99L);
            
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            
            LocalDate date = LocalDate.now();
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.getBlockedDatesForCurrentChefByDate(date);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }
    
    @Test
    @DisplayName("Test 3: getBlockedDatesForCurrentChefByDate with chef not found should throw exception")
    void getBlockedDatesForCurrentChefByDate_WithChefNotFound_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.empty());
            
            LocalDate date = LocalDate.now();
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.getBlockedDatesForCurrentChefByDate(date);
            });
            
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("Chef profile not found"));
        }
    }
    
    @Test
    @DisplayName("Test 4: getBlockedDatesForCurrentChefByDate with no dates should return empty list")
    void getBlockedDatesForCurrentChefByDate_WithNoDates_ShouldReturnEmptyList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            LocalDate date = LocalDate.now();
            
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(
                eq(testChef), eq(date))).thenReturn(Collections.emptyList());
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.getBlockedDatesForCurrentChefByDate(date);
            
            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
            
            verify(blockedDateRepository).findByChefAndBlockedDateAndIsDeletedFalse(
                eq(testChef), eq(date));
            verify(modelMapper, never()).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
        }
    }
    
    private <T> T eq(T value) {
        return Mockito.eq(value);
    }
    
    private int anyInt() {
        return Mockito.anyInt();
    }
} 