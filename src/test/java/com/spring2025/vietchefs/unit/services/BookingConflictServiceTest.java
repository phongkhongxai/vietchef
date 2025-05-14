package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.repositories.BookingDetailRepository;
import com.spring2025.vietchefs.repositories.BookingRepository;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.services.impl.BookingConflictServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingConflictServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @Mock
    private ChefRepository chefRepository;

    @InjectMocks
    private BookingConflictServiceImpl bookingConflictService;

    private Chef testChef;
    private User testUser;
    private Booking activeBooking;
    private Booking inactiveBooking;
    private BookingDetail activeBookingDetail;
    private BookingDetail inactiveBookingDetail;
    private LocalDate testDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @BeforeEach
    void setUp() {
        // Set up test chef
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        testChef = new Chef();
        testChef.setId(1L);
        testChef.setUser(testUser);
        testChef.setStatus("ACTIVE");

        // Set up test date and times
        testDate = LocalDate.now();
        startTime = LocalTime.of(10, 0); // 10:00 AM
        endTime = LocalTime.of(12, 0);   // 12:00 PM

        // Set up active booking
        activeBooking = new Booking();
        activeBooking.setId(1L);
        activeBooking.setChef(testChef);
        activeBooking.setStatus("CONFIRMED");
        activeBooking.setIsDeleted(false);

        // Set up inactive booking
        inactiveBooking = new Booking();
        inactiveBooking.setId(2L);
        inactiveBooking.setChef(testChef);
        inactiveBooking.setStatus("CANCELED");
        inactiveBooking.setIsDeleted(false);

        // Set up active booking detail
        activeBookingDetail = new BookingDetail();
        activeBookingDetail.setId(1L);
        activeBookingDetail.setBooking(activeBooking);
        activeBookingDetail.setSessionDate(testDate);
        activeBookingDetail.setTimeBeginTravel(LocalTime.of(9, 30)); // Travel starts at 9:30 AM
        activeBookingDetail.setStartTime(LocalTime.of(11, 0));       // Cooking starts at 11:00 AM
        activeBookingDetail.setStatus("CONFIRMED");
        activeBookingDetail.setIsDeleted(false);

        // Set up inactive booking detail
        inactiveBookingDetail = new BookingDetail();
        inactiveBookingDetail.setId(2L);
        inactiveBookingDetail.setBooking(inactiveBooking);
        inactiveBookingDetail.setSessionDate(testDate);
        inactiveBookingDetail.setTimeBeginTravel(LocalTime.of(9, 30));
        inactiveBookingDetail.setStartTime(LocalTime.of(11, 0));
        inactiveBookingDetail.setStatus("CANCELED");
        inactiveBookingDetail.setIsDeleted(false);
    }

    // ==================== hasBookingConflict Tests ====================

    @Test
    @DisplayName("Test 1: hasBookingConflict with conflicting active booking should return true")
    void hasBookingConflict_WithConflictingActiveBooking_ShouldReturnTrue() {
        // Arrange
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(activeBookingDetail);
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflict(testChef, testDate, startTime, endTime);
        
        // Assert
        assertTrue(result);
        verify(bookingDetailRepository).findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate);
    }
    
    @Test
    @DisplayName("Test 2: hasBookingConflict with non-conflicting time range should return false")
    void hasBookingConflict_WithNonConflictingTimeRange_ShouldReturnFalse() {
        // Arrange
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(activeBookingDetail);
        
        // Set non-conflicting time (after existing booking)
        LocalTime laterStartTime = LocalTime.of(13, 0); // 1:00 PM
        LocalTime laterEndTime = LocalTime.of(15, 0);   // 3:00 PM
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflict(testChef, testDate, laterStartTime, laterEndTime);
        
        // Assert
        assertFalse(result);
        verify(bookingDetailRepository).findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate);
    }
    
    @Test
    @DisplayName("Test 3: hasBookingConflict with inactive booking should return false")
    void hasBookingConflict_WithInactiveBooking_ShouldReturnFalse() {
        // Arrange
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(inactiveBookingDetail);
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflict(testChef, testDate, startTime, endTime);
        
        // Assert
        assertFalse(result);
        verify(bookingDetailRepository).findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate);
    }
    
    @Test
    @DisplayName("Test 4: hasBookingConflict with no bookings should return false")
    void hasBookingConflict_WithNoBookings_ShouldReturnFalse() {
        // Arrange
        List<BookingDetail> emptyBookingDetails = new ArrayList<>();
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate))
                .thenReturn(emptyBookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflict(testChef, testDate, startTime, endTime);
        
        // Assert
        assertFalse(result);
        verify(bookingDetailRepository).findByBooking_ChefAndSessionDateAndIsDeletedFalse(testChef, testDate);
    }

    // ==================== hasBookingConflictOnDayOfWeek Tests ====================
    
    @Test
    @DisplayName("Test 1: hasBookingConflictOnDayOfWeek with conflicting active booking should return true")
    void hasBookingConflictOnDayOfWeek_WithConflictingActiveBooking_ShouldReturnTrue() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7; // Convert to 0-6 (Sunday = 0)
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(activeBookingDetail);
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflictOnDayOfWeek(testChef, dayOfWeek, startTime, endTime, 7);
        
        // Assert
        assertTrue(result);
        verify(bookingDetailRepository, atLeastOnce()).findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class));
    }
    
    @Test
    @DisplayName("Test 2: hasBookingConflictOnDayOfWeek with non-conflicting time range should return false")
    void hasBookingConflictOnDayOfWeek_WithNonConflictingTimeRange_ShouldReturnFalse() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7;
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(activeBookingDetail);
        
        // Set non-conflicting time
        LocalTime laterStartTime = LocalTime.of(13, 0);
        LocalTime laterEndTime = LocalTime.of(15, 0);
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflictOnDayOfWeek(testChef, dayOfWeek, laterStartTime, laterEndTime, 7);
        
        // Assert
        assertFalse(result);
        verify(bookingDetailRepository, atLeastOnce()).findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class));
    }
    
    @Test
    @DisplayName("Test 3: hasBookingConflictOnDayOfWeek with inactive booking should return false")
    void hasBookingConflictOnDayOfWeek_WithInactiveBooking_ShouldReturnFalse() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7;
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(inactiveBookingDetail);
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflictOnDayOfWeek(testChef, dayOfWeek, startTime, endTime, 7);
        
        // Assert
        assertFalse(result);
        verify(bookingDetailRepository, atLeastOnce()).findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class));
    }
    
    @Test
    @DisplayName("Test 4: hasBookingConflictOnDayOfWeek with no bookings should return false")
    void hasBookingConflictOnDayOfWeek_WithNoBookings_ShouldReturnFalse() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7;
        List<BookingDetail> emptyBookingDetails = new ArrayList<>();
        
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(emptyBookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasBookingConflictOnDayOfWeek(testChef, dayOfWeek, startTime, endTime, 7);
        
        // Assert
        assertFalse(result);
        verify(bookingDetailRepository, atLeastOnce()).findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class));
    }

    // ==================== hasActiveBookingsForDayOfWeek Tests ====================
    
    @Test
    @DisplayName("Test 1: hasActiveBookingsForDayOfWeek with active booking should return true")
    void hasActiveBookingsForDayOfWeek_WithActiveBooking_ShouldReturnTrue() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7;
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(activeBookingDetail);
        
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasActiveBookingsForDayOfWeek(testChef.getId(), dayOfWeek);
        
        // Assert
        assertTrue(result);
        verify(chefRepository).findById(testChef.getId());
        verify(bookingDetailRepository, atLeastOnce()).findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class));
    }
    
    @Test
    @DisplayName("Test 2: hasActiveBookingsForDayOfWeek with inactive booking should return false")
    void hasActiveBookingsForDayOfWeek_WithInactiveBooking_ShouldReturnFalse() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7;
        List<BookingDetail> bookingDetails = new ArrayList<>();
        bookingDetails.add(inactiveBookingDetail);
        
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(bookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasActiveBookingsForDayOfWeek(testChef.getId(), dayOfWeek);
        
        // Assert
        assertFalse(result);
        verify(chefRepository).findById(testChef.getId());
        verify(bookingDetailRepository, atLeastOnce()).findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class));
    }
    
    @Test
    @DisplayName("Test 3: hasActiveBookingsForDayOfWeek with no bookings should return false")
    void hasActiveBookingsForDayOfWeek_WithNoBookings_ShouldReturnFalse() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7;
        List<BookingDetail> emptyBookingDetails = new ArrayList<>();
        
        when(chefRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class)))
                .thenReturn(emptyBookingDetails);
        
        // Act
        boolean result = bookingConflictService.hasActiveBookingsForDayOfWeek(testChef.getId(), dayOfWeek);
        
        // Assert
        assertFalse(result);
        verify(chefRepository).findById(testChef.getId());
        verify(bookingDetailRepository, atLeastOnce()).findByBooking_ChefAndSessionDateAndIsDeletedFalse(eq(testChef), any(LocalDate.class));
    }
    
    @Test
    @DisplayName("Test 4: hasActiveBookingsForDayOfWeek with non-existent chef should throw exception")
    void hasActiveBookingsForDayOfWeek_WithNonExistentChef_ShouldThrowException() {
        // Arrange
        int dayOfWeek = testDate.getDayOfWeek().getValue() % 7;
        long nonExistentChefId = 99L;
        
        when(chefRepository.findById(nonExistentChefId)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingConflictService.hasActiveBookingsForDayOfWeek(nonExistentChefId, dayOfWeek);
        });
        
        assertTrue(exception.getMessage().contains("Chef not found"));
        verify(chefRepository).findById(nonExistentChefId);
        verifyNoInteractions(bookingDetailRepository);
    }
} 