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

import java.math.BigDecimal;
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
    LEFT JOIN FETCH b.bookingDetails bd
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

    // Statistics queries
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.isDeleted = false")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.isDeleted = false")
    long countActiveBookings();

    @Query("SELECT AVG(b.totalPrice) FROM Booking b WHERE b.status = 'COMPLETED' AND b.isDeleted = false")
    java.math.BigDecimal findAverageBookingValue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.chef.id = :chefId AND b.status = :status AND b.isDeleted = false")
    long countByChefIdAndStatus(@Param("chefId") Long chefId, @Param("status") String status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.chef.id = :chefId AND b.isDeleted = false")
    long countByChefId(@Param("chefId") Long chefId);
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.chef.id = :chefId AND b.isDeleted = false AND b.status <> 'PENDING'")
    long countByChefIdExcludingPending(@Param("chefId") Long chefId);


    @Query("SELECT COUNT(DISTINCT b.customer.id) FROM Booking b WHERE b.chef.id = :chefId AND b.status = 'COMPLETED' AND b.isDeleted = false")
    long countUniqueCustomersByChef(@Param("chefId") Long chefId);

    @Query("SELECT AVG(b.totalPrice) FROM Booking b WHERE b.chef.id = :chefId AND b.status = 'COMPLETED' AND b.isDeleted = false")
    java.math.BigDecimal findAverageOrderValueByChef(@Param("chefId") Long chefId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt >= :startDate AND b.isDeleted = false")
    long countBookingsFromDate(@Param("startDate") java.time.LocalDateTime startDate);
    @Query("SELECT COUNT(b) FROM Booking b WHERE DATE(b.createdAt) = :date")
    Long countBookingsByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM Booking b WHERE DATE(b.createdAt) = :date AND b.status = 'COMPLETED'")
    Long countCompletedBookingsByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE DATE(b.createdAt) = :date " +
            "AND b.status IN ('CANCELED', 'REJECTED', 'OVERDUE')")
    Long countCanceledBookingsByDate(@Param("date") LocalDate date);

    @Query("SELECT AVG(b.totalPrice) FROM Booking b WHERE DATE(b.createdAt) = :date")
    BigDecimal averageBookingValueByDate(@Param("date") LocalDate date);
    @Query("SELECT AVG(b.totalPrice) FROM Booking b " +
            "WHERE DATE(b.createdAt) = :date AND b.status = 'COMPLETED'")
    BigDecimal averageBookingCompletedValueByDate(@Param("date") LocalDate date);



    // Date-based analytics queries for trend charts
    @Query("SELECT COUNT(b) FROM Booking b WHERE DATE(b.createdAt) = :date AND b.isDeleted = false")
    Long countBookingsByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'COMPLETED' AND DATE(b.createdAt) = :date AND b.isDeleted = false")
    Long countCompletedBookingsByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN ('CANCELED', 'OVERDUE') AND DATE(b.createdAt) = :date AND b.isDeleted = false")
    Long countCanceledBookingsByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT AVG(b.totalPrice) FROM Booking b WHERE b.status = 'COMPLETED' AND DATE(b.createdAt) = :date AND b.isDeleted = false")
    java.math.BigDecimal findAverageBookingValueByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT AVG(r.rating) FROM Review r JOIN r.booking b WHERE DATE(b.createdAt) = :date AND b.isDeleted = false")
    java.math.BigDecimal findAverageRatingByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COUNT(r) FROM Review r JOIN r.booking b WHERE DATE(b.createdAt) = :date AND b.isDeleted = false")
    Long countReviewsByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COALESCE(SUM(r.rating), 0) FROM Review r JOIN r.booking b WHERE b.status = 'COMPLETED' AND b.isDeleted = false")
    java.math.BigDecimal findTotalRatingSum();

    @Query("SELECT COUNT(r) FROM Review r JOIN r.booking b WHERE b.status = 'COMPLETED' AND b.isDeleted = false")
    Long countTotalRatings();

    // Date range queries for seasonal analysis
    @Query("SELECT COUNT(b) FROM Booking b WHERE DATE(b.createdAt) BETWEEN :startDate AND :endDate AND b.isDeleted = false")
    Long countBookingsByDateRange(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

}
