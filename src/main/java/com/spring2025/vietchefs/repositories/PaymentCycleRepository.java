package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.PaymentCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentCycleRepository extends JpaRepository<PaymentCycle, Long> {
    @Query("SELECT p FROM PaymentCycle p WHERE p.booking = :booking ORDER BY p.cycleOrder")
    List<PaymentCycle> findByBookingOrderByCycleOrder(@Param("booking") Booking booking);

    @Query("SELECT p FROM PaymentCycle p WHERE p.booking.id = :bookingId ORDER BY p.cycleOrder")
    List<PaymentCycle> findByBookingId(@Param("bookingId") Long bookingId);
    List<PaymentCycle>findByBookingOrderByCycleOrderAsc(Booking booking);
    PaymentCycle findByBookingAndCycleOrder(Booking booking, int cycleOrder);
    List<PaymentCycle> findByDueDateBeforeAndStatus(LocalDate localDate, String status);
    List<PaymentCycle> findByBookingAndDueDateAfter(Booking booking,LocalDate localDate);
    List<PaymentCycle> findByDueDateInAndStatus(List<LocalDate> dueDates, String status);



}
