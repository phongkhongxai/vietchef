package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.AvailableTimeSlotRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingConflictService;
import com.spring2025.vietchefs.services.impl.*;
import com.spring2025.vietchefs.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class AvailabilityFinderServiceTest {


    @Mock
    private ChefRepository chefRepository;

    @Mock
    private ChefScheduleRepository scheduleRepository;

    @Mock
    private ChefBlockedDateRepository blockedDateRepository;

    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PackageRepository packageRepository;

    @Mock
    private BookingConflictService bookingConflictService;

    @Mock
    private DistanceService distanceService;

    @Mock
    private CalculateService calculateService;

    @Mock
    private TimeZoneService timeZoneService;

    @InjectMocks
    private AvailabilityFinderServiceImpl availabilityFinderService;

    // Test data
    private User user;
    private Chef chef;
    private List<ChefSchedule> schedules;
    private List<ChefBlockedDate> blockedDates;
    private List<BookingDetail> bookings;

    @BeforeEach
    void setUp() {
        // Initialize common test data
        user = new User();
        user.setId(1L);
        // Not setting user.name to avoid linter errors

        chef = new Chef();
        chef.setId(1L);
        chef.setUser(user);
        chef.setAddress("123 Lê Lợi, Quận 1, TP.HCM, Việt Nam");

        // Create chef schedules (working hours)
        schedules = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            ChefSchedule schedule = new ChefSchedule();
            schedule.setId((long) (day + 1));
            schedule.setChef(chef);
            schedule.setDayOfWeek(day);
            schedule.setStartTime(LocalTime.of(8, 0));  // 8:00 AM
            schedule.setEndTime(LocalTime.of(22, 0));   // 10:00 PM
            schedule.setIsDeleted(false);
            schedules.add(schedule);
        }

        // Create some blocked dates
        blockedDates = new ArrayList<>();
        ChefBlockedDate blockedDate = new ChefBlockedDate();
        blockedDate.setBlockId(1L);
        blockedDate.setChef(chef);
        blockedDate.setBlockedDate(LocalDate.now().plusDays(2));
        blockedDate.setStartTime(LocalTime.of(12, 0));
        blockedDate.setEndTime(LocalTime.of(14, 0));
        blockedDate.setIsDeleted(false);
        blockedDates.add(blockedDate);

        // Create some existing bookings
        bookings = new ArrayList<>();
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setChef(chef);
        booking.setStatus("CONFIRMED");
        booking.setIsDeleted(false);

        BookingDetail bookingDetail = new BookingDetail();
        bookingDetail.setId(1L);
        bookingDetail.setBooking(booking);
        bookingDetail.setSessionDate(LocalDate.now().plusDays(1));
        bookingDetail.setTimeBeginTravel(LocalTime.of(16, 0));
        bookingDetail.setTimeBeginCook(LocalTime.of(17, 0));
        bookingDetail.setStartTime(LocalTime.of(19, 0));
        bookingDetail.setStatus("CONFIRMED");
        bookingDetail.setIsDeleted(false);
        bookings.add(bookingDetail);
    }

    // Test Group 1: findAvailableTimeSlotsWithInSingleDate
    
    @Test
    @DisplayName("1.1 Should find available slots with cooking and travel time calculated")
    void findAvailableTimeSlotsWithInSingleDateBasic() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(5);
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        String chefAddress = "123 Lê Lợi, Quận 1, TP.HCM, Việt Nam";
        Long menuId = 1L;
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        
        // Prepare current request time for 24h rule
        LocalDateTime requestTime = LocalDateTime.now();
        
        // Prepare timezone data - Vietnam is in Asia/Ho_Chi_Minh timezone (UTC+7)
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone since both addresses are in Vietnam
        
        // Prepare distance service response - realistic travel time in HCMC
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5)); // 2.5 km between Lê Lợi and Nguyễn Huệ
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25)); // 15 minutes travel time in HCMC traffic
        
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Return schedule for the day
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek)));
        
        // No blocked dates or bookings
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        
        // Mock distance service with realistic data
        when(distanceService.calculateDistanceAndTime(chef.getAddress(), customerLocation))
                .thenReturn(distanceResponse);
        
        // Mock timezone service - realistic for Vietnam addresses
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        
        // Mock timezone conversion - same timezone for both locations in Vietnam
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> {
                    LocalDateTime dt = invocation.getArgument(0);
                    return dt; // No timezone difference
                });
        
        // Mock cooking time calculation - 60 minutes for selected menu
        doReturn(BigDecimal.valueOf(1.0)).when(calculateService).calculateTotalCookTimeFromMenu(
                eq(menuId), any(), eq(guestCount)); // 1 hour
        
        // Act
        System.out.println("=========== TEST 1.1: Basic Slot Finding with Timezone and 24h Rule ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", menuId=" + menuId + ", guestCount=" + guestCount);
        System.out.println("Chef timezone: " + chefTimezone + ", Customer timezone: " + customerTimezone);
        System.out.println("Current request time: " + requestTime);
        System.out.println("24h threshold: " + requestTime.plusHours(24));
        System.out.println("Expected: ");
        System.out.println("1. Slots with at least 75 minutes duration (15min travel + 60min cooking)");
        System.out.println("2. No timezone conversion needed as both are in same timezone");
        System.out.println("3. Only slots that start after 24h from request time");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                chef.getId(), date, customerLocation, menuId, null, guestCount, maxDishesPerMeal);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            LocalDateTime slotDateTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
            
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime() + 
                    ", duration=" + slot.getDurationMinutes() + " minutes");
            
            // Verify slot starts at least 24h after request time
            boolean isAfter24hThreshold = slotDateTime.isAfter(requestTime.plusHours(24));
            System.out.println("Slot starts after 24h threshold: " + isAfter24hThreshold + 
                    " (expected: true)");
            assertTrue(isAfter24hThreshold, "Slot should start at least 24h after request time");
            
            // Verify the slot is within the chef's schedule
            boolean startTimeWithinSchedule = 
                    slot.getStartTime().isAfter(LocalTime.of(8, 0)) || 
                    slot.getStartTime().equals(LocalTime.of(8, 0));
            boolean endTimeWithinSchedule = 
                    slot.getEndTime().isBefore(LocalTime.of(22, 0)) || 
                    slot.getEndTime().equals(LocalTime.of(22, 0));
            
            System.out.println("Start time within schedule (8:00-22:00): " + 
                    startTimeWithinSchedule + " (expected: true)");
            System.out.println("End time within schedule (8:00-22:00): " + 
                    endTimeWithinSchedule + " (expected: true)");
            
            assertTrue(startTimeWithinSchedule, "Slot start time should be within chef's schedule");
            assertTrue(endTimeWithinSchedule, "Slot end time should be within chef's schedule");
            
            // Verify slot duration is sufficient for travel + cooking
            int totalRequiredMinutes = 15 + 60; // 15min travel + 60min cooking
            int slotDurationMinutes = slot.getDurationMinutes();
            
            System.out.println("Required duration: " + totalRequiredMinutes + " minutes");
            System.out.println("Actual duration: " + slotDurationMinutes + " minutes");
            System.out.println("Duration sufficient: " + (slotDurationMinutes >= totalRequiredMinutes) + 
                    " (expected: true)");
            
            assertTrue(slotDurationMinutes >= totalRequiredMinutes, 
                    "Slot duration should be at least the required time for travel and cooking");
        }
        
        System.out.println("=================================================");
    }
    
    @Test
    @DisplayName("1.2 Should find available slots with dish list instead of menu")
    void findAvailableTimeSlotsWithInSingleDateWithDishList() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(5);
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        List<Long> dishIds = Arrays.asList(1L, 2L, 3L);
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        
        // For timezone testing - both in Vietnam
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone
        
        // Prepare distance service response - realistic travel time in HCMC
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5)); // 2.5 km between Lê Lợi and Nguyễn Huệ
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25)); // 15 minutes travel time in HCMC traffic
        
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Return schedule for the day
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek)));
        
        // No blocked dates or bookings
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        
        // Mock distance service with realistic data
        when(distanceService.calculateDistanceAndTime(chef.getAddress(), customerLocation))
                .thenReturn(distanceResponse);
        
        // Mock timezone service - realistic for Vietnam addresses
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        
        // Mock timezone conversion - same timezone for both locations in Vietnam
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // No timezone difference
        
        // Mock cooking time calculation - 90 minutes for the dish list
        when(calculateService.calculateTotalCookTime(dishIds, guestCount))
                .thenReturn(BigDecimal.valueOf(1.5)); // 1.5 hours
        
        // Act
        System.out.println("=========== TEST 1.2: Slot Finding with Dish List ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", dishIds=" + dishIds + ", guestCount=" + guestCount);
        System.out.println("Expected: Slots with at least 105 minutes duration (15min travel + 90min cooking)");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                chef.getId(), date, customerLocation, null, dishIds, guestCount, maxDishesPerMeal);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime() + 
                    ", duration=" + slot.getDurationMinutes() + " minutes");
            
            assertEquals(date, slot.getDate(), "Slot date should match requested date");
            
            // Verify the slot is within the chef's schedule
            boolean startTimeWithinSchedule = slot.getStartTime().compareTo(LocalTime.of(8, 0)) >= 0;
            boolean endTimeWithinSchedule = slot.getEndTime().compareTo(LocalTime.of(22, 0)) <= 0;
            
            System.out.println("Start time within schedule (8:00-22:00): " + startTimeWithinSchedule + 
                    " (expected: true)");
            System.out.println("End time within schedule (8:00-22:00): " + endTimeWithinSchedule + 
                    " (expected: true)");
            
            assertTrue(startTimeWithinSchedule, "Start time should be within chef's schedule");
            assertTrue(endTimeWithinSchedule, "End time should be within chef's schedule");
            
            // Verify slot duration is sufficient for travel + cooking
            int totalRequiredMinutes = 15 + 90; // 15min travel + 90min cooking
            int slotDurationMinutes = slot.getDurationMinutes();
            
            System.out.println("Required duration: " + totalRequiredMinutes + " minutes");
            System.out.println("Actual duration: " + slotDurationMinutes + " minutes");
            System.out.println("Duration sufficient: " + (slotDurationMinutes >= totalRequiredMinutes) + 
                    " (expected: true)");
            
            assertTrue(slotDurationMinutes >= totalRequiredMinutes, 
                    "Slot duration should be at least the required time for travel and cooking");
        }
        
        System.out.println("=======================================================");
    }
    
    @Test
    @DisplayName("1.3 Should use maxDishesPerMeal when no menu or dish list provided")
    void findAvailableTimeSlotsWithInSingleDateUsingDefaultCalculation() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(5);
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        
        // For timezone testing - both in Vietnam
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone
        
        // Prepare distance service response - realistic travel time in HCMC
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5)); // 2.5 km between Lê Lợi and Nguyễn Huệ
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25)); // 15 minutes travel time in HCMC traffic
        
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Return schedule for the day
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek)));
        
        // No blocked dates or bookings
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        
        // Mock distance service with realistic data
        when(distanceService.calculateDistanceAndTime(chef.getAddress(), customerLocation))
                .thenReturn(distanceResponse);
            
        // Mock timezone service - realistic for Vietnam addresses
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        
        // Mock timezone conversion - same timezone for both locations in Vietnam
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // No timezone difference
        
        // Mock cooking time calculation - 120 minutes for default calculation
        when(calculateService.calculateMaxCookTime(chef.getId(), maxDishesPerMeal, guestCount))
                .thenReturn(BigDecimal.valueOf(2.0)); // 2 hours
        
        // Act
        System.out.println("=========== TEST 1.3: Default Calculation (No Menu or Dish List) ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", maxDishesPerMeal=" + maxDishesPerMeal);
        System.out.println("Expected: Slots with at least 135 minutes duration (15min travel + 120min cooking)");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                chef.getId(), date, customerLocation, null, null, guestCount, maxDishesPerMeal);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime() + 
                    ", duration=" + slot.getDurationMinutes() + " minutes");
            
            assertEquals(date, slot.getDate(), "Slot date should match requested date");
            
            // Verify the slot is within the chef's schedule
            boolean startTimeWithinSchedule = slot.getStartTime().compareTo(LocalTime.of(8, 0)) >= 0;
            boolean endTimeWithinSchedule = slot.getEndTime().compareTo(LocalTime.of(22, 0)) <= 0;
            
            System.out.println("Start time within schedule (8:00-22:00): " + startTimeWithinSchedule + 
                    " (expected: true)");
            System.out.println("End time within schedule (8:00-22:00): " + endTimeWithinSchedule + 
                    " (expected: true)");
            
            assertTrue(startTimeWithinSchedule, "Start time should be within chef's schedule");
            assertTrue(endTimeWithinSchedule, "End time should be within chef's schedule");
            
            // Verify slot duration is sufficient for travel + cooking
            int totalRequiredMinutes = 15 + 120; // 15min travel + 120min cooking
            int slotDurationMinutes = slot.getDurationMinutes();
            
            System.out.println("Required duration: " + totalRequiredMinutes + " minutes");
            System.out.println("Actual duration: " + slotDurationMinutes + " minutes");
            System.out.println("Duration sufficient: " + (slotDurationMinutes >= totalRequiredMinutes) + 
                    " (expected: true)");
            
            assertTrue(slotDurationMinutes >= totalRequiredMinutes, 
                    "Slot duration should be at least the required time for travel and cooking");
        }
        
        System.out.println("==================================================================");
    }
    
    @Test
    @DisplayName("1.4 Should return empty list when chef has no schedule for the day")
    void findAvailableTimeSlotsWithInSingleDateWhenNoSchedule() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(5);
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        Long menuId = 1L;
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        
        // For timezone testing - both in Vietnam
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone
        
        // Create mock distance response
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5));
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25));
        
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Return empty schedule for the day
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek))
                .thenReturn(Collections.emptyList());
        
        // Mock timezone service - realistic for Vietnam addresses
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        
        // Mock distance service
        when(distanceService.calculateDistanceAndTime(chef.getAddress(), customerLocation))
                .thenReturn(distanceResponse);
        
        // Mock cooking time calculation using Mockito.any() to avoid strict stubbing issues
        // Make the stubbing lenient since it may not be used when there's no schedule
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId), any(), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(1.0)); // 1 hour
        
        // Mock timezone conversion - same timezone for both locations in Vietnam
        lenient().when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // No timezone difference
        
        // Act
        System.out.println("=========== TEST 1.4: No Schedule for Day ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", menuId=" + menuId + ", guestCount=" + guestCount);
        System.out.println("Expected: Empty list of time slots, as chef has no schedule for the day");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                chef.getId(), date, customerLocation, menuId, null, guestCount, maxDishesPerMeal);
        
        // Assert
        System.out.println("Actual: Result list is empty? " + (result.isEmpty() ? "Yes" : "No"));
        System.out.println("Expected: Yes");
        
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list when chef has no schedule for the day");
        
        System.out.println("=================================================");
    }
    
    @Test
    @DisplayName("1.5 Should respect blocked dates when finding available slots")
    void findAvailableTimeSlotsWithInSingleDateWithBlockedDates() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(5);
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        Long menuId = 1L;
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        
        // Create a Chef with a valid user
        User user = new User();
        user.setId(1L);
        user.setFullName("Test Chef");
        
        // Clone chef to avoid affecting other tests
        Chef testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(user);
        testChef.setAddress("123 Lê Lợi, Quận 1, TP.HCM, Việt Nam");
        
        // Create a schedule with working hours 8:00-22:00
        ChefSchedule schedule = new ChefSchedule();
        schedule.setId(1L);
        schedule.setChef(testChef);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(LocalTime.of(8, 0));  // 8:00 AM
        schedule.setEndTime(LocalTime.of(22, 0));   // 10:00 PM
        schedule.setIsDeleted(false);
        
        // Create a blocked date for testing
        ChefBlockedDate blockedDate = new ChefBlockedDate();
        blockedDate.setBlockId(1L);
        blockedDate.setChef(testChef);
        blockedDate.setBlockedDate(date);
        blockedDate.setStartTime(LocalTime.of(12, 0)); // 12:00 PM
        blockedDate.setEndTime(LocalTime.of(14, 0)); // 2:00 PM
        blockedDate.setIsDeleted(false);
        
        // Create a booking example to force slot creation
        Booking booking = new Booking();
        booking.setId(2L);
        booking.setChef(testChef);
        booking.setStatus("CONFIRMED");
        booking.setIsDeleted(false);
        
        // Create a booking detail in the middle of the day to split up the day
        BookingDetail bookingDetail = new BookingDetail();
        bookingDetail.setId(2L);
        bookingDetail.setBooking(booking);
        bookingDetail.setSessionDate(date);
        bookingDetail.setTimeBeginTravel(LocalTime.of(15, 0)); // Chef starts traveling at 15:00
        bookingDetail.setTimeBeginCook(LocalTime.of(16, 0));   // Chef starts cooking at 16:00
        bookingDetail.setStartTime(LocalTime.of(19, 0));       // Meal starts at 19:00
        bookingDetail.setStatus("CONFIRMED");
        bookingDetail.setIsDeleted(false);
        
        // For timezone testing
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone
        
        // Prepare distance service response
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5)); 
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25)); // 15 minutes travel time
        
        // Mock chef repository
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        
        // Return schedule for the day
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(Chef.class), eq(dayOfWeek)))
                .thenReturn(Collections.singletonList(schedule));
        
        // Return blocked date
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(Chef.class), eq(date)))
                .thenReturn(Collections.singletonList(blockedDate));
        
        // Return booking detail to create slots
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(Chef.class), eq(date)))
                .thenReturn(Collections.singletonList(bookingDetail));
        
        // Mock distance service
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock timezone service
        when(timeZoneService.getTimezoneFromAddress(anyString())).thenReturn(chefTimezone);
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // No timezone difference
        
        // Mock cooking time calculation
        when(calculateService.calculateTotalCookTimeFromMenu(eq(1L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0)); // 1 hour
        
        // Log important information for debugging
        System.out.println("=========== TEST 1.5: Respect Blocked Dates ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", menuId=" + menuId + ", guestCount=" + guestCount);
        System.out.println("Schedule: " + schedule.getStartTime() + "-" + schedule.getEndTime());
        System.out.println("Blocked period: " + blockedDate.getStartTime() + "-" + blockedDate.getEndTime());
        System.out.println("Booking: travel start=" + bookingDetail.getTimeBeginTravel() + 
                ", meal start=" + bookingDetail.getStartTime());
        System.out.println("Expected: Slots should not overlap with the blocked period");
        
        // Act
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                testChef.getId(), date, customerLocation, menuId, null, guestCount, maxDishesPerMeal);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should return at least one available slot");
        
        System.out.println("Found " + result.size() + " available slots:");
        
        // Define the blocked period boundaries
        LocalTime blockedStartTime = blockedDate.getStartTime(); // 12:00
        LocalTime blockedEndTime = blockedDate.getEndTime(); // 14:00
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime());
            
            // Check if this slot overlaps with the blocked period
            boolean overlapsWithBlockedPeriod = 
                (slot.getStartTime().isBefore(blockedEndTime) && 
                 slot.getEndTime().isAfter(blockedStartTime));
            
            System.out.println("Overlaps with blocked period (12:00-14:00): " + overlapsWithBlockedPeriod + 
                    " (expected: false)");
            
            assertFalse(overlapsWithBlockedPeriod, 
                    "Slot should not overlap with blocked period 12:00-14:00");
        }
    }
    
    @Test
    @DisplayName("1.6 Should respect existing bookings when finding available slots")
    void findAvailableTimeSlotsWithInSingleDateWithExistingBookings() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(3);
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        Long menuId = 1L;
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        
        // Create existing booking for the test day
        Booking booking = new Booking();
        booking.setId(2L);
        booking.setChef(chef);
        booking.setStatus("CONFIRMED");
        booking.setIsDeleted(false);
        
        // This booking occupies time from 15:00 (travel start) to 19:00 (meal start)
        // With 30 minutes rest after booking, the total blocked time is 15:00-19:30
        // The service considers chef unavailable during:
        // 1. Travel time (timeBeginTravel to timeBeginCook)
        // 2. Cooking time (timeBeginCook to startTime)
        // 3. Rest period (startTime to startTime + 30 minutes)
        BookingDetail bookingDetail = new BookingDetail();
        bookingDetail.setId(2L);
        bookingDetail.setBooking(booking);
        bookingDetail.setSessionDate(date);
        bookingDetail.setTimeBeginTravel(LocalTime.of(15, 0)); // Chef starts traveling at 15:00
        bookingDetail.setTimeBeginCook(LocalTime.of(16, 0));   // Chef starts cooking at 16:00
        bookingDetail.setStartTime(LocalTime.of(19, 0));       // Meal starts at 19:00
        bookingDetail.setStatus("CONFIRMED");
        bookingDetail.setIsDeleted(false);
        
        // For timezone testing - both in Vietnam
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone
        
        // Prepare distance service response - realistic travel time in HCMC
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5)); // 2.5 km between Lê Lợi and Nguyễn Huệ
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25)); // 15 minutes travel time in HCMC traffic
        
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Return schedule for the day
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek)));
        
        // No blocked dates
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        
        // Return our booking
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.singletonList(bookingDetail));
        
        // Mock distance service with realistic data
        when(distanceService.calculateDistanceAndTime(chef.getAddress(), customerLocation))
                .thenReturn(distanceResponse);
        
        // Mock timezone service - realistic for Vietnam addresses
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        
        // Mock timezone conversion - same timezone for both locations in Vietnam
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // No timezone difference
        
        // Mock cooking time calculation - 60 minutes for selected menu + 15 minutes travel
        doReturn(BigDecimal.valueOf(1.0)).when(calculateService).calculateTotalCookTimeFromMenu(
                eq(menuId), any(), eq(guestCount)); // 1 hour
        
        // Act
        System.out.println("=========== TEST 1.6: Respect Existing Bookings ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", menuId=" + menuId + ", guestCount=" + guestCount);
        System.out.println("Existing booking details:");
        System.out.println("- Travel begin: " + bookingDetail.getTimeBeginTravel());
        System.out.println("- Cooking begin: " + bookingDetail.getTimeBeginCook());
        System.out.println("- Meal start: " + bookingDetail.getStartTime());
        System.out.println("- End of rest period: " + bookingDetail.getStartTime().plusMinutes(30));
        System.out.println("Booked period (including 30min rest): 15:00-19:30");
        System.out.println("Expected: Slots should not overlap with the booked period 15:00-19:30");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                chef.getId(), date, customerLocation, menuId, null, guestCount, maxDishesPerMeal);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        // Define the booking period boundaries including rest time
        LocalTime bookingStartTime = bookingDetail.getTimeBeginTravel();              // 15:00
        LocalTime bookingEndTime = bookingDetail.getStartTime().plusMinutes(30);      // 19:30 (including 30min rest)
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime());
            
            // Check if this slot overlaps with the booked period (from travel start to meal start + rest)
            // A slot overlaps with a booked period if:
            // 1. The slot starts before the booking ends AND
            // 2. The slot ends after the booking starts
            boolean overlapsWithBookedPeriod = 
                (slot.getStartTime().isBefore(bookingEndTime) && 
                 slot.getEndTime().isAfter(bookingStartTime));
            
            System.out.println("Overlaps with booked period (15:00-19:30): " + overlapsWithBookedPeriod + 
                    " (expected: false)");
            
            assertFalse(overlapsWithBookedPeriod, 
                    "Slot should not overlap with booked period 15:00-19:30");
            
            // Also verify available slots are in two expected time ranges:
            // 1. Before the booking: must end by 15:00 (accounting for min 15min travel + 60min cooking)
            // 2. After the booking: must start after 19:30
            boolean isValidBeforeBooking = 
                (slot.getStartTime().compareTo(LocalTime.of(8, 0)) >= 0 && 
                 slot.getEndTime().compareTo(LocalTime.of(15, 0)) <= 0);
            
            boolean isValidAfterBooking = 
                (slot.getStartTime().compareTo(LocalTime.of(19, 30)) >= 0 && 
                 slot.getEndTime().compareTo(LocalTime.of(22, 0)) <= 0);
            
            System.out.println("Slot is in valid range (either 8:00-15:00 or 19:30-22:00): " + 
                    (isValidBeforeBooking || isValidAfterBooking) + " (expected: true)");
            
            assertTrue(isValidBeforeBooking || isValidAfterBooking,
                    "Slot should be either before booking (8:00-15:00) or after booking (19:30-22:00)");
        }
        
        System.out.println("=======================================================");
    }
    
    @Test
    @DisplayName("1.7 Should handle long travel times appropriately")
    void findAvailableTimeSlotsWithInSingleDateWithLongTravelTime() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(5);
        // Using a location far away from HCMC
        String customerLocation = "43 Đường Lý Thái Tổ, Hàng Trống, Hoàn Kiếm, Hà Nội, Việt Nam";
        Long menuId = 1L;
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        
        // For timezone testing - both in Vietnam
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone, even though Hanoi is in the north
        
        // Prepare distance service response with long travel time (HCMC to Hanoi approximately 1700km)
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(1700.0)); // 1700 km from HCMC to Hanoi
        distanceResponse.setDurationHours(BigDecimal.valueOf(3.0)); // 3 hours by flight (180 minutes)
        
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Return schedule for the day
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek)));
        
        // No blocked dates or bookings
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date))
                .thenReturn(Collections.emptyList());
        
        // Mock distance service with long travel time
        when(distanceService.calculateDistanceAndTime(chef.getAddress(), customerLocation))
                .thenReturn(distanceResponse);
        
        // Mock timezone service - both in Vietnam
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        
        // Mock timezone conversion - same timezone
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // No timezone difference
        
        // Mock cooking time calculation - 60 minutes for selected menu
        doReturn(BigDecimal.valueOf(1.0)).when(calculateService).calculateTotalCookTimeFromMenu(
                eq(menuId), any(), eq(guestCount)); // 1 hour
        
        // Act
        System.out.println("=========== TEST 1.7: Long Travel Time ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", menuId=" + menuId + ", guestCount=" + guestCount);
        System.out.println("Expected: Slots with at least 240 minutes duration (180min travel + 60min cooking)");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                chef.getId(), date, customerLocation, menuId, null, guestCount, maxDishesPerMeal);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime() + 
                    ", duration=" + slot.getDurationMinutes() + " minutes");
            
            assertEquals(date, slot.getDate(), "Slot date should match requested date");
            
            // Verify slot duration is sufficient for travel + cooking
            int totalRequiredMinutes = 180 + 60; // 3 hours (180min) travel + 60min cooking
            int slotDurationMinutes = slot.getDurationMinutes();
            
            System.out.println("Required duration: " + totalRequiredMinutes + " minutes");
            System.out.println("Actual duration: " + slotDurationMinutes + " minutes");
            System.out.println("Duration sufficient: " + (slotDurationMinutes >= totalRequiredMinutes) + 
                    " (expected: true)");
            
            assertTrue(slotDurationMinutes >= totalRequiredMinutes, 
                    "Slot duration should be at least the required time for travel and cooking");
        }
        
        System.out.println("=================================================");
    }
    
    @Test
    @DisplayName("1.8 Should throw exception when chef is not found")
    void findAvailableTimeSlotsWithInSingleDateWithInvalidChefId() {
        // Arrange
        Long invalidChefId = 999L;
        LocalDate date = LocalDate.now().plusDays(5);
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        Long menuId = 1L;
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        when(chefRepository.findById(invalidChefId)).thenReturn(Optional.empty());
        
        // Act & Assert
        System.out.println("=========== TEST 1.8: Invalid Chef ID ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", menuId=" + menuId + ", guestCount=" + guestCount);
        System.out.println("Expected: NOT_FOUND status");
        
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                invalidChefId, date, customerLocation, menuId, null, guestCount, maxDishesPerMeal);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "Should return NOT_FOUND status");
        assertTrue(exception.getMessage().contains("Chef not found"), 
                "Exception message should mention that chef was not found");
        
        System.out.println("=================================================");
    }

    // Test Group 2: findAvailableTimeSlotsWithInMultipleDates
    
    @Test
    @DisplayName("2.1 Should find available slots for multiple dates")
    void findAvailableTimeSlotsWithInMultipleDatesBasic() {
        // Arrange
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        // Create multiple date requests
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(7);
        Long menuId1 = 1L;
        Long menuId2 = 2L;
        
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(date1);
        request1.setMenuId(menuId1);
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(date2);
        request2.setMenuId(menuId2);
        
        List<AvailableTimeSlotRequest> requests = Arrays.asList(request1, request2);
        
        // For timezone testing - both in Vietnam
        String chefTimezone = "Asia/Ho_Chi_Minh"; // UTC+7
        String customerTimezone = "Asia/Ho_Chi_Minh"; // Same timezone
        
        // Mock chef repository - this is needed for initial validation
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Mock schedule repository for each day
        int dayOfWeek1 = date1.getDayOfWeek().getValue() - 1;
        int dayOfWeek2 = date2.getDayOfWeek().getValue() - 1;
        
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek1))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek1)));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek2))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek2)));
        
        // Mock distance service
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5));
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25));
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock calculate service
        when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId1), isNull(), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(1.0));
        when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId2), isNull(), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(1.0));
            
        // Mock timezone service - this is required to prevent NullPointerException
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        
        // Mock timezone conversion - same timezone for both locations in Vietnam
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // No timezone difference
        
        // Act
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                chef.getId(), customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify we have slots for both dates
        Set<LocalDate> dates = new HashSet<>();
        for (AvailableTimeSlotResponse slot : result) {
            dates.add(slot.getDate());
            
            // Verify slot times are within chef's schedule (8:00-22:00)
            assertTrue(slot.getStartTime().compareTo(LocalTime.of(8, 0)) >= 0, 
                    "Start time should be after or at 8:00");
            assertTrue(slot.getEndTime().compareTo(LocalTime.of(22, 0)) <= 0, 
                    "End time should be before or at 22:00");
            
            // Verify slot duration is sufficient for travel + cooking
            int totalRequiredMinutes = 15 + 60; // 15min travel + 60min cooking
            assertTrue(slot.getDurationMinutes() >= totalRequiredMinutes, 
                    "Slot duration should be at least the required time for travel and cooking");
        }
        
        assertEquals(2, dates.size(), "Should have slots for both requested dates");
        assertTrue(dates.contains(date1), "Should have slots for first date");
        assertTrue(dates.contains(date2), "Should have slots for second date");
    }
    
    @Test
    @DisplayName("2.2 Should find available slots for multiple dates with dish lists")
    void findAvailableTimeSlotsWithInMultipleDatesWithDishLists() {
        // Arrange
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        // Create multiple date requests with dish lists
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(7);
        List<Long> dishIds1 = Arrays.asList(1L, 2L);
        List<Long> dishIds2 = Arrays.asList(3L, 4L, 5L);
        
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(date1);
        request1.setDishIds(dishIds1);
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(date2);
        request2.setDishIds(dishIds2);
        
        List<AvailableTimeSlotRequest> requests = Arrays.asList(request1, request2);
        
        // For timezone testing - both in Vietnam
        String chefTimezone = "Asia/Ho_Chi_Minh";
        String customerTimezone = "Asia/Ho_Chi_Minh";
        
        // Mock chef repository
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Mock schedule repository for each day
        int dayOfWeek1 = date1.getDayOfWeek().getValue() - 1;
        int dayOfWeek2 = date2.getDayOfWeek().getValue() - 1;
        
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek1))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek1)));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek2))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek2)));
        
        // Mock distance service
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5));
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25));
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock calculate service for dish lists
        when(calculateService.calculateTotalCookTime(eq(dishIds1), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(1.2));
        when(calculateService.calculateTotalCookTime(eq(dishIds2), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(1.5));
        
        // Mock timezone service
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                chef.getId(), customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify we have slots for both dates with correct cooking times
        Map<LocalDate, List<AvailableTimeSlotResponse>> slotsByDate = new HashMap<>();
        for (AvailableTimeSlotResponse slot : result) {
            slotsByDate.computeIfAbsent(slot.getDate(), k -> new ArrayList<>()).add(slot);
            
            // Verify slot times are within chef's schedule
            assertTrue(slot.getStartTime().compareTo(LocalTime.of(8, 0)) >= 0);
            assertTrue(slot.getEndTime().compareTo(LocalTime.of(22, 0)) <= 0);
        }
        
        assertEquals(2, slotsByDate.size());
        assertTrue(slotsByDate.containsKey(date1));
        assertTrue(slotsByDate.containsKey(date2));
        
        // Verify cooking times in notes
        if (!slotsByDate.get(date1).isEmpty()) {
            assertTrue(slotsByDate.get(date1).get(0).getNote().contains("1.20 hours cooking time"));
        }
        if (!slotsByDate.get(date2).isEmpty()) {
            assertTrue(slotsByDate.get(date2).get(0).getNote().contains("1.50 hours cooking time"));
        }
    }
    
    @Test
    @DisplayName("2.3 Should return empty list for dates with no schedules")
    void findAvailableTimeSlotsWithInMultipleDatesWhenNoSchedules() {
        // Arrange
        String customerLocation = "456 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(7);
        Long menuId1 = 1L;
        Long menuId2 = 2L;
        
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(date1);
        request1.setMenuId(menuId1);
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(date2);
        request2.setMenuId(menuId2);
        
        List<AvailableTimeSlotRequest> requests = Arrays.asList(request1, request2);
        
        // Mock chef repository - needed for initial validation
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Mock distance service - needed before schedule check
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5));
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25));
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock calculate service - needed before schedule check
        // Use lenient() to avoid strict stubbing issues
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(1L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(2L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        
        // Return empty schedules for both days
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), anyInt()))
                .thenReturn(Collections.emptyList());
        
        // Act
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                chef.getId(), customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("2.4 Should respect blocked dates across multiple dates")
    void findAvailableTimeSlotsWithInMultipleDatesWithBlockedDates() {
        // Arrange
        String customerLocation = "123 Customer St";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        // Create multiple date requests
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(7);
        Long menuId1 = 1L;
        Long menuId2 = 2L;
        
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(date1);
        request1.setMenuId(menuId1);
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(date2);
        request2.setMenuId(menuId2);
        
        List<AvailableTimeSlotRequest> requests = Arrays.asList(request1, request2);
        
        // Create blocked date for the first date
        ChefBlockedDate blockedDate = new ChefBlockedDate();
        blockedDate.setBlockId(2L);
        blockedDate.setChef(chef);
        blockedDate.setBlockedDate(date1);
        blockedDate.setStartTime(LocalTime.of(12, 0));
        blockedDate.setEndTime(LocalTime.of(14, 0));
        blockedDate.setIsDeleted(false);
        
        // Mock chef repository - needed for initial validation
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Mock distance service - needed before schedule check
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5));
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25));
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock calculate service - needed before schedule check
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(1L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(2L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        
        // Mock timezone service - needed for timezone conversion
        String chefTimezone = "Asia/Ho_Chi_Minh";
        String customerTimezone = "Asia/Ho_Chi_Minh";
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Set up mocks for each day
        int dayOfWeek1 = date1.getDayOfWeek().getValue() - 1;
        int dayOfWeek2 = date2.getDayOfWeek().getValue() - 1;
        
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek1))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek1)));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek2))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek2)));
        
        // Set up blocked date for first date only
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date1))
                .thenReturn(Collections.singletonList(blockedDate));
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date2))
                .thenReturn(Collections.emptyList());
        
        // No bookings
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date1))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date2))
                .thenReturn(Collections.emptyList());
        
        // Act
        System.out.println("=========== TEST 2.4: Blocked Dates ==========");
        System.out.println("Input: date1=" + date1 + ", date2=" + date2 + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", menuId1=" + menuId1 + ", menuId2=" + menuId2);
        System.out.println("Blocked period: 12:00-14:00");
        System.out.println("Expected: Slots should not overlap with the blocked period");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                chef.getId(), customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime());
            
            // For date1, verify no slots overlap with the blocked period
            if (slot.getDate().equals(date1)) {
                boolean overlapsWithBlockedPeriod = 
                    (slot.getStartTime().isBefore(LocalTime.of(14, 0)) && 
                     slot.getEndTime().isAfter(LocalTime.of(12, 0)));
                
                System.out.println("Overlaps with blocked period (12:00-14:00): " + overlapsWithBlockedPeriod + 
                        " (expected: false)");
                
                assertFalse(overlapsWithBlockedPeriod, 
                        "Slot should not overlap with blocked period 12:00-14:00");
            }
        }
        
        System.out.println("=======================================================");
    }
    
    @Test
    @DisplayName("2.5 Should respect existing bookings across multiple dates")
    void findAvailableTimeSlotsWithInMultipleDatesWithExistingBookings() {
        // Arrange
        String customerLocation = "123 Customer St";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        // Create multiple date requests
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(7);
        Long menuId1 = 1L;
        Long menuId2 = 2L;
        
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(date1);
        request1.setMenuId(menuId1);
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(date2);
        request2.setMenuId(menuId2);
        
        List<AvailableTimeSlotRequest> requests = Arrays.asList(request1, request2);
        
        // Create existing booking for the second date
        Booking booking = new Booking();
        booking.setId(2L);
        booking.setChef(chef);
        booking.setStatus("CONFIRMED");
        booking.setIsDeleted(false);
        
        BookingDetail bookingDetail = new BookingDetail();
        bookingDetail.setId(2L);
        bookingDetail.setBooking(booking);
        bookingDetail.setSessionDate(date2);
        bookingDetail.setTimeBeginTravel(LocalTime.of(16, 0));
        bookingDetail.setTimeBeginCook(LocalTime.of(17, 0));
        bookingDetail.setStartTime(LocalTime.of(19, 0));
        bookingDetail.setStatus("CONFIRMED");
        bookingDetail.setIsDeleted(false);
        
        // Mock chef repository - needed for initial validation
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Mock distance service - needed before schedule check
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5));
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25));
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock calculate service - needed before schedule check
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(1L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(2L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        
        // Mock timezone service - needed for timezone conversion
        String chefTimezone = "Asia/Ho_Chi_Minh";
        String customerTimezone = "Asia/Ho_Chi_Minh";
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Set up mocks for each day
        int dayOfWeek1 = date1.getDayOfWeek().getValue() - 1;
        int dayOfWeek2 = date2.getDayOfWeek().getValue() - 1;
        
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek1))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek1)));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek2))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek2)));
        
        // No blocked dates
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date1))
                .thenReturn(Collections.emptyList());
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(chef, date2))
                .thenReturn(Collections.emptyList());
        
        // Booking on date2 only
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date1))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date2))
                .thenReturn(Collections.singletonList(bookingDetail));
        
        // Act
        System.out.println("=========== TEST 2.5: Existing Bookings ==========");
        System.out.println("Input: date1=" + date1 + ", date2=" + date2 + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", menuId1=" + menuId1 + ", menuId2=" + menuId2);
        System.out.println("Booked period: 16:00-19:30");
        System.out.println("Expected: Slots should not overlap with the booked period");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                chef.getId(), customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime());
            
            // For date2, verify no slots overlap with the booked period
            if (slot.getDate().equals(date2)) {
                boolean overlapsWithBookedPeriod = 
                    (slot.getStartTime().isBefore(LocalTime.of(19, 30)) && // Including 30min rest after
                     slot.getEndTime().isAfter(LocalTime.of(16, 0)));
                
                System.out.println("Overlaps with booked period (16:00-19:30): " + overlapsWithBookedPeriod + 
                        " (expected: false)");
                
                assertFalse(overlapsWithBookedPeriod, 
                        "Slot should not overlap with booked period 16:00-19:30");
            }
        }
        
        System.out.println("=======================================================");
    }
    
    @Test
    @DisplayName("2.6 Should handle mixed menu and dish list requests")
    void findAvailableTimeSlotsWithInMultipleDatesWithMixedRequests() {
        // Arrange
        String customerLocation = "123 Customer St";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        // Create mixed requests - one with menu, one with dish list
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(7);
        Long menuId = 1L;
        List<Long> dishIds = Arrays.asList(2L, 3L, 4L);
        
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(date1);
        request1.setMenuId(menuId);
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(date2);
        request2.setDishIds(dishIds);
        
        List<AvailableTimeSlotRequest> requests = Arrays.asList(request1, request2);
        
        // Mock chef repository - needed for initial validation
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Mock distance service - needed before schedule check
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(2.5));
        distanceResponse.setDurationHours(BigDecimal.valueOf(0.25));
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock calculate service - needed before schedule check
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(1L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0)); // 1 hour for menu
        lenient().when(calculateService.calculateTotalCookTime(eq(dishIds), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.5)); // 1.5 hours for dish list
        
        // Mock timezone service - needed for timezone conversion
        String chefTimezone = "Asia/Ho_Chi_Minh";
        String customerTimezone = "Asia/Ho_Chi_Minh";
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Set up mocks for each day
        int dayOfWeek1 = date1.getDayOfWeek().getValue() - 1;
        int dayOfWeek2 = date2.getDayOfWeek().getValue() - 1;
        
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek1))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek1)));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek2))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek2)));
        
        // No blocked dates or bookings
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(chef), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(chef), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        
        // Act
        System.out.println("=========== TEST 2.6: Mixed Requests ==========");
        System.out.println("Input: date1=" + date1 + ", date2=" + date2 + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", menuId=" + menuId + ", dishIds=" + dishIds);
        System.out.println("Expected: Slots for both dates");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                chef.getId(), customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime());
            
            // Check that the note contains the correct cooking time based on the date
            if (slot.getDate().equals(date1)) {
                assertTrue(slot.getNote().contains("1.00 hours cooking time"), 
                        "Note for date1 should mention 1 hour cooking time");
            } else if (slot.getDate().equals(date2)) {
                assertTrue(slot.getNote().contains("1.50 hours cooking time"), 
                        "Note for date2 should mention 1.5 hours cooking time");
            }
        }
        
        System.out.println("=======================================================");
    }
    
    @Test
    @DisplayName("2.7 Should handle long travel times across multiple dates")
    void findAvailableTimeSlotsWithInMultipleDatesWithLongTravelTime() {
        // Arrange
        String customerLocation = "123 Far Away St";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        
        // Create multiple date requests
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(7);
        Long menuId1 = 1L;
        Long menuId2 = 2L;
        
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(date1);
        request1.setMenuId(menuId1);
        
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(date2);
        request2.setMenuId(menuId2);
        
        List<AvailableTimeSlotRequest> requests = Arrays.asList(request1, request2);
        
        // Mock chef repository - needed for initial validation
        when(chefRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
        
        // Mock distance service - needed before schedule check
        DistanceResponse distanceResponse = new DistanceResponse();
        distanceResponse.setDistanceKm(BigDecimal.valueOf(50.0));
        distanceResponse.setDurationHours(BigDecimal.valueOf(1.5)); // 1.5 hours travel time
        when(distanceService.calculateDistanceAndTime(anyString(), anyString()))
                .thenReturn(distanceResponse);
        
        // Mock calculate service - needed before schedule check
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(1L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        lenient().when(calculateService.calculateTotalCookTimeFromMenu(eq(2L), isNull(), eq(4)))
                .thenReturn(BigDecimal.valueOf(1.0));
        
        // Mock timezone service - needed for timezone conversion
        String chefTimezone = "Asia/Ho_Chi_Minh";
        String customerTimezone = "Asia/Ho_Chi_Minh";
        when(timeZoneService.getTimezoneFromAddress(chef.getAddress())).thenReturn(chefTimezone);
        when(timeZoneService.getTimezoneFromAddress(customerLocation)).thenReturn(customerTimezone);
        when(timeZoneService.convertBetweenTimezones(any(LocalDateTime.class), eq(chefTimezone), eq(customerTimezone)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Set up mocks for each day
        int dayOfWeek1 = date1.getDayOfWeek().getValue() - 1;
        int dayOfWeek2 = date2.getDayOfWeek().getValue() - 1;
        
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek1))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek1)));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek2))
                .thenReturn(Collections.singletonList(schedules.get(dayOfWeek2)));
        
        // No blocked dates or bookings
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(eq(chef), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(chef), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        
        // Act
        System.out.println("=========== TEST 2.7: Long Travel Time ==========");
        System.out.println("Input: date1=" + date1 + ", date2=" + date2 + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", menuId1=" + menuId1 + ", menuId2=" + menuId2);
        System.out.println("Expected: Slots with at least 120 minutes duration (50min travel + 60min cooking)");
        
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                chef.getId(), customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        System.out.println("Actual: Found " + result.size() + " available time slots");
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println("Slot: date=" + slot.getDate() + 
                    ", time=" + slot.getStartTime() + "~" + slot.getEndTime());
            
            // Verify slot duration is sufficient for meal + travel + cooking
            int totalRequiredMinutes = 90 + 60; // 1.5 hours travel + 1 hour cooking
            int slotDurationMinutes = slot.getDurationMinutes();
            
            System.out.println("Required duration: " + totalRequiredMinutes + " minutes");
            System.out.println("Actual duration: " + slotDurationMinutes + " minutes");
            System.out.println("Duration sufficient: " + (slotDurationMinutes >= totalRequiredMinutes) + 
                    " (expected: true)");
            
            assertTrue(slotDurationMinutes >= totalRequiredMinutes, 
                    "Slot duration should be at least the required time for travel and cooking");
        }
        
        System.out.println("=======================================================");
    }
    
    @Test
    @DisplayName("2.8 Should throw exception for invalid input parameters")
    void findAvailableTimeSlotsWithInMultipleDatesWithInvalidInput() {
        // Arrange
        String customerLocation = "123 Customer St";
        int guestCount = 4;
        int maxDishesPerMeal = 5;
        Long invalidChefId = 999L;
        
        // Create request
        LocalDate date = LocalDate.now().plusDays(5);
        AvailableTimeSlotRequest request = new AvailableTimeSlotRequest();
        request.setSessionDate(date);
        request.setMenuId(1L);
        
        List<AvailableTimeSlotRequest> requests = Collections.singletonList(request);
        
        // Mock chef repository - needed for initial validation
        when(chefRepository.findById(invalidChefId)).thenReturn(Optional.empty());
        
        // Act & Assert - Test invalid chef ID
        System.out.println("=========== TEST 2.8: Invalid Chef ID ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", maxDishesPerMeal=" + maxDishesPerMeal);
        System.out.println("Expected: NOT_FOUND status");
        
        VchefApiException chefException = assertThrows(VchefApiException.class, () -> {
            availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                invalidChefId, customerLocation, guestCount, maxDishesPerMeal, requests);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, chefException.getStatus(), "Should return NOT_FOUND status");
        assertTrue(chefException.getMessage().contains("Chef not found"), 
                "Exception message should mention that chef was not found");
        
        // Act & Assert - Test empty requests list
        System.out.println("=========== TEST 2.8: Empty Requests ==========");
        System.out.println("Input: chefId=" + invalidChefId + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", maxDishesPerMeal=" + maxDishesPerMeal);
        System.out.println("Expected: BAD_REQUEST status");
        
        VchefApiException requestsException = assertThrows(VchefApiException.class, () -> {
            availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                invalidChefId, customerLocation, guestCount, maxDishesPerMeal, Collections.emptyList());
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, requestsException.getStatus(), "Should return BAD_REQUEST status");
        assertTrue(requestsException.getMessage().contains("Requests list cannot be empty"), 
                "Exception message should mention that requests list cannot be empty");
        
        // Act & Assert - Test null customer location
        System.out.println("=========== TEST 2.8: Null Location ==========");
        System.out.println("Input: date=" + date + ", location=" + customerLocation + 
                ", guestCount=" + guestCount + ", maxDishesPerMeal=" + maxDishesPerMeal);
        System.out.println("Expected: BAD_REQUEST status");
        
        VchefApiException locationException = assertThrows(VchefApiException.class, () -> {
            availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                invalidChefId, null, guestCount, maxDishesPerMeal, requests);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, locationException.getStatus(), "Should return BAD_REQUEST status");
        assertTrue(locationException.getMessage().contains("Customer location cannot be empty"), 
                "Exception message should mention that customer location cannot be empty");
        
        System.out.println("=======================================================");
    }
} 