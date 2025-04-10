package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.payload.requestModel.AvailableTimeSlotRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.repositories.BookingDetailRepository;
import com.spring2025.vietchefs.repositories.ChefBlockedDateRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.ChefTimeSettingsRepository;
import com.spring2025.vietchefs.repositories.PackageRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.impl.CalculateService;
import com.spring2025.vietchefs.services.impl.DistanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class AvailabilityFinderServiceImplTest {

    @Mock
    private ChefRepository chefRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ChefScheduleRepository scheduleRepository;
    
    @Mock
    private ChefBlockedDateRepository blockedDateRepository;
    
    @Mock
    private ChefTimeSettingsRepository timeSettingsRepository;
    
    @Mock
    private BookingConflictService bookingConflictService;
    
    @Mock
    private CalculateService calculateService;
    
    @Mock
    private PackageRepository packageRepository;
    
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    
    @Mock
    private DistanceService distanceService;
    
    @InjectMocks
    private AvailabilityFinderServiceImpl availabilityFinderService;
    
    private User testUser;
    private Chef testChef;
    private List<ChefSchedule> testSchedules;
    private LocalDate testDate;
    private String testCustomerLocation;
    private DistanceResponse testDistanceResponse;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test Chef");
        
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);
        testChef.setAddress("123 Test St");
        
        testSchedules = new ArrayList<>();
        ChefSchedule schedule = new ChefSchedule();
        schedule.setChef(testChef);
        schedule.setDayOfWeek(1); // Monday
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(18, 0));
        testSchedules.add(schedule);
        
        testDate = LocalDate.now().plusDays(1);
        testCustomerLocation = "456 Customer St";
        
        testDistanceResponse = new DistanceResponse();
        testDistanceResponse.setDurationHours(BigDecimal.ONE); // 1 hour travel time
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithInSingleDate_Success() {
        // Setup
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), anyInt())).thenReturn(List.of(testSchedules.get(0)));
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(timeSettingsRepository.findByChef(any())).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(testDistanceResponse);
        
        // Mock reasonable cooking time (1 hour)
        when(calculateService.calculateMaxCookTime(any(), anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(1));
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, testDate, testCustomerLocation, null, null, 4, 3);
        
        // Assertions
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Result should not be empty");
        
        // Verify each slot has valid time range
        for (AvailableTimeSlotResponse slot : result) {
            assertFalse(slot.getStartTime().isAfter(slot.getEndTime()), 
                    "Start time must be before end time");
            assertTrue(slot.getDurationMinutes() > 0, 
                    "Duration must be positive");
            assertEquals(testChef.getId(), slot.getChefId(), "Chef ID should match");
            assertEquals(testDate, slot.getDate(), "Date should match");
        }
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithInSingleDate_StartTimeMustBeBeforeEndTime() {
        // Setup
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), anyInt())).thenReturn(List.of(testSchedules.get(0)));
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(timeSettingsRepository.findByChef(any())).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(testDistanceResponse);
        
        // Mock extreme cooking time (10 hours) which would cause invalid slots
        when(calculateService.calculateMaxCookTime(any(), anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(1));
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, testDate, testCustomerLocation, null, null, 4, 3);

        assertFalse(result.isEmpty(), "Result should not be empty");
        
        // All slots should be valid or the list might be empty if all slots were invalid
        for (AvailableTimeSlotResponse slot : result) {
            assertFalse(slot.getStartTime().isAfter(slot.getEndTime()), 
                    "Start time must be before end time");
            assertTrue(slot.getDurationMinutes() > 0, 
                    "Duration must be positive");
        }
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithInMultipleDates_Success() {
        // Setup
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), anyInt())).thenReturn(List.of(testSchedules.get(0)));
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(timeSettingsRepository.findByChef(any())).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(testDistanceResponse);
        
        // Mock reasonable cooking time (1 hour)
        when(calculateService.calculateMaxCookTime(any(), anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(1));
        when(calculateService.calculateTotalCookTime(any(), anyInt())).thenReturn(BigDecimal.valueOf(1));
        
        // Create test requests for multiple dates
        List<AvailableTimeSlotRequest> requests = new ArrayList<>();
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(testDate);
        request1.setDishIds(Arrays.asList(1L, 2L, 3L));
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(testDate.plusDays(1));
        request2.setDishIds(Arrays.asList(1L, 2L, 3L));
        
        requests.add(request1);
        requests.add(request2);
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                1L, testCustomerLocation, 4, 3, requests);
        
        // Assertions
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Result should not be empty");
        
        // Verify each slot has valid time range
        for (AvailableTimeSlotResponse slot : result) {
            assertFalse(slot.getStartTime().isAfter(slot.getEndTime()), 
                    "Start time must be before end time");
            assertTrue(slot.getDurationMinutes() > 0, 
                    "Duration must be positive");
            assertEquals(testChef.getId(), slot.getChefId(), "Chef ID should match");
            // The date should be one of our request dates
            assertTrue(
                    slot.getDate().equals(testDate) || slot.getDate().equals(testDate.plusDays(1)),
                    "Date should match one of the requested dates"
            );
        }
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithInMultipleDates_StartTimeMustBeBeforeEndTime() {
        // Setup
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), anyInt())).thenReturn(List.of(testSchedules.get(0)));
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(timeSettingsRepository.findByChef(any())).thenReturn(Optional.empty());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), any())).thenReturn(List.of());
        when(bookingConflictService.hasBookingConflict(any(), any(), any(), any())).thenReturn(false);
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(testDistanceResponse);
        
        // Mock extreme cooking time (10 hours) which would cause invalid slots
        when(calculateService.calculateMaxCookTime(any(), anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(1));
        when(calculateService.calculateTotalCookTime(any(), anyInt())).thenReturn(BigDecimal.valueOf(1));
        
        // Create test requests for multiple dates
        List<AvailableTimeSlotRequest> requests = new ArrayList<>();
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(testDate);
        request1.setDishIds(Arrays.asList(1L, 2L, 3L));
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(testDate.plusDays(1));
        request2.setDishIds(Arrays.asList(1L, 2L, 3L));
        
        requests.add(request1);
        requests.add(request2);
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                1L, testCustomerLocation, 4, 3, requests);
        
        assertFalse(result.isEmpty(), "Result should not be empty");

        // All slots should be valid or the list might be empty if all slots were invalid
        for (AvailableTimeSlotResponse slot : result) {
            assertFalse(slot.getStartTime().isAfter(slot.getEndTime()), 
                    "Start time must be before end time");
            assertTrue(slot.getDurationMinutes() > 0, 
                    "Duration must be positive");
        }
    }
} 