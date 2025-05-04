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
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testchef");
        testUser.setEmail("testchef@example.com");

        // Setup test chef
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);
        
        // Setup test blocked date 1
        testBlockedDate1 = new ChefBlockedDate();
        testBlockedDate1.setBlockId(1L);
        testBlockedDate1.setChef(testChef);
        testBlockedDate1.setBlockedDate(LocalDate.now());
        testBlockedDate1.setStartTime(LocalTime.of(8, 0));
        testBlockedDate1.setEndTime(LocalTime.of(12, 0));
        testBlockedDate1.setReason("Test reason 1");
        testBlockedDate1.setIsDeleted(false);
        
        // Setup test blocked date 2
        testBlockedDate2 = new ChefBlockedDate();
        testBlockedDate2.setBlockId(2L);
        testBlockedDate2.setChef(testChef);
        testBlockedDate2.setBlockedDate(LocalDate.now().plusDays(1));
        testBlockedDate2.setStartTime(LocalTime.of(14, 0));
        testBlockedDate2.setEndTime(LocalTime.of(18, 0));
        testBlockedDate2.setReason("Test reason 2");
        testBlockedDate2.setIsDeleted(false);
        
        // Setup test blocked date response 1
        testBlockedDateResponse1 = new ChefBlockedDateResponse();
        testBlockedDateResponse1.setBlockId(1L);
        testBlockedDateResponse1.setBlockedDate(LocalDate.now());
        testBlockedDateResponse1.setStartTime(LocalTime.of(8, 0));
        testBlockedDateResponse1.setEndTime(LocalTime.of(12, 0));
        testBlockedDateResponse1.setReason("Test reason 1");
        
        // Setup test blocked date response 2
        testBlockedDateResponse2 = new ChefBlockedDateResponse();
        testBlockedDateResponse2.setBlockId(2L);
        testBlockedDateResponse2.setBlockedDate(LocalDate.now().plusDays(1));
        testBlockedDateResponse2.setStartTime(LocalTime.of(14, 0));
        testBlockedDateResponse2.setEndTime(LocalTime.of(18, 0));
        testBlockedDateResponse2.setReason("Test reason 2");
        
        // Setup blocked date request
        testBlockedDateRequest = new ChefBlockedDateRequest();
        testBlockedDateRequest.setBlockedDate(LocalDate.now());
        testBlockedDateRequest.setStartTime(LocalTime.of(8, 0));
        testBlockedDateRequest.setEndTime(LocalTime.of(12, 0));
        testBlockedDateRequest.setReason("Test reason");
        
        // Setup update request
        testUpdateRequest = new ChefBlockedDateUpdateRequest();
        testUpdateRequest.setBlockId(1L);
        testUpdateRequest.setBlockedDate(LocalDate.now());
        testUpdateRequest.setStartTime(LocalTime.of(9, 0));
        testUpdateRequest.setEndTime(LocalTime.of(13, 0));
        testUpdateRequest.setReason("Updated reason");
        
        // Setup range request
        testRangeRequest = new ChefBlockedDateRangeRequest();
        testRangeRequest.setStartDate(LocalDate.now());
        testRangeRequest.setEndDate(LocalDate.now().plusDays(2));
        testRangeRequest.setStartTime(LocalTime.of(10, 0));
        testRangeRequest.setEndTime(LocalTime.of(16, 0));
        testRangeRequest.setReason("Range block reason");
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
        assertEquals(LocalDate.now(), response.getBlockedDate());
        assertEquals(LocalTime.of(8, 0), response.getStartTime());
        assertEquals(LocalTime.of(12, 0), response.getEndTime());
        assertEquals("Test reason 1", response.getReason());
        
        verify(blockedDateRepository).findById(1L);
        verify(modelMapper).map(testBlockedDate1, ChefBlockedDateResponse.class);
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
        assertEquals(LocalDate.now().plusDays(1), response.getBlockedDate());
        assertEquals(LocalTime.of(14, 0), response.getStartTime());
        assertEquals(LocalTime.of(18, 0), response.getEndTime());
        assertEquals("Test reason 2", response.getReason());
        
        verify(blockedDateRepository).findById(2L);
        verify(modelMapper).map(testBlockedDate2, ChefBlockedDateResponse.class);
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
            when(bookingConflictService.hasBookingConflict(
                    eq(testChef), 
                    any(LocalDate.class), 
                    any(LocalTime.class), 
                    any(LocalTime.class))).thenReturn(false);
                    
            ChefBlockedDate updatedBlockedDate = new ChefBlockedDate();
            updatedBlockedDate.setBlockId(1L);
            updatedBlockedDate.setChef(testChef);
            updatedBlockedDate.setBlockedDate(testUpdateRequest.getBlockedDate());
            updatedBlockedDate.setStartTime(testUpdateRequest.getStartTime());
            updatedBlockedDate.setEndTime(testUpdateRequest.getEndTime());
            updatedBlockedDate.setReason(testUpdateRequest.getReason());
            updatedBlockedDate.setIsDeleted(false);
            
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(updatedBlockedDate);
            
            ChefBlockedDateResponse updatedResponse = new ChefBlockedDateResponse();
            updatedResponse.setBlockId(1L);
            updatedResponse.setBlockedDate(testUpdateRequest.getBlockedDate());
            updatedResponse.setStartTime(testUpdateRequest.getStartTime());
            updatedResponse.setEndTime(testUpdateRequest.getEndTime());
            updatedResponse.setReason(testUpdateRequest.getReason());
            
            when(modelMapper.map(updatedBlockedDate, ChefBlockedDateResponse.class)).thenReturn(updatedResponse);
            
            // Act
            ChefBlockedDateResponse response = chefBlockedDateService.updateBlockedDate(testUpdateRequest);
            
            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getBlockId());
            assertEquals(LocalTime.of(9, 0), response.getStartTime());
            assertEquals(LocalTime.of(13, 0), response.getEndTime());
            assertEquals("Updated reason", response.getReason());
            
            verify(blockedDateRepository).findById(1L);
            verify(blockedDateRepository).save(any(ChefBlockedDate.class));
            verify(modelMapper).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
        }
    }
    
    @Test
    @DisplayName("Test 2: updateBlockedDate with non-existent ID should throw exception")
    void updateBlockedDate_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(blockedDateRepository.findById(99L)).thenReturn(Optional.empty());
        
        testUpdateRequest.setBlockId(99L);
        
        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            chefBlockedDateService.updateBlockedDate(testUpdateRequest);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("not found"));
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
            
            // Set invalid time (end before start)
            testUpdateRequest.setStartTime(LocalTime.of(14, 0));
            testUpdateRequest.setEndTime(LocalTime.of(13, 0));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.updateBlockedDate(testUpdateRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("End time must be after start time"));
        }
    }
    
    @Test
    @DisplayName("Test 4: updateBlockedDate with booking conflict should throw exception")
    void updateBlockedDate_WithBookingConflict_ShouldThrowException() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(testBlockedDate1));
            
            // Mock booking conflict
            when(bookingConflictService.hasBookingConflict(
                    eq(testChef), 
                    any(LocalDate.class), 
                    any(LocalTime.class), 
                    any(LocalTime.class))).thenReturn(true);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.updateBlockedDate(testUpdateRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("booking"));
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
    @DisplayName("Test 4: deleteBlockedDate with already deleted date should not change state")
    void deleteBlockedDate_WithAlreadyDeletedDate_ShouldNotChangeState() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(testUser)).thenReturn(Optional.of(testChef));
            
            ChefBlockedDate deletedBlockedDate = new ChefBlockedDate();
            deletedBlockedDate.setBlockId(3L);
            deletedBlockedDate.setChef(testChef);
            deletedBlockedDate.setBlockedDate(LocalDate.now().plusDays(2));
            deletedBlockedDate.setStartTime(LocalTime.of(10, 0));
            deletedBlockedDate.setEndTime(LocalTime.of(14, 0));
            deletedBlockedDate.setReason("Already deleted");
            deletedBlockedDate.setIsDeleted(true);
            
            when(blockedDateRepository.findById(3L)).thenReturn(Optional.of(deletedBlockedDate));
            
            // Act
            chefBlockedDateService.deleteBlockedDate(3L);
            
            // Assert
            assertTrue(deletedBlockedDate.getIsDeleted());
            verify(blockedDateRepository).findById(3L);
            verify(blockedDateRepository).save(deletedBlockedDate);
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
            when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(eq(testChef), anyInt()))
                .thenReturn(Collections.emptyList());
            when(bookingConflictService.hasBookingConflict(
                    eq(testChef), 
                    any(LocalDate.class), 
                    any(LocalTime.class), 
                    any(LocalTime.class))).thenReturn(false);
            
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(testBlockedDate1);
            when(modelMapper.map(testBlockedDate1, ChefBlockedDateResponse.class)).thenReturn(testBlockedDateResponse1);
            
            // Act
            ChefBlockedDateResponse response = chefBlockedDateService.createBlockedDateForCurrentChef(testBlockedDateRequest);
            
            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getBlockId());
            assertEquals(LocalDate.now(), response.getBlockedDate());
            assertEquals(LocalTime.of(8, 0), response.getStartTime());
            assertEquals(LocalTime.of(12, 0), response.getEndTime());
            assertEquals("Test reason 1", response.getReason());
            
            verify(blockedDateRepository).save(any(ChefBlockedDate.class));
            verify(modelMapper).map(testBlockedDate1, ChefBlockedDateResponse.class);
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
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
            when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(eq(testChef), anyInt()))
                .thenReturn(Collections.emptyList());
            when(bookingConflictService.hasBookingConflict(
                    eq(testChef), 
                    any(LocalDate.class), 
                    any(LocalTime.class), 
                    any(LocalTime.class))).thenReturn(false);
            
            // Mock saved blocked dates
            ChefBlockedDate savedDate1 = new ChefBlockedDate();
            savedDate1.setBlockId(10L);
            savedDate1.setChef(testChef);
            savedDate1.setBlockedDate(LocalDate.now());
            savedDate1.setStartTime(LocalTime.of(10, 0));
            savedDate1.setEndTime(LocalTime.of(16, 0));
            savedDate1.setReason("Range block reason");
            savedDate1.setIsDeleted(false);
            
            ChefBlockedDate savedDate2 = new ChefBlockedDate();
            savedDate2.setBlockId(11L);
            savedDate2.setChef(testChef);
            savedDate2.setBlockedDate(LocalDate.now().plusDays(1));
            savedDate2.setStartTime(LocalTime.of(8, 0));
            savedDate2.setEndTime(LocalTime.of(22, 0));
            savedDate2.setReason("Range block reason");
            savedDate2.setIsDeleted(false);
            
            ChefBlockedDate savedDate3 = new ChefBlockedDate();
            savedDate3.setBlockId(12L);
            savedDate3.setChef(testChef);
            savedDate3.setBlockedDate(LocalDate.now().plusDays(2));
            savedDate3.setStartTime(LocalTime.of(8, 0));
            savedDate3.setEndTime(LocalTime.of(16, 0));
            savedDate3.setReason("Range block reason");
            savedDate3.setIsDeleted(false);
            
            // Mock responses
            ChefBlockedDateResponse response1 = new ChefBlockedDateResponse();
            response1.setBlockId(10L);
            response1.setBlockedDate(LocalDate.now());
            response1.setStartTime(LocalTime.of(10, 0));
            response1.setEndTime(LocalTime.of(16, 0));
            response1.setReason("Range block reason");
            
            ChefBlockedDateResponse response2 = new ChefBlockedDateResponse();
            response2.setBlockId(11L);
            response2.setBlockedDate(LocalDate.now().plusDays(1));
            response2.setStartTime(LocalTime.of(8, 0));
            response2.setEndTime(LocalTime.of(22, 0));
            response2.setReason("Range block reason");
            
            ChefBlockedDateResponse response3 = new ChefBlockedDateResponse();
            response3.setBlockId(12L);
            response3.setBlockedDate(LocalDate.now().plusDays(2));
            response3.setStartTime(LocalTime.of(8, 0));
            response3.setEndTime(LocalTime.of(16, 0));
            response3.setReason("Range block reason");
            
            when(blockedDateRepository.save(any(ChefBlockedDate.class)))
                .thenReturn(savedDate1, savedDate2, savedDate3);
                
            when(modelMapper.map(savedDate1, ChefBlockedDateResponse.class)).thenReturn(response1);
            when(modelMapper.map(savedDate2, ChefBlockedDateResponse.class)).thenReturn(response2);
            when(modelMapper.map(savedDate3, ChefBlockedDateResponse.class)).thenReturn(response3);
            
            // Act
            List<ChefBlockedDateResponse> responses = chefBlockedDateService.createBlockedDateRangeForCurrentChef(testRangeRequest);
            
            // Assert
            assertNotNull(responses);
            assertEquals(3, responses.size());
            assertEquals(10L, responses.get(0).getBlockId());
            assertEquals(11L, responses.get(1).getBlockId());
            assertEquals(12L, responses.get(2).getBlockId());
            
            verify(blockedDateRepository, times(3)).save(any(ChefBlockedDate.class));
            verify(modelMapper, times(3)).map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class));
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
            when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(eq(testChef), anyInt()))
                .thenReturn(Collections.emptyList());
                
            // Mock booking conflict on the first day
            when(bookingConflictService.hasBookingConflict(
                    eq(testChef), 
                    eq(LocalDate.now()), 
                    any(LocalTime.class), 
                    any(LocalTime.class))).thenReturn(true);
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                chefBlockedDateService.createBlockedDateRangeForCurrentChef(testRangeRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("booking"));
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