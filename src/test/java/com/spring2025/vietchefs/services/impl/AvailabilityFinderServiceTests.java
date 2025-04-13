package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.payload.requestModel.AvailableTimeSlotRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AvailableTimeSlotResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingConflictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Tests for AvailabilityFinderService covering:
 * - Finding slots with menu ID
 * - Finding slots with dish IDs
 * - Finding slots with neither (maximum cooking time)
 * - Testing with bookings
 * - Testing multiple dates
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AvailabilityFinderServiceTests {

    @Mock
    private ChefRepository chefRepository;
    
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
    
    @Mock
    private DistanceService distanceService;
    
    @InjectMocks
    private AvailabilityFinderServiceImpl availabilityFinderService;
    
    private Chef chef;
    private User user;
    private LocalDate today;
    
    @BeforeEach
    public void setUp() {
        today = LocalDate.now();
        
        user = new User();
        user.setId(1L);
        
        chef = new Chef();
        chef.setId(1L);
        chef.setUser(user);
        chef.setAddress("123 Chef Street");
        
        // Set up schedule for today
        ChefSchedule schedule = new ChefSchedule();
        schedule.setChef(chef);
        schedule.setDayOfWeek(today.getDayOfWeek().getValue());
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(22, 0));
        
        // Set up standard mocks
        when(chefRepository.findById(1L)).thenReturn(Optional.of(chef));
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), anyInt()))
                .thenReturn(List.of(schedule));
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), any()))
                .thenReturn(new ArrayList<>());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), any()))
                .thenReturn(new ArrayList<>());
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithMenuId() {
        // Setup
        String customerLocation = "456 Customer Street";
        Long menuId = 1L;
        int guestCount = 4;
        
        // Mock distance calculation - 30 minutes travel time
        DistanceResponse distanceResponse = new DistanceResponse(BigDecimal.valueOf(10), BigDecimal.valueOf(0.5));
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(distanceResponse);
        
        // Mock cooking time - 1 hour for menu
        when(calculateService.calculateTotalCookTimeFromMenu(eq(menuId), isNull(), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(1.0));
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, today, customerLocation, menuId, null, guestCount, 6);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithDishIds() {
        // Setup
        String customerLocation = "456 Customer Street";
        List<Long> dishIds = Arrays.asList(1L, 2L, 3L);
        int guestCount = 4;
        
        // Mock distance calculation - 30 minutes travel time
        DistanceResponse distanceResponse = new DistanceResponse(BigDecimal.valueOf(10), BigDecimal.valueOf(0.5));
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(distanceResponse);
        
        // Mock cooking time - 1.5 hours for dishes
        when(calculateService.calculateTotalCookTime(eq(dishIds), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(1.5));
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, today, customerLocation, null, dishIds, guestCount, 6);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithMaxCookTime() {
        // Setup
        String customerLocation = "456 Customer Street";
        int guestCount = 4;
        int maxDishesPerMeal = 6;
        
        // Mock distance calculation - 30 minutes travel time
        DistanceResponse distanceResponse = new DistanceResponse(BigDecimal.valueOf(10), BigDecimal.valueOf(0.5));
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(distanceResponse);
        
        // Mock cooking time - 2 hours max time
        when(calculateService.calculateMaxCookTime(eq(1L), eq(maxDishesPerMeal), eq(guestCount)))
                .thenReturn(BigDecimal.valueOf(2.0));
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, today, customerLocation, null, null, guestCount, maxDishesPerMeal);
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithBookingConflict() {
        // Setup
        String customerLocation = "456 Customer Street";
        int guestCount = 4;
        
        System.out.println("===== DEBUG testFindAvailableTimeSlotsWithBookingConflict =====");
        System.out.println("Today's date: " + today);
        
        // Mock distance calculation
        DistanceResponse distanceResponse = new DistanceResponse(BigDecimal.valueOf(10), BigDecimal.valueOf(0.5));
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(distanceResponse);
        System.out.println("Mocked distance response: " + distanceResponse.getDistanceKm() + "km, " 
                + distanceResponse.getDurationHours() + " hours");
        
        // Mock cooking time
        when(calculateService.calculateMaxCookTime(anyLong(), anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(1.0));
        System.out.println("Mocked cooking time: 1.0 hours");
        
        // Đảm bảo schedule là từ 8:00 đến 22:00
        ChefSchedule schedule = new ChefSchedule();
        schedule.setChef(chef);
        schedule.setDayOfWeek(today.getDayOfWeek().getValue());
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(22, 0));
        
        System.out.println("Schedule: " + schedule.getStartTime() + " to " + schedule.getEndTime());
        
        // Make sure we use the correct schedule
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), anyInt()))
                .thenReturn(List.of(schedule));
        
        // Tạo booking từ 12:00 đến 14:00
        Booking booking = new Booking();
        booking.setChef(chef);
        booking.setStatus("CONFIRMED");
        booking.setIsDeleted(false);
        
        BookingDetail bookingDetail = new BookingDetail();
        bookingDetail.setBooking(booking);
        bookingDetail.setSessionDate(today);
        bookingDetail.setStartTime(LocalTime.of(14, 0)); // Thời gian kết thúc booking là 14:00 (khách bắt đầu ăn)
        bookingDetail.setTimeBeginCook(LocalTime.of(13, 0)); // Chef bắt đầu nấu ăn lúc 13:00 (nấu trong 1 giờ)
        bookingDetail.setTimeBeginTravel(LocalTime.of(12, 0)); // Thời gian bắt đầu booking là 12:00 (chef bắt đầu di chuyển)
        bookingDetail.setStatus("CONFIRMED");
        bookingDetail.setIsDeleted(false);
        bookingDetail.setLocation(customerLocation);
        
        System.out.println("Created booking detail: Date=" + bookingDetail.getSessionDate() 
                + ", Booking time span=" + bookingDetail.getTimeBeginTravel() + "-" + bookingDetail.getStartTime()
                + " (chef travels at " + bookingDetail.getTimeBeginTravel() 
                + ", cooks at " + bookingDetail.getTimeBeginCook() + ")");
        
        // Tính toán thời gian sớm nhất có thể bắt đầu dựa trên lịch trình
        LocalTime earliestPossibleStartTime = schedule.getStartTime()
                .plusMinutes((int)(0.5 * 60)) // Thời gian di chuyển 0.5h
                .plusMinutes((int)(1.0 * 60)); // Thời gian nấu ăn 1.0h
        System.out.println("Earliest possible start time based on schedule: " 
                + schedule.getStartTime() + " + 0.5h travel + 1.0h cook = " + earliestPossibleStartTime);
        
        // Thêm booking vào danh sách
        List<BookingDetail> bookings = new ArrayList<>();
        bookings.add(bookingDetail);
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), eq(today)))
                .thenReturn(bookings);
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, today, customerLocation, null, null, guestCount, 6);
        
        // Debug output for the result
        System.out.println("\nResult contains " + result.size() + " available time slots:");
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println(" - " + slot.getDate() + " " + slot.getStartTime() + " to " + slot.getEndTime() 
                    + " (" + slot.getDurationMinutes() + " minutes)");
        }
        
        // Verify
        assertNotNull(result);
        
        // Expected slots based on the problem description:
        // 1. Morning slot: 9:30 - 12:00 (before booking starts at 12:00)
        // 2. Evening slot: 16:00 - 22:00 (after booking ends at 14:00, plus prep time)

        // Kiểm tra tổng số slot
        System.out.println("Expected number of slots: 2");
        assertEquals(2, result.size(), "Should have exactly 2 available time slots");
        
        // Kiểm tra các slot cụ thể
        boolean hasSlot1 = false; // 9:30 - 12:00
        boolean hasSlot2 = false; // 16:00 - 22:00
        
        for (AvailableTimeSlotResponse slot : result) {
            // Kiểm tra slot 1: 9:30 - 12:00
            if (slot.getStartTime().equals(LocalTime.of(9, 30)) && 
                slot.getEndTime().equals(LocalTime.of(12, 0))) {
                hasSlot1 = true;
                System.out.println("Found expected morning slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
            
            // Kiểm tra slot 2: 16:00 - 22:00
            if (slot.getStartTime().equals(LocalTime.of(16, 0)) && 
                slot.getEndTime().equals(LocalTime.of(22, 0))) {
                hasSlot2 = true;
                System.out.println("Found expected evening slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
        }
        
        // Verify tất cả các slot
        System.out.println("Has morning slot (9:30-12:00)? " + hasSlot1 + " (should be true)");
        assertTrue(hasSlot1, "Should have a slot from 9:30 to 12:00");
        
        System.out.println("Has evening slot (16:00-22:00)? " + hasSlot2 + " (should be true)");
        assertTrue(hasSlot2, "Should have a slot from 16:00 to 22:00");
        
        System.out.println("===== END DEBUG testFindAvailableTimeSlotsWithBookingConflict =====\n");
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithMultipleDates() {
        // Setup
        String customerLocation = "456 Customer Street";
        int guestCount = 4;
        int maxDishesPerMeal = 6;
        LocalDate tomorrow = today.plusDays(1);
        
        System.out.println("===== DEBUG testFindAvailableTimeSlotsWithMultipleDates =====");
        System.out.println("Today's date: " + today);
        System.out.println("Tomorrow's date: " + tomorrow);
        
        // Create requests for different dates
        List<AvailableTimeSlotRequest> requests = new ArrayList<>();
        
        // Today with menu
        AvailableTimeSlotRequest request1 = new AvailableTimeSlotRequest();
        request1.setSessionDate(today);
        request1.setMenuId(1L);
        
        // Tomorrow with dishes
        AvailableTimeSlotRequest request2 = new AvailableTimeSlotRequest();
        request2.setSessionDate(tomorrow);
        request2.setDishIds(Arrays.asList(1L, 2L, 3L));
        
        requests.add(request1);
        requests.add(request2);
        
        System.out.println("Request 1: Date=" + request1.getSessionDate() + ", MenuId=" + request1.getMenuId());
        System.out.println("Request 2: Date=" + request2.getSessionDate() + ", DishIds=" + request2.getDishIds());
        
        // Mock distance calculation
        DistanceResponse distanceResponse = new DistanceResponse(BigDecimal.valueOf(10), BigDecimal.valueOf(0.5));
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(distanceResponse);
        System.out.println("Mocked distance response: " + distanceResponse.getDistanceKm() + "km, " 
                + distanceResponse.getDurationHours() + " hours");
        
        // Sử dụng lenient() để tránh UnnecessaryStubbingException
        // Mock cooking times for menu and dishes
        when(calculateService.calculateTotalCookTimeFromMenu(anyLong(), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(1.0));
        when(calculateService.calculateTotalCookTime(any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(1.5));
        
        // Mocking cho calculateMaxCookTime để tránh lỗi khi không có menu hoặc dishes
        when(calculateService.calculateMaxCookTime(anyLong(), anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(1.0));
                
        System.out.println("Mocked cooking time for menu: 1.0 hours");
        System.out.println("Mocked cooking time for dishes: 1.5 hours");
        
        // Set up schedule for tomorrow
        ChefSchedule tomorrowSchedule = new ChefSchedule();
        tomorrowSchedule.setChef(chef);
        tomorrowSchedule.setDayOfWeek(tomorrow.getDayOfWeek().getValue());
        tomorrowSchedule.setStartTime(LocalTime.of(8, 0));
        tomorrowSchedule.setEndTime(LocalTime.of(22, 0));
        
        System.out.println("Tomorrow's schedule: " + tomorrowSchedule.getStartTime() + " to " + tomorrowSchedule.getEndTime());
        
        // Make sure we are setting up a list for both today and tomorrow
        // For today, we already have setup in the @BeforeEach
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), eq(tomorrow.getDayOfWeek().getValue())))
                .thenReturn(List.of(tomorrowSchedule));
                
        // Setup empty booking lists for both days
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), eq(today)))
                .thenReturn(new ArrayList<>());
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), eq(tomorrow)))
                .thenReturn(new ArrayList<>());
        
        // Setup blocked dates (empty for this test)
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), eq(today)))
                .thenReturn(new ArrayList<>());
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), eq(tomorrow)))
                .thenReturn(new ArrayList<>());
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInMultipleDates(
                1L, customerLocation, guestCount, maxDishesPerMeal, requests);
        
        // Debug output for the result
        System.out.println("\nResult contains " + result.size() + " available time slots:");
        
        List<AvailableTimeSlotResponse> todaySlots = new ArrayList<>();
        List<AvailableTimeSlotResponse> tomorrowSlots = new ArrayList<>();
        
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println(" - " + slot.getDate() + " " + slot.getStartTime() + " to " + slot.getEndTime() 
                    + " (" + slot.getDurationMinutes() + " minutes)");
                    
            if (slot.getDate().equals(today)) {
                todaySlots.add(slot);
            } else if (slot.getDate().equals(tomorrow)) {
                tomorrowSlots.add(slot);
            }
        }
        
        // Verify
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify that slots for both dates are included
        System.out.println("\nToday's slots: " + todaySlots.size());
        System.out.println("Tomorrow's slots: " + tomorrowSlots.size());
        
        boolean hasToday = !todaySlots.isEmpty();
        boolean hasTomorrow = !tomorrowSlots.isEmpty();
        
        assertTrue(hasToday, "Should include slots for today");
        assertTrue(hasTomorrow, "Should include slots for tomorrow");
        
        // Verify that we have appropriate cooking times reflected in the slots
        boolean allTodaySlotsValid = true;
        boolean allTomorrowSlotsValid = true;
        
        System.out.println("\nChecking start times for today's slots (should be after 9:30):");
        for (AvailableTimeSlotResponse slot : todaySlots) {
            System.out.println(" - " + slot.getStartTime() + " > 9:30? " + !slot.getStartTime().isBefore(LocalTime.of(9, 30)));
            
            // For today with menu (1h cooking time)
            // 8:00 + 1h cooking + 0.5h travel = 9:30 earliest possible start time
            if (slot.getStartTime().isBefore(LocalTime.of(9, 30))) {
                allTodaySlotsValid = false;
            }
        }
        
        System.out.println("\nChecking start times for tomorrow's slots (should be after 10:00):");
        for (AvailableTimeSlotResponse slot : tomorrowSlots) {
            System.out.println(" - " + slot.getStartTime() + " > 10:00? " + !slot.getStartTime().isBefore(LocalTime.of(10, 0)));
            
            // For tomorrow with dishes (1.5h cooking time)
            // 8:00 + 1.5h cooking + 0.5h travel = 10:00 earliest possible start time
            if (slot.getStartTime().isBefore(LocalTime.of(10, 0))) {
                allTomorrowSlotsValid = false;
            }
        }
        
        System.out.println("\nAll today's slots valid (start after 9:30)? " + allTodaySlotsValid + " (should be true)");
        System.out.println("All tomorrow's slots valid (start after 10:00)? " + allTomorrowSlotsValid + " (should be true)");
        
        if (!allTodaySlotsValid) {
            fail("Slots for today should account for 1h cooking + 0.5h travel time");
        }
        
        if (!allTomorrowSlotsValid) {
            fail("Slots for tomorrow should account for 1.5h cooking + 0.5h travel time");
        }
        
        System.out.println("===== END DEBUG testFindAvailableTimeSlotsWithMultipleDates =====");
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithMultipleSchedules() {
        // Setup
        String customerLocation = "456 Customer Street";
        int guestCount = 2; // Ít khách => nấu nhanh hơn
        
        System.out.println("===== DEBUG testFindAvailableTimeSlotsWithMultipleSchedules =====");
        System.out.println("Today's date: " + today);
        
        // Mock distance calculation - 30 phút di chuyển
        DistanceResponse distanceResponse = new DistanceResponse(BigDecimal.valueOf(10), BigDecimal.valueOf(0.5));
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(distanceResponse);
        System.out.println("Mocked distance response: " + distanceResponse.getDistanceKm() + "km, " 
                + distanceResponse.getDurationHours() + " hours (30 minutes)");
        
        // Mock cooking time - 30 phút nấu
        when(calculateService.calculateMaxCookTime(anyLong(), anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(0.5));
        System.out.println("Mocked cooking time: 0.5 hours (30 minutes)");
        
        // Tạo nhiều schedule cho chef trong cùng 1 ngày
        // Schedule 1: 8:00 - 12:00
        ChefSchedule morningSchedule = new ChefSchedule();
        morningSchedule.setChef(chef);
        morningSchedule.setDayOfWeek(today.getDayOfWeek().getValue() - 1);
        morningSchedule.setStartTime(LocalTime.of(8, 0));
        morningSchedule.setEndTime(LocalTime.of(12, 0));
        
        // Schedule 2: 14:00 - 16:00
        ChefSchedule afternoonSchedule = new ChefSchedule();
        afternoonSchedule.setChef(chef);
        afternoonSchedule.setDayOfWeek(today.getDayOfWeek().getValue() - 1);
        afternoonSchedule.setStartTime(LocalTime.of(14, 0));
        afternoonSchedule.setEndTime(LocalTime.of(16, 0));
        
        // Schedule 3: 18:00 - 22:00
        ChefSchedule eveningSchedule = new ChefSchedule();
        eveningSchedule.setChef(chef);
        eveningSchedule.setDayOfWeek(today.getDayOfWeek().getValue() - 1);
        eveningSchedule.setStartTime(LocalTime.of(18, 0));
        eveningSchedule.setEndTime(LocalTime.of(22, 0));
        
        System.out.println("Created 3 schedules for today:");
        System.out.println("Morning: " + morningSchedule.getStartTime() + " to " + morningSchedule.getEndTime());
        System.out.println("Afternoon: " + afternoonSchedule.getStartTime() + " to " + afternoonSchedule.getEndTime());
        System.out.println("Evening: " + eveningSchedule.getStartTime() + " to " + eveningSchedule.getEndTime());
        
        // Setup schedule mock
        int adjustedDayOfWeek = today.getDayOfWeek().getValue() - 1;
        System.out.println("Adjusted dayOfWeek for database query: " + adjustedDayOfWeek);
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), eq(adjustedDayOfWeek)))
                .thenReturn(List.of(morningSchedule, afternoonSchedule, eveningSchedule));
        
        // Tạo các booking ngăn cách các schedules
        // Booking 1: 10:00 - 11:00 (trong morning schedule)
        Booking booking1 = new Booking();
        booking1.setChef(chef);
        booking1.setStatus("CONFIRMED");
        booking1.setIsDeleted(false);
        
        BookingDetail bookingDetail1 = new BookingDetail();
        bookingDetail1.setBooking(booking1);
        bookingDetail1.setSessionDate(today);
        bookingDetail1.setStartTime(LocalTime.of(11, 0)); // Thời gian kết thúc booking là 11:00 (khách bắt đầu ăn)
        bookingDetail1.setTimeBeginCook(LocalTime.of(10, 30)); // Chef bắt đầu nấu ăn lúc 10:30
        bookingDetail1.setTimeBeginTravel(LocalTime.of(10, 0)); // Thời gian bắt đầu booking là 10:00
        bookingDetail1.setStatus("CONFIRMED");
        bookingDetail1.setIsDeleted(false);
        bookingDetail1.setLocation(customerLocation);
        
        // Booking 2: 15:00 - 16:00 (chiếm hết nửa sau của afternoon schedule)
        Booking booking2 = new Booking();
        booking2.setChef(chef);
        booking2.setStatus("CONFIRMED");
        booking2.setIsDeleted(false);
        
        BookingDetail bookingDetail2 = new BookingDetail();
        bookingDetail2.setBooking(booking2);
        bookingDetail2.setSessionDate(today);
        bookingDetail2.setStartTime(LocalTime.of(16, 0)); // Thời gian kết thúc booking là 16:00
        bookingDetail2.setTimeBeginCook(LocalTime.of(15, 30)); // Chef bắt đầu nấu ăn lúc 15:30
        bookingDetail2.setTimeBeginTravel(LocalTime.of(15, 0)); // Thời gian bắt đầu booking là 15:00
        bookingDetail2.setStatus("CONFIRMED");
        bookingDetail2.setIsDeleted(false);
        bookingDetail2.setLocation(customerLocation);
        
        System.out.println("Created 2 bookings:");
        System.out.println("Booking 1: " + bookingDetail1.getTimeBeginTravel() + " to " + bookingDetail1.getStartTime() 
                + " (travels at " + bookingDetail1.getTimeBeginTravel() + ", cooks at " + bookingDetail1.getTimeBeginCook() + ")");
        System.out.println("Booking 2: " + bookingDetail2.getTimeBeginTravel() + " to " + bookingDetail2.getStartTime()
                + " (travels at " + bookingDetail2.getTimeBeginTravel() + ", cooks at " + bookingDetail2.getTimeBeginCook() + ")");
        
        // Thêm booking vào danh sách
        List<BookingDetail> bookings = new ArrayList<>();
        bookings.add(bookingDetail1);
        bookings.add(bookingDetail2);
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), eq(today)))
                .thenReturn(bookings);
        
        // Đảm bảo không có blocked dates
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), eq(today)))
                .thenReturn(new ArrayList<>());
        
        // Thời gian cơ bản cần thiết (30 phút di chuyển + 30 phút nấu = 1 giờ)
        LocalTime baseTime = LocalTime.of(9, 0); // 8:00 + 1h = 9:00 là thời gian sớm nhất có thể bắt đầu
        System.out.println("With 30min travel + 30min cooking, earliest possible start time is: " + baseTime);
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, today, customerLocation, null, null, guestCount, 6);
        
        // Debug output for the result
        System.out.println("\nResult contains " + result.size() + " available time slots:");
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println(" - " + slot.getDate() + " " + slot.getStartTime() + " to " + slot.getEndTime() 
                    + " (" + slot.getDurationMinutes() + " minutes)");
        }
        
        // Verify
        assertNotNull(result);
        
        // Expected slots based on the problem description:
        // 1. Morning slot: 9:00 - 10:00 (before booking1 starts at 10:00)
        // 2. Evening slot: 19:00 - 22:00 (after adding cook and travel time to evening schedule)
        // Note: The afternoon slot (14:00-15:00) is invalid because when adding travel + cooking time (1h),
        // it would conflict with the booking at 15:00

        // Kiểm tra tổng số slot
        System.out.println("Expected number of slots: 2");
        assertEquals(2, result.size(), "Should have exactly 2 available time slots");
        
        // Kiểm tra các slot cụ thể
        boolean hasSlot1 = false; // 9:00 - 10:00
        boolean hasSlot3 = false; // 19:00 - 22:00
        
        for (AvailableTimeSlotResponse slot : result) {
            // Kiểm tra slot 1: 9:00 - 10:00
            if (slot.getStartTime().equals(LocalTime.of(9, 0)) && 
                slot.getEndTime().equals(LocalTime.of(10, 0))) {
                hasSlot1 = true;
                System.out.println("Found expected morning slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
            
            // Kiểm tra slot 3: 19:00 - 22:00
            if (slot.getStartTime().equals(LocalTime.of(19, 0)) && 
                slot.getEndTime().equals(LocalTime.of(22, 0))) {
                hasSlot3 = true;
                System.out.println("Found expected evening slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
        }
        
        // Verify tất cả các slot
        System.out.println("Has morning slot (9:00-10:00)? " + hasSlot1 + " (should be true)");
        assertTrue(hasSlot1, "Should have a slot from 9:00 to 10:00");
        
        System.out.println("Has evening slot (19:00-22:00)? " + hasSlot3 + " (should be true)");
        assertTrue(hasSlot3, "Should have a slot from 19:00 to 22:00");
        
        System.out.println("===== END DEBUG testFindAvailableTimeSlotsWithMultipleSchedules =====");
    }
    
    @Test
    public void testFindAvailableTimeSlotsWithSplitSchedulesAndBookings() {
        // Setup
        String customerLocation = "456 Customer Street";
        int guestCount = 2; // Ít khách => nấu nhanh hơn
        
        System.out.println("===== DEBUG testFindAvailableTimeSlotsWithSplitSchedulesAndBookings =====");
        System.out.println("Today's date: " + today);
        
        // Mock distance calculation - 30 phút di chuyển
        DistanceResponse distanceResponse = new DistanceResponse(BigDecimal.valueOf(10), BigDecimal.valueOf(0.5));
        when(distanceService.calculateDistanceAndTime(any(), any())).thenReturn(distanceResponse);
        System.out.println("Mocked distance response: " + distanceResponse.getDistanceKm() + "km, " 
                + distanceResponse.getDurationHours() + " hours (30 minutes)");
        
        // Mock cooking time - 30 phút nấu
        when(calculateService.calculateMaxCookTime(anyLong(), anyInt(), anyInt())).thenReturn(BigDecimal.valueOf(0.5));
        System.out.println("Mocked cooking time: 0.5 hours (30 minutes)");
        
        // Tạo 2 schedule cho chef trong cùng 1 ngày
        // Schedule 1: 8:00 - 15:00
        ChefSchedule morningSchedule = new ChefSchedule();
        morningSchedule.setChef(chef);
        morningSchedule.setDayOfWeek(today.getDayOfWeek().getValue() - 1);
        morningSchedule.setStartTime(LocalTime.of(8, 0));
        morningSchedule.setEndTime(LocalTime.of(15, 0));
        
        // Schedule 2: 16:00 - 22:00
        ChefSchedule eveningSchedule = new ChefSchedule();
        eveningSchedule.setChef(chef);
        eveningSchedule.setDayOfWeek(today.getDayOfWeek().getValue() - 1);
        eveningSchedule.setStartTime(LocalTime.of(16, 0));
        eveningSchedule.setEndTime(LocalTime.of(22, 0));
        
        System.out.println("Created 2 schedules for today:");
        System.out.println("Morning/Afternoon: " + morningSchedule.getStartTime() + " to " + morningSchedule.getEndTime());
        System.out.println("Evening: " + eveningSchedule.getStartTime() + " to " + eveningSchedule.getEndTime());
        
        // Setup schedule mock
        int adjustedDayOfWeek = today.getDayOfWeek().getValue() - 1;
        System.out.println("Adjusted dayOfWeek for database query: " + adjustedDayOfWeek);
        when(scheduleRepository.findByChefAndDayOfWeekAndIsDeletedFalse(any(), eq(adjustedDayOfWeek)))
                .thenReturn(List.of(morningSchedule, eveningSchedule));
        
        // Tạo 2 booking
        // Booking 1: 10:00 - 11:00 => 11:00 - 12:00 (bắt đầu di chuyển lúc 10:00, bắt đầu nấu lúc 10:30, kết thúc nấu 11:00, khách ăn lúc 12:00)
        Booking booking1 = new Booking();
        booking1.setChef(chef);
        booking1.setStatus("CONFIRMED");
        booking1.setIsDeleted(false);
        
        BookingDetail bookingDetail1 = new BookingDetail();
        bookingDetail1.setBooking(booking1);
        bookingDetail1.setSessionDate(today);
        bookingDetail1.setStartTime(LocalTime.of(12, 0)); // Thời gian kết thúc booking là 12:00 (khách bắt đầu ăn)
        bookingDetail1.setTimeBeginCook(LocalTime.of(11, 0)); // Chef bắt đầu nấu ăn lúc 11:00
        bookingDetail1.setTimeBeginTravel(LocalTime.of(10, 0)); // Thời gian bắt đầu booking là 10:00
        bookingDetail1.setStatus("CONFIRMED");
        bookingDetail1.setIsDeleted(false);
        bookingDetail1.setLocation(customerLocation);
        
        // Booking 2: 18:00 - 19:00 => 19:00 - 20:00 (bắt đầu di chuyển lúc 18:00, bắt đầu nấu lúc 18:30, kết thúc nấu 19:00, khách ăn lúc 20:00)
        Booking booking2 = new Booking();
        booking2.setChef(chef);
        booking2.setStatus("CONFIRMED");
        booking2.setIsDeleted(false);
        
        BookingDetail bookingDetail2 = new BookingDetail();
        bookingDetail2.setBooking(booking2);
        bookingDetail2.setSessionDate(today);
        bookingDetail2.setStartTime(LocalTime.of(20, 0)); // Thời gian kết thúc booking là 20:00
        bookingDetail2.setTimeBeginCook(LocalTime.of(19, 0)); // Chef bắt đầu nấu ăn lúc 19:00
        bookingDetail2.setTimeBeginTravel(LocalTime.of(18, 0)); // Thời gian bắt đầu booking là 18:00
        bookingDetail2.setStatus("CONFIRMED");
        bookingDetail2.setIsDeleted(false);
        bookingDetail2.setLocation(customerLocation);
        
        System.out.println("Created 2 bookings:");
        System.out.println("Booking 1: " + bookingDetail1.getTimeBeginTravel() + " to " + bookingDetail1.getStartTime() 
                + " (travels at " + bookingDetail1.getTimeBeginTravel() + ", cooks at " + bookingDetail1.getTimeBeginCook() + ")");
        System.out.println("Booking 2: " + bookingDetail2.getTimeBeginTravel() + " to " + bookingDetail2.getStartTime()
                + " (travels at " + bookingDetail2.getTimeBeginTravel() + ", cooks at " + bookingDetail2.getTimeBeginCook() + ")");
        
        // Thêm booking vào danh sách
        List<BookingDetail> bookings = new ArrayList<>();
        bookings.add(bookingDetail1);
        bookings.add(bookingDetail2);
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(any(), eq(today)))
                .thenReturn(bookings);
        
        // Đảm bảo không có blocked dates
        when(blockedDateRepository.findByChefAndBlockedDateAndIsDeletedFalse(any(), eq(today)))
                .thenReturn(new ArrayList<>());
        
        // Thời gian cơ bản cần thiết (30 phút di chuyển + 30 phút nấu = 1 giờ)
        LocalTime baseTime = LocalTime.of(9, 0); // 8:00 + 1h = 9:00 là thời gian sớm nhất có thể bắt đầu
        System.out.println("With 30min travel + 30min cooking, earliest possible start time is: " + baseTime);
        
        // Test method
        List<AvailableTimeSlotResponse> result = availabilityFinderService.findAvailableTimeSlotsWithInSingleDate(
                1L, today, customerLocation, null, null, guestCount, 6);
        
        // Debug output for the result
        System.out.println("\nResult contains " + result.size() + " available time slots:");
        for (AvailableTimeSlotResponse slot : result) {
            System.out.println(" - " + slot.getDate() + " " + slot.getStartTime() + " to " + slot.getEndTime() 
                    + " (" + slot.getDurationMinutes() + " minutes)");
        }
        
        // Verify
        assertNotNull(result);
        
        // Expected slots based on the problem description:
        // 1. Morning slot: 9:00 - 10:00 (before booking1 starts at 10:00)
        // 2. Afternoon slot: 13:30 - 15:00 (after booking1 ends at 12:00 plus prep time)
        // 3. Early evening slot: 17:00 - 18:00 (between start of evening schedule and booking2)
        // 4. Late evening slot: 21:30 - 22:00 (after booking2 ends at 20:00 plus prep time)

        // Kiểm tra tổng số slot
        System.out.println("Expected number of slots: 4");
        assertEquals(4, result.size(), "Should have exactly 4 available time slots");
        
        // Kiểm tra các slot cụ thể
        boolean hasSlot1 = false; // 9:00 - 10:00
        boolean hasSlot2 = false; // 13:30 - 15:00
        boolean hasSlot3 = false; // 17:00 - 18:00
        boolean hasSlot4 = false; // 21:30 - 22:00
        
        for (AvailableTimeSlotResponse slot : result) {
            // Kiểm tra slot 1: 9:00 - 10:00
            if (slot.getStartTime().equals(LocalTime.of(9, 0)) && 
                slot.getEndTime().equals(LocalTime.of(10, 0))) {
                hasSlot1 = true;
                System.out.println("Found expected morning slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
            
            // Kiểm tra slot 2: 13:30 - 15:00
            if (slot.getStartTime().equals(LocalTime.of(13, 30)) && 
                slot.getEndTime().equals(LocalTime.of(15, 0))) {
                hasSlot2 = true;
                System.out.println("Found expected afternoon slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
            
            // Kiểm tra slot 3: 17:00 - 18:00
            if (slot.getStartTime().equals(LocalTime.of(17, 0)) && 
                slot.getEndTime().equals(LocalTime.of(18, 0))) {
                hasSlot3 = true;
                System.out.println("Found expected early evening slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
            
            // Kiểm tra slot 4: 21:30 - 22:00
            if (slot.getStartTime().equals(LocalTime.of(21, 30)) && 
                slot.getEndTime().equals(LocalTime.of(22, 0))) {
                hasSlot4 = true;
                System.out.println("Found expected late evening slot: " + slot.getStartTime() + "-" + slot.getEndTime());
            }
        }
        
        // Verify tất cả các slot
        System.out.println("Has morning slot (9:00-10:00)? " + hasSlot1 + " (should be true)");
        assertTrue(hasSlot1, "Should have a slot from 9:00 to 10:00");
        
        System.out.println("Has afternoon slot (13:30-15:00)? " + hasSlot2 + " (should be true)");
        assertTrue(hasSlot2, "Should have a slot from 13:30 to 15:00");
        
        System.out.println("Has early evening slot (17:00-18:00)? " + hasSlot3 + " (should be true)");
        assertTrue(hasSlot3, "Should have a slot from 17:00 to 18:00");
        
        System.out.println("Has late evening slot (21:30-22:00)? " + hasSlot4 + " (should be true)");
        assertTrue(hasSlot4, "Should have a slot from 21:30 to 22:00");
        
        System.out.println("===== END DEBUG testFindAvailableTimeSlotsWithSplitSchedulesAndBookings =====");
    }
} 