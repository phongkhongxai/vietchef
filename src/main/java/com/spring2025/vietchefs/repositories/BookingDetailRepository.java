package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.*;
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
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    Page<BookingDetail> findByBookingAndIsDeletedFalse(Booking booking, Pageable pageable);
    List<BookingDetail> findByBooking(Booking booking);
    List<BookingDetail> findByBookingOrderBySessionDateAsc(Booking booking);

    @Query("SELECT bd FROM BookingDetail bd WHERE bd.booking.id = :bookingId")
    List<BookingDetail> findByBookingId(@Param("bookingId") Long bookingId);
    @Query("SELECT COALESCE(SUM(bd.totalPrice), 0) " +
            "FROM BookingDetail bd " +
            "WHERE bd.booking.id = :bookingId " +
            "AND bd.status NOT IN ('CANCELED', 'OVERDUE')")
    BigDecimal calculateTotalPriceByBooking(@Param("bookingId") Long bookingId);



    /**
     * Tìm tất cả booking detail của một chef vào một ngày cụ thể
     */
    @Query("SELECT bd FROM BookingDetail bd WHERE bd.booking.chef = :chef AND bd.sessionDate = :sessionDate AND bd.isDeleted = false")
    List<BookingDetail> findByBooking_ChefAndSessionDateAndIsDeletedFalse(@Param("chef") Chef chef, @Param("sessionDate") LocalDate sessionDate);
    List<BookingDetail> findBySessionDateAndStatusAndIsDeletedFalse(LocalDate sessionDate, String status);
    List<BookingDetail> findAllByStatusAndIsDeletedFalse(String status);

    @Query("SELECT bd FROM BookingDetail bd " +
            "WHERE bd.booking.chef.id = :chefId " +
            "AND bd.isDeleted = false")
    Page<BookingDetail> findByChefIdAndNotDeleted(
            @Param("chefId") Long chefId,
            Pageable pageable
    );
    Page<BookingDetail> findByBooking_ChefAndStatusIgnoreCaseAndIsDeletedFalse(Chef chef, String status, Pageable pageable);
    Page<BookingDetail> findByBooking_ChefAndStatusInIgnoreCaseAndIsDeletedFalse(Chef chef, List<String> status, Pageable pageable);


    @Query("SELECT bd FROM BookingDetail bd " +
            "WHERE bd.booking.customer.id = :customerId " +
            "AND bd.isDeleted = false")
    Page<BookingDetail> findByCustomerIdAndNotDeleted(
            @Param("customerId") Long customerId,
            Pageable pageable
    );
    Page<BookingDetail> findByBooking_CustomerAndStatusIgnoreCaseAndIsDeletedFalse(User user, String status, Pageable pageable);
    Page<BookingDetail> findByBooking_CustomerAndStatusInIgnoreCaseAndIsDeletedFalse(User user, List<String> status, Pageable pageable);

    @Query("SELECT bd FROM BookingDetail bd " +
            "WHERE bd.status IN ('IN_PROGRESS', 'SCHEDULED', 'SCHEDULED_COMPLETE') " +
            "AND bd.isDeleted = false " +
            "AND bd.sessionDate < :today")
    List<BookingDetail> findOverdueBookingDetails(@Param("today") LocalDate today);



    @Query("SELECT bd.sessionDate, COUNT(bd) " +
            "FROM BookingDetail bd " +
            "WHERE bd.booking.chef = :chef " +
            "AND bd.sessionDate IN :dates " +
            "AND bd.isDeleted = false " +
            "AND bd.status NOT IN ('CANCELED', 'OVERDUE', 'REJECTED') " +
            "AND bd.booking.status NOT IN ('CANCELED', 'OVERDUE', 'REJECTED') " +
            "AND bd.booking.isDeleted = false " +
            "GROUP BY bd.sessionDate")
    List<Object[]> countActiveBookingsByChefAndDates(@Param("chef") Chef chef, @Param("dates") List<LocalDate> dates);
    @Query("SELECT bd.sessionDate, COUNT(bd) FROM BookingDetail bd " +
            "WHERE bd.booking.chef = :chef " +
            "AND bd.sessionDate >= :today " +
            "AND bd.isDeleted = false " +
            "AND bd.status NOT IN ('CANCELED', 'OVERDUE', 'REJECTED') " +
            "AND bd.booking.status NOT IN ('CANCELED', 'OVERDUE', 'REJECTED') " +
            "AND bd.booking.isDeleted = false " +
            "GROUP BY bd.sessionDate")
    List<Object[]> countFutureBookingsByChef(@Param("chef") Chef chef, @Param("today") LocalDate today);
    // Tổng doanh thu từ các buổi ăn đã hoàn tất
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM BookingDetail b WHERE b.isDeleted = false AND b.status = 'COMPLETED'")
    BigDecimal findTotalRevenue();

    // Doanh thu tháng hiện tại từ các buổi ăn đã hoàn tất
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM BookingDetail b WHERE b.isDeleted = false AND b.status = 'COMPLETED' AND MONTH(b.sessionDate) = MONTH(CURRENT_DATE) AND YEAR(b.sessionDate) = YEAR(CURRENT_DATE)")
    BigDecimal findMonthlyRevenue();

    // Hoa hồng nền tảng đã nhận được từ các buổi ăn hoàn tất
    @Query("SELECT COALESCE(SUM(b.platformFee - COALESCE(b.discountAmout, 0)), 0) " +
            "FROM BookingDetail b " +
            "WHERE b.isDeleted = false AND b.status = 'COMPLETED'")
    BigDecimal findSystemCommission();
    // Tổng số tiền đã trả cho đầu bếp
    @Query("SELECT COALESCE(SUM(b.totalChefFeePrice), 0) FROM BookingDetail b WHERE b.isDeleted = false AND b.status = 'COMPLETED'")
    BigDecimal findTotalPayoutsToChefs();
    @Query("SELECT SUM(bd.totalPrice) FROM BookingDetail bd " +
            "WHERE bd.status = 'COMPLETED' AND bd.sessionDate BETWEEN :start AND :end")
    BigDecimal findRevenueBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);
    @Query("SELECT SUM(bd.totalPrice) FROM BookingDetail bd " +
            "WHERE bd.status = 'COMPLETED' AND bd.sessionDate = :today")
    BigDecimal findRevenueForDate(@Param("today") LocalDate today);
    @Query("SELECT COUNT(b) FROM BookingDetail b " +
            "WHERE b.status = 'COMPLETED' AND b.isDeleted = false")
    Long countCompletedTransactions();






}
