package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import com.spring2025.vietchefs.models.entity.ChefTimeSettings;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;
import com.spring2025.vietchefs.repositories.BookingDetailRepository;
import com.spring2025.vietchefs.repositories.ChefBlockedDateRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.ChefScheduleRepository;
import com.spring2025.vietchefs.repositories.ChefTimeSettingsRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.BookingConflictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private BookingDetailRepository bookingDetailRepository;

    @InjectMocks
    private AvailabilityFinderServiceImpl availabilityFinderService;

    private Chef testChef;
    private User testUser;
    private LocalDate testDate;
    private List<ChefSchedule> testSchedules;
    private ChefTimeSettings testTimeSettings;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test Chef");
        
        // Setup test data
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);
        testChef.setAddress("Test Address");
        testChef.setPrice(BigDecimal.valueOf(50));
        testChef.setStatus("ACTIVE");
        testChef.setCountry("USA");
        
        testDate = LocalDate.now().plusDays(1);
        
        // Create test schedule
        testSchedules = new ArrayList<>();
        ChefSchedule schedule = new ChefSchedule();
        schedule.setChef(testChef);
        schedule.setDayOfWeek(testDate.getDayOfWeek().getValue() % 7);
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(18, 0));
        schedule.setIsDeleted(false);
        testSchedules.add(schedule);
        
        // Create test time settings
        testTimeSettings = new ChefTimeSettings();
        testTimeSettings.setChef(testChef);
        testTimeSettings.setStandardPrepTime(30);
        testTimeSettings.setStandardCleanupTime(30);
    }

    @Test
    public void testFindAvailableTimeSlotsWithCookingTime() {
        // Setup test data
//        Long chefId = 1L;
//        Long menuId = 100L;
//        List<Long> dishIds = List.of(1L, 2L, 3L);
//        int guestCount = 6;
//        Integer minDuration = 120;
//
//        // Mock repository calls
//        when(chefRepository.findById(eq(chefId))).thenReturn(Optional.of(testChef));
//        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(eq(testChef), anyInt()))
//                .thenReturn(testSchedules);
//        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(testChef), eq(testDate)))
//                .thenReturn(Collections.emptyList());
//        when(timeSettingsRepository.findByChef(eq(testChef))).thenReturn(Optional.of(testTimeSettings));
//        when(bookingConflictService.hasBookingConflict(eq(testChef), eq(testDate), any(LocalTime.class), any(LocalTime.class)))
//                .thenReturn(false);
//        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), eq(testDate)))
//                .thenReturn(Collections.emptyList());
//
//        // Mock calculate service for cooking time (0.5 hour = 30 minutes)
//        when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId), eq(dishIds), eq(guestCount)))
//                .thenReturn(BigDecimal.valueOf(0.5));
//
//        // Call the method under test
//        List<AvailableTimeSlotResponse> result = availabilityFinderService
//                .findAvailableTimeSlotsWithCookingTime(chefId, testDate, menuId, dishIds, guestCount,packageId, minDuration);
//
//        // Assertions
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//
//        // Check that each slot has been adjusted by cooking time (30 min)
//        for (AvailableTimeSlotResponse slot : result) {
//            assertTrue(slot.getStartTime().isAfter(LocalTime.of(8, 0)));
//            assertTrue(slot.getNote().contains("Cooking starts at"));
//            assertTrue(slot.getNote().contains("service begins at"));
//        }
    }

    @Test
    public void testFindAvailableTimeSlotsWithCookingTimeWhenNoSlotsAvailable() {
//        // Setup test data
//        Long chefId = 1L;
//        Long menuId = 100L;
//        List<Long> dishIds = List.of(1L, 2L, 3L);
//        int guestCount = 6;
//        Integer minDuration = 120;
//
//        // Mock repository calls
//        when(chefRepository.findById(eq(chefId))).thenReturn(Optional.of(testChef));
//        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(eq(testChef), anyInt()))
//                .thenReturn(Collections.emptyList()); // No schedules for this day
//
//        when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId), eq(dishIds), eq(guestCount)))
//                .thenReturn(BigDecimal.ZERO);
//
//        // Call the method under test
//        List<AvailableTimeSlotResponse> result = availabilityFinderService
//                .findAvailableTimeSlotsWithCookingTime(chefId, testDate, menuId, dishIds, guestCount, minDuration);
//
//        // Assertions
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAvailableTimeSlotsWithCookingTimeWhenCookingTimeTooLong() {
//        // Setup test data
//        Long chefId = 1L;
//        Long menuId = 100L;
//        List<Long> dishIds = List.of(1L, 2L, 3L);
//        int guestCount = 6;
//        Integer minDuration = 120;
//
//        // Mock repository calls
//        when(chefRepository.findById(eq(chefId))).thenReturn(Optional.of(testChef));
//        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(eq(testChef), anyInt()))
//                .thenReturn(testSchedules);
//        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(testChef), eq(testDate)))
//                .thenReturn(Collections.emptyList());
//        when(timeSettingsRepository.findByChef(eq(testChef))).thenReturn(Optional.of(testTimeSettings));
//        when(bookingConflictService.hasBookingConflict(eq(testChef), eq(testDate), any(LocalTime.class), any(LocalTime.class)))
//                .thenReturn(false);
//        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), eq(testDate)))
//                .thenReturn(Collections.emptyList());
//
//        // Mock calculate service for a very long cooking time (8 hours)
//        when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId), eq(dishIds), eq(guestCount)))
//                .thenReturn(BigDecimal.valueOf(8.0));
//
//        // Call the method under test
//        List<AvailableTimeSlotResponse> result = availabilityFinderService
//                .findAvailableTimeSlotsWithCookingTime(chefId, testDate, menuId, dishIds, guestCount, minDuration);
//
//        // Assertions - all slots should be filtered out
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithTravelTimeBuffer() {
//        // Setup test data
//        Long chefId = 1L;
//        Long menuId = 100L;
//        List<Long> dishIds = List.of(1L, 2L, 3L);
//        int guestCount = 6;
//        Integer minDuration = 120;
//
//        // Create a booking with travel time starting at 17:00 and service starting at 18:00
//        Booking existingBooking = new Booking();
//        existingBooking.setId(1L);
//        existingBooking.setChef(testChef);
//        existingBooking.setStatus("CONFIRMED");
//        existingBooking.setIsDeleted(false);
//
//        BookingDetail bookingDetail = new BookingDetail();
//        bookingDetail.setId(1L);
//        bookingDetail.setBooking(existingBooking);
//        bookingDetail.setStatus("CONFIRMED");
//        bookingDetail.setSessionDate(testDate);
//        bookingDetail.setStartTime(LocalTime.of(18, 0)); // Service starts at 18:00
//        bookingDetail.setTimeBeginTravel(LocalTime.of(17, 0)); // Travel starts at 17:00
//        bookingDetail.setIsDeleted(false);
//
//        List<BookingDetail> bookingDetails = Collections.singletonList(bookingDetail);
//
//        // Mock repository calls
//        when(chefRepository.findById(eq(chefId))).thenReturn(Optional.of(testChef));
//        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(eq(testChef), anyInt()))
//                .thenReturn(testSchedules);
//        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(testChef), eq(testDate)))
//                .thenReturn(Collections.emptyList());
//        when(timeSettingsRepository.findByChef(eq(testChef))).thenReturn(Optional.of(testTimeSettings));
//        when(bookingConflictService.hasBookingConflict(eq(testChef), eq(testDate), any(LocalTime.class), any(LocalTime.class)))
//                .thenReturn(false);
//        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), eq(testDate)))
//                .thenReturn(bookingDetails);
//
//        // Mock calculate service for cooking time (0.5 hour = 30 minutes)
//        when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId), eq(dishIds), eq(guestCount)))
//                .thenReturn(BigDecimal.valueOf(0.5));
//
//        // Call the method under test
//        List<AvailableTimeSlotResponse> result = availabilityFinderService
//                .findAvailableTimeSlotsWithCookingTime(chefId, testDate, menuId, dishIds, guestCount, minDuration);
//
//        // Assertions
//        assertNotNull(result);
//        assertFalse(result.isEmpty());
//
//
//        // Verify that no slots end after 16:00 (17:00 travel time minus 1 hour buffer)
//        boolean allSlotsEndBeforeTravelTime = result.stream()
//                .allMatch(slot -> !slot.getEndTime().isAfter(LocalTime.of(16, 0)));
//
//        assertTrue(allSlotsEndBeforeTravelTime, "All slots should end at least 1 hour before travel start time");
    }
} 