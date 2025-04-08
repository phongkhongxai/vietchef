package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Chef;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Service to check if a chef's schedule conflicts with existing bookings
 */
public interface BookingConflictService {
    
    /**
     * Check if a chef has any bookings during a specific date and time range
     * 
     * @param chef The chef to check bookings for
     * @param date The date to check
     * @param startTime The start time of the period
     * @param endTime The end time of the period
     * @return true if there's a conflict, false otherwise
     */
    boolean hasBookingConflict(Chef chef, LocalDate date, LocalTime startTime, LocalTime endTime);
    
    /**
     * Check if a chef has any bookings on a specific day of the week and time range
     * This is used for checking conflicts with recurring schedule entries
     * 
     * @param chef The chef to check bookings for
     * @param dayOfWeek The day of week (0-6, 0 is Sunday)
     * @param startTime The start time of the period
     * @param endTime The end time of the period
     * @param daysToCheck Number of days ahead to check for bookings
     * @return true if there's a conflict, false otherwise
     */
    boolean hasBookingConflictOnDayOfWeek(Chef chef, Integer dayOfWeek, LocalTime startTime, LocalTime endTime, Integer daysToCheck);
    
    /**
     * Check if a chef has any active bookings for a specific day of week
     * This is used when deleting all schedules for a day of week
     * 
     * @param chefId The ID of the chef to check
     * @param dayOfWeek The day of week (0-6, 0 is Sunday)
     * @return true if there are active bookings, false otherwise
     */
    boolean hasActiveBookingsForDayOfWeek(Long chefId, Integer dayOfWeek);
} 