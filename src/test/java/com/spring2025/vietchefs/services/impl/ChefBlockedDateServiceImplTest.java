package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefBlockedDate;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateUpdateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.ChefBlockedDateRangeRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ChefBlockedDateResponse;
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
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChefBlockedDateServiceImplTest {

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
    private ChefBlockedDateServiceImpl blockedDateService;

    private User testUser;
    private Chef testChef;
    private ChefBlockedDate testBlockedDate;
    private ChefBlockedDateRequest blockedDateRequest;
    private ChefBlockedDateUpdateRequest updateRequest;
    private ChefBlockedDateResponse blockedDateResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test Chef User");

        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);

        LocalDate blockedDate = LocalDate.of(2023, 7, 15);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(14, 0);

        testBlockedDate = new ChefBlockedDate();
        testBlockedDate.setBlockId(1L);
        testBlockedDate.setChef(testChef);
        testBlockedDate.setBlockedDate(blockedDate);
        testBlockedDate.setStartTime(startTime);
        testBlockedDate.setEndTime(endTime);
        testBlockedDate.setReason("Vacation");
        testBlockedDate.setIsDeleted(false);

        blockedDateRequest = new ChefBlockedDateRequest();
        blockedDateRequest.setBlockedDate(LocalDate.of(2023, 8, 20));
        blockedDateRequest.setStartTime(LocalTime.of(12, 0));
        blockedDateRequest.setEndTime(LocalTime.of(18, 0));
        blockedDateRequest.setReason("Family event");

        updateRequest = new ChefBlockedDateUpdateRequest();
        updateRequest.setBlockId(1L);
        updateRequest.setBlockedDate(LocalDate.of(2023, 7, 16));
        updateRequest.setStartTime(LocalTime.of(9, 0));
        updateRequest.setEndTime(LocalTime.of(15, 0));
        updateRequest.setReason("Extended vacation");

        blockedDateResponse = new ChefBlockedDateResponse();
        blockedDateResponse.setBlockId(1L);
        blockedDateResponse.setBlockedDate(blockedDate);
        blockedDateResponse.setStartTime(startTime);
        blockedDateResponse.setEndTime(endTime);
        blockedDateResponse.setReason("Vacation");
    }

    @Test
    void getBlockedDateById_ShouldReturnBlockedDate_WhenBlockedDateExists() {
        // Arrange
        when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.of(testBlockedDate));
        when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(blockedDateResponse);

        // Act
        ChefBlockedDateResponse result = blockedDateService.getBlockedDateById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(blockedDateResponse.getBlockId(), result.getBlockId());
        assertEquals(blockedDateResponse.getBlockedDate(), result.getBlockedDate());
        verify(blockedDateRepository).findById(1L);
    }

    @Test
    void getBlockedDateById_ShouldThrowException_WhenBlockedDateNotFound() {
        // Arrange
        when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            blockedDateService.getBlockedDateById(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateBlockedDate_ShouldReturnUpdatedBlockedDate_WhenValidAndOwner() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.of(testBlockedDate));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                    .thenReturn(List.of());
            when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(testBlockedDate);
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(blockedDateResponse);

            // Act
            ChefBlockedDateResponse result = blockedDateService.updateBlockedDate(updateRequest);

            // Assert
            assertNotNull(result);
            assertEquals(blockedDateResponse.getBlockId(), result.getBlockId());
            verify(blockedDateRepository).save(testBlockedDate);
            
            // Verify the blocked date was updated
            assertEquals(updateRequest.getBlockedDate(), testBlockedDate.getBlockedDate());
            assertEquals(updateRequest.getStartTime(), testBlockedDate.getStartTime());
            assertEquals(updateRequest.getEndTime(), testBlockedDate.getEndTime());
            assertEquals(updateRequest.getReason(), testBlockedDate.getReason());
        }
    }

    @Test
    void updateBlockedDate_ShouldThrowException_WhenBlockedDateNotFound() {
        // Arrange
        when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            blockedDateService.updateBlockedDate(updateRequest);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateBlockedDate_ShouldThrowException_WhenNotOwner() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Create a different chef as owner
            Chef differentChef = new Chef();
            differentChef.setId(2L);
            
            ChefBlockedDate blockedDate = new ChefBlockedDate();
            blockedDate.setBlockId(1L);
            blockedDate.setChef(differentChef);
            
            when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.of(blockedDate));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.updateBlockedDate(updateRequest);
            });
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Test
    void deleteBlockedDate_ShouldMarkAsDeleted_WhenOwner() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.of(testBlockedDate));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(testBlockedDate);

            // Act
            blockedDateService.deleteBlockedDate(1L);

            // Assert
            assertTrue(testBlockedDate.getIsDeleted());
            verify(blockedDateRepository).save(testBlockedDate);
        }
    }

    @Test
    void deleteBlockedDate_ShouldThrowException_WhenBlockedDateNotFound() {
        // Arrange
        when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            blockedDateService.deleteBlockedDate(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void deleteBlockedDate_ShouldThrowException_WhenNotOwner() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Create a different chef as owner
            Chef differentChef = new Chef();
            differentChef.setId(2L);
            
            ChefBlockedDate blockedDate = new ChefBlockedDate();
            blockedDate.setBlockId(1L);
            blockedDate.setChef(differentChef);
            
            when(blockedDateRepository.findById(anyLong())).thenReturn(Optional.of(blockedDate));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.deleteBlockedDate(1L);
            });
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Test
    void createBlockedDateForCurrentChef_ShouldReturnCreatedBlockedDate_WhenValid() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                    .thenReturn(List.of());
            when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(testBlockedDate);
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(blockedDateResponse);

            // Act
            ChefBlockedDateResponse result = blockedDateService.createBlockedDateForCurrentChef(blockedDateRequest);

            // Assert
            assertNotNull(result);
            verify(blockedDateRepository).save(any(ChefBlockedDate.class));
        }
    }

    @Test
    void createBlockedDateForCurrentChef_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.createBlockedDateForCurrentChef(blockedDateRequest);
            });
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }

    @Test
    void createBlockedDateForCurrentChef_ShouldThrowException_WhenChefNotFound() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.empty());

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.createBlockedDateForCurrentChef(blockedDateRequest);
            });
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }

    @Test
    void getBlockedDatesForCurrentChef_ShouldReturnBlockedDateList() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findByChefAndIsDeletedFalse(any(Chef.class)))
                    .thenReturn(Arrays.asList(testBlockedDate));
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(blockedDateResponse);

            // Act
            List<ChefBlockedDateResponse> result = blockedDateService.getBlockedDatesForCurrentChef();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(blockedDateRepository).findByChefAndIsDeletedFalse(testChef);
        }
    }

    @Test
    void getBlockedDatesForCurrentChefBetween_ShouldReturnBlockedDateListInRange() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            LocalDate startDate = LocalDate.of(2023, 7, 1);
            LocalDate endDate = LocalDate.of(2023, 7, 31);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findByChefAndBlockedDateBetweenAndIsDeletedFalse(
                    any(Chef.class), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testBlockedDate));
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(blockedDateResponse);

            // Act
            List<ChefBlockedDateResponse> result = blockedDateService.getBlockedDatesForCurrentChefBetween(startDate, endDate);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(blockedDateRepository).findByChefAndBlockedDateBetweenAndIsDeletedFalse(testChef, startDate, endDate);
        }
    }

    @Test
    void getBlockedDatesForCurrentChefByDate_ShouldReturnBlockedDateListForDate() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            LocalDate date = LocalDate.of(2023, 7, 15);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(
                    any(Chef.class), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(testBlockedDate));
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(blockedDateResponse);

            // Act
            List<ChefBlockedDateResponse> result = blockedDateService.getBlockedDatesForCurrentChefByDate(date);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(blockedDateRepository).findByChefAndBlockedDateAndIsDeletedFalse(testChef, date);
        }
    }

    @Test
    void createBlockedDateForCurrentChef_ShouldThrowException_WhenOverlappingWithExistingBlockedDate() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            
            // Create a request with a time that overlaps with an existing blocked date
            ChefBlockedDateRequest overlappingRequest = new ChefBlockedDateRequest();
            overlappingRequest.setBlockedDate(LocalDate.of(2023, 7, 15)); // Same date as testBlockedDate
            overlappingRequest.setStartTime(LocalTime.of(9, 0));  // Overlaps with testBlockedDate (10:00-14:00)
            overlappingRequest.setEndTime(LocalTime.of(11, 0));
            overlappingRequest.setReason("Overlapping event");
            
            // Create a list with the existing blocked date
            List<ChefBlockedDate> existingBlockedDates = List.of(testBlockedDate);
            
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(Chef.class), any(LocalDate.class)))
                    .thenReturn(existingBlockedDates);

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.createBlockedDateForCurrentChef(overlappingRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Time slot conflicts with an existing blocked date"));
        }
    }
    
    @Test
    void updateBlockedDate_ShouldThrowException_WhenModifiedTimeOverlapsWithExistingBlockedDate() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Create first blocked date (the one being updated)
            ChefBlockedDate firstBlockedDate = new ChefBlockedDate();
            firstBlockedDate.setBlockId(1L);
            firstBlockedDate.setChef(testChef);
            firstBlockedDate.setBlockedDate(LocalDate.of(2023, 7, 16));
            firstBlockedDate.setStartTime(LocalTime.of(9, 0));
            firstBlockedDate.setEndTime(LocalTime.of(10, 0));
            
            // Create second blocked date (already in the system)
            ChefBlockedDate secondBlockedDate = new ChefBlockedDate();
            secondBlockedDate.setBlockId(2L);
            secondBlockedDate.setChef(testChef);
            secondBlockedDate.setBlockedDate(LocalDate.of(2023, 7, 16)); // Same date
            secondBlockedDate.setStartTime(LocalTime.of(11, 0));
            secondBlockedDate.setEndTime(LocalTime.of(14, 0));
            
            // Create update request that would extend the first blocked date to overlap with the second
            ChefBlockedDateUpdateRequest updateRequest = new ChefBlockedDateUpdateRequest();
            updateRequest.setBlockId(1L);
            updateRequest.setBlockedDate(LocalDate.of(2023, 7, 16));
            updateRequest.setStartTime(LocalTime.of(9, 0));
            updateRequest.setEndTime(LocalTime.of(12, 0)); // Now extends into second blocked date's time
            
            when(blockedDateRepository.findById(1L)).thenReturn(Optional.of(firstBlockedDate));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            
            // Return both blocked dates when checking for conflicts
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(
                    eq(testChef), eq(LocalDate.of(2023, 7, 16))))
                    .thenReturn(List.of(firstBlockedDate, secondBlockedDate));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.updateBlockedDate(updateRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Time slot conflicts with an existing blocked date"));
            
            // Verify that the repository methods were called correctly
            verify(blockedDateRepository).findById(1L);
            verify(blockedDateRepository).findByChefAndBlockedDateAndIsDeletedFalse(
                    eq(testChef), eq(LocalDate.of(2023, 7, 16)));
        }
    }

    @Test
    void createBlockedDateForCurrentChef_ShouldThrowException_WhenCompletelyOverlapsWithExistingBlockedDate() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            
            // Existing blocked date
            ChefBlockedDate existingBlockedDate = new ChefBlockedDate();
            existingBlockedDate.setBlockId(1L);
            existingBlockedDate.setChef(testChef);
            existingBlockedDate.setBlockedDate(LocalDate.of(2023, 7, 15));
            existingBlockedDate.setStartTime(LocalTime.of(10, 0));
            existingBlockedDate.setEndTime(LocalTime.of(14, 0));
            
            // Create a request with exactly the same time as existing blocked date
            ChefBlockedDateRequest duplicateRequest = new ChefBlockedDateRequest();
            duplicateRequest.setBlockedDate(LocalDate.of(2023, 7, 15));
            duplicateRequest.setStartTime(LocalTime.of(10, 0));  // Same as existing
            duplicateRequest.setEndTime(LocalTime.of(14, 0));    // Same as existing
            duplicateRequest.setReason("Duplicate event");
            
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(
                    eq(testChef), eq(LocalDate.of(2023, 7, 15))))
                    .thenReturn(List.of(existingBlockedDate));

            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.createBlockedDateForCurrentChef(duplicateRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Time slot conflicts with an existing blocked date"));
            
            // Verify repository was called with correct parameters
            verify(blockedDateRepository).findByChefAndBlockedDateAndIsDeletedFalse(
                    eq(testChef), eq(LocalDate.of(2023, 7, 15)));
        }
    }

    @Test
    void createBlockedDateRangeForCurrentChef_ShouldCreateMultipleBlockedDates_WhenValidRangeProvided() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Setup test dates
            LocalDate startDate = LocalDate.of(2023, 7, 15);
            LocalDate endDate = LocalDate.of(2023, 7, 17);
            LocalTime startTime = LocalTime.of(9, 0);
            LocalTime endTime = LocalTime.of(14, 0);
            
            ChefBlockedDateRangeRequest rangeRequest = new ChefBlockedDateRangeRequest();
            rangeRequest.setStartDate(startDate);
            rangeRequest.setEndDate(endDate);
            rangeRequest.setStartTime(startTime);
            rangeRequest.setEndTime(endTime);
            rangeRequest.setReason("Vacation");
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                    .thenReturn(List.of());
            when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(Chef.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            
            // Setup saved blocked dates
            ChefBlockedDate savedDay1 = new ChefBlockedDate();
            savedDay1.setBlockId(1L);
            savedDay1.setChef(testChef);
            savedDay1.setBlockedDate(startDate);
            savedDay1.setStartTime(startTime);
            savedDay1.setEndTime(LocalTime.of(22, 0)); // MAX_WORK_HOUR
            savedDay1.setReason("Vacation");
            
            ChefBlockedDate savedDay2 = new ChefBlockedDate();
            savedDay2.setBlockId(2L);
            savedDay2.setChef(testChef);
            savedDay2.setBlockedDate(startDate.plusDays(1));
            savedDay2.setStartTime(LocalTime.of(8, 0)); // MIN_WORK_HOUR
            savedDay2.setEndTime(LocalTime.of(22, 0)); // MAX_WORK_HOUR
            savedDay2.setReason("Vacation");
            
            ChefBlockedDate savedDay3 = new ChefBlockedDate();
            savedDay3.setBlockId(3L);
            savedDay3.setChef(testChef);
            savedDay3.setBlockedDate(endDate);
            savedDay3.setStartTime(LocalTime.of(8, 0)); // MIN_WORK_HOUR
            savedDay3.setEndTime(endTime);
            savedDay3.setReason("Vacation");
            
            // Setup responses
            ChefBlockedDateResponse response1 = new ChefBlockedDateResponse();
            response1.setBlockId(1L);
            
            ChefBlockedDateResponse response2 = new ChefBlockedDateResponse();
            response2.setBlockId(2L);
            
            ChefBlockedDateResponse response3 = new ChefBlockedDateResponse();
            response3.setBlockId(3L);
            
            // Mock the save operation with a simple implementation
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenAnswer(invocation -> {
                ChefBlockedDate date = invocation.getArgument(0);
                if (date.getBlockedDate().isEqual(startDate)) {
                    return savedDay1;
                } else if (date.getBlockedDate().isEqual(startDate.plusDays(1))) {
                    return savedDay2;
                } else if (date.getBlockedDate().isEqual(endDate)) {
                    return savedDay3;
                }
                return new ChefBlockedDate(); // Fallback
            });
            
            // Mock mapping of saved objects to responses
            when(modelMapper.map(eq(savedDay1), eq(ChefBlockedDateResponse.class))).thenReturn(response1);
            when(modelMapper.map(eq(savedDay2), eq(ChefBlockedDateResponse.class))).thenReturn(response2);
            when(modelMapper.map(eq(savedDay3), eq(ChefBlockedDateResponse.class))).thenReturn(response3);
            
            // Act
            List<ChefBlockedDateResponse> results = blockedDateService.createBlockedDateRangeForCurrentChef(rangeRequest);
            
            // Assert
            assertNotNull(results);
            assertEquals(3, results.size());
            
            // Verify the service interacted with the repository 3 times (once for each day)
            verify(blockedDateRepository, times(3)).save(any(ChefBlockedDate.class));
            
            // Verify date, time ranges with explicit argument captors
            ArgumentCaptor<ChefBlockedDate> dateCaptor = ArgumentCaptor.forClass(ChefBlockedDate.class);
            verify(blockedDateRepository, times(3)).save(dateCaptor.capture());
            
            List<ChefBlockedDate> capturedDates = dateCaptor.getAllValues();
            assertEquals(3, capturedDates.size());
            
            // First day (startDate)
            ChefBlockedDate firstDay = capturedDates.stream()
                .filter(d -> d.getBlockedDate().equals(startDate))
                .findFirst()
                .orElse(null);
            assertNotNull(firstDay);
            assertEquals(startTime, firstDay.getStartTime());
            assertEquals(LocalTime.of(22, 0), firstDay.getEndTime());
            
            // Middle day
            ChefBlockedDate middleDay = capturedDates.stream()
                .filter(d -> d.getBlockedDate().equals(startDate.plusDays(1)))
                .findFirst()
                .orElse(null);
            assertNotNull(middleDay);
            assertEquals(LocalTime.of(8, 0), middleDay.getStartTime());
            assertEquals(LocalTime.of(22, 0), middleDay.getEndTime());
            
            // Last day (endDate)
            ChefBlockedDate lastDay = capturedDates.stream()
                .filter(d -> d.getBlockedDate().equals(endDate))
                .findFirst()
                .orElse(null);
            assertNotNull(lastDay);
            assertEquals(LocalTime.of(8, 0), lastDay.getStartTime());
            assertEquals(endTime, lastDay.getEndTime());
        }
    }

    @Test
    void createBlockedDateRangeForCurrentChef_ShouldCreateSingleBlockedDate_WhenStartAndEndDateAreSame() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Setup test dates - same start and end date
            LocalDate singleDate = LocalDate.of(2023, 7, 15);
            LocalTime startTime = LocalTime.of(9, 0);
            LocalTime endTime = LocalTime.of(14, 0);
            
            ChefBlockedDateRangeRequest rangeRequest = new ChefBlockedDateRangeRequest();
            rangeRequest.setStartDate(singleDate);
            rangeRequest.setEndDate(singleDate);
            rangeRequest.setStartTime(startTime);
            rangeRequest.setEndTime(endTime);
            rangeRequest.setReason("Single day off");
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), anyInt()))
                    .thenReturn(List.of());
            when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(Chef.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            
            // Setup saved blocked date
            ChefBlockedDate savedDate = new ChefBlockedDate();
            savedDate.setBlockId(1L);
            savedDate.setChef(testChef);
            savedDate.setBlockedDate(singleDate);
            savedDate.setStartTime(startTime);
            savedDate.setEndTime(endTime);
            savedDate.setReason("Single day off");
            
            // Setup response
            ChefBlockedDateResponse response = new ChefBlockedDateResponse();
            response.setBlockId(1L);
            
            when(blockedDateRepository.save(any(ChefBlockedDate.class))).thenReturn(savedDate);
            when(modelMapper.map(any(ChefBlockedDate.class), eq(ChefBlockedDateResponse.class))).thenReturn(response);
            
            // Act
            List<ChefBlockedDateResponse> results = blockedDateService.createBlockedDateRangeForCurrentChef(rangeRequest);
            
            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
            
            // Verify exactly one blocked date was created
            verify(blockedDateRepository, times(1)).save(any(ChefBlockedDate.class));
            
            // Verify the single day uses the exact start and end times from the request
            verify(blockedDateRepository).save(argThat(date -> 
                date.getBlockedDate().equals(singleDate) &&
                date.getStartTime().equals(startTime) &&
                date.getEndTime().equals(endTime)));
        }
    }

    @Test
    void createBlockedDateRangeForCurrentChef_ShouldThrowException_WhenStartDateAfterEndDate() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Setup invalid date range (start date after end date)
            LocalDate startDate = LocalDate.of(2023, 7, 20);
            LocalDate endDate = LocalDate.of(2023, 7, 15);
            
            ChefBlockedDateRangeRequest invalidRangeRequest = new ChefBlockedDateRangeRequest();
            invalidRangeRequest.setStartDate(startDate);
            invalidRangeRequest.setEndDate(endDate);
            invalidRangeRequest.setStartTime(LocalTime.of(9, 0));
            invalidRangeRequest.setEndTime(LocalTime.of(14, 0));
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.createBlockedDateRangeForCurrentChef(invalidRangeRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Start date must be before or equal to end date"));
            
            // Verify no save operations were attempted
            verify(blockedDateRepository, never()).save(any(ChefBlockedDate.class));
        }
    }

    @Test
    void createBlockedDateRangeForCurrentChef_ShouldThrowException_WhenTimeConstraintViolated() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Time outside allowed working hours (8:00-22:00)
            LocalTime invalidStartTime = LocalTime.of(6, 0); // Before 8:00
            
            ChefBlockedDateRangeRequest invalidTimeRequest = new ChefBlockedDateRangeRequest();
            invalidTimeRequest.setStartDate(LocalDate.of(2023, 7, 15));
            invalidTimeRequest.setEndDate(LocalDate.of(2023, 7, 17));
            invalidTimeRequest.setStartTime(invalidStartTime);
            invalidTimeRequest.setEndTime(LocalTime.of(14, 0));
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.createBlockedDateRangeForCurrentChef(invalidTimeRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("Blocked time must be between"));
            
            // Verify no save operations were attempted
            verify(blockedDateRepository, never()).save(any(ChefBlockedDate.class));
        }
    }

    @Test
    void createBlockedDateRangeForCurrentChef_ShouldThrowException_WhenConflictExists() {
        // Arrange
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            
            // Setup test dates
            LocalDate startDate = LocalDate.of(2023, 7, 15);
            LocalDate endDate = LocalDate.of(2023, 7, 17);
            
            ChefBlockedDateRangeRequest rangeRequest = new ChefBlockedDateRangeRequest();
            rangeRequest.setStartDate(startDate);
            rangeRequest.setEndDate(endDate);
            rangeRequest.setStartTime(LocalTime.of(9, 0));
            rangeRequest.setEndTime(LocalTime.of(14, 0));
            
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
            when(chefRepository.findByUser(any(User.class))).thenReturn(Optional.of(testChef));
            
            // Create an existing blocked date that conflicts
            ChefBlockedDate existingBlockedDate = new ChefBlockedDate();
            existingBlockedDate.setBlockId(5L);
            existingBlockedDate.setChef(testChef);
            existingBlockedDate.setBlockedDate(startDate);
            existingBlockedDate.setStartTime(LocalTime.of(10, 0));
            existingBlockedDate.setEndTime(LocalTime.of(13, 0));
            
            when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(testChef), eq(startDate)))
                    .thenReturn(List.of(existingBlockedDate));
            
            // Act & Assert
            VchefApiException exception = assertThrows(VchefApiException.class, () -> {
                blockedDateService.createBlockedDateRangeForCurrentChef(rangeRequest);
            });
            
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            assertTrue(exception.getMessage().contains("conflicts with an existing blocked date"));
            
            // Verify no save operations were attempted
            verify(blockedDateRepository, never()).save(any(ChefBlockedDate.class));
        }
    }
}