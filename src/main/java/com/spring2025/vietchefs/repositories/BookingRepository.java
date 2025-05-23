package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@EnableJpaRepositories
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByCustomerIdAndIsDeletedFalse(Long customerId, Pageable pageable);
    Page<Booking> findByChefIdAndIsDeletedFalse(Long chefId, Pageable pageable);
    Page<Booking> findByChefIdAndStatusNotInAndIsDeletedFalse(Long chefId, List<String> statusList, Pageable pageable);
    Page<Booking> findByCustomerIdAndStatusIgnoreCase(Long customerId, String status, Pageable pageable);
    Page<Booking> findByCustomerIdAndStatusInIgnoreCaseAndIsDeletedFalse(Long customerId, List<String> statuses, Pageable pageable);
    Page<Booking> findByChefIdAndStatusInIgnoreCaseAndIsDeletedFalse(Long chefId, List<String> statuses, Pageable pageable);
    Page<Booking> findByChefIdAndStatusIgnoreCase(Long chefId, String status, Pageable pageable);
    // Tìm tất cả các booking có status trong danh sách (PENDING, PAID, ...)
    List<Booking> findByStatusIn(List<String> statuses);
    // Tìm booking có status trong danh sách AND createdAt < thời điểm now
    List<Booking> findByStatusInAndCreatedAtBefore(List<String> statuses, LocalDateTime dateTime);
    boolean existsByCustomerIdAndChefIdAndBookingTypeIgnoreCaseAndStatusIgnoreCase(
            Long customerId, Long chefId, String bookingType, String status
    );
    @Query("""
    SELECT b FROM Booking b
    WHERE b.status IN ('CONFIRMED', 'CONFIRMED_PAID', 'CONFIRMED_PARTIALLY_PAID')
    AND b.isDeleted = false
    AND NOT EXISTS (
        SELECT 1 FROM BookingDetail d
        WHERE d.booking = b
        AND d.isDeleted = false
        AND d.sessionDate >= :now
    )
""")
    List<Booking> findBookingsWhereAllDetailsBeforeNow(@Param("now") LocalDate now);






}
