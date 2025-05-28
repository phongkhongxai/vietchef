package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.CustomerTransaction;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    List<CustomerTransaction> findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(Long bookingId, String transactionType, String status);
    boolean existsByBookingIdAndTransactionType(Long bookingId, String transactionType);
    Page<CustomerTransaction> findByWalletAndIsDeletedFalse(Wallet wallet, Pageable pageable);
    List<CustomerTransaction> findByWalletAndIsDeletedFalse(Wallet wallet);

    // Statistics queries
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CustomerTransaction ct WHERE ct.transactionType = 'PAYMENT' AND ct.status = 'COMPLETED'")
    java.math.BigDecimal findTotalRevenue();

    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CustomerTransaction ct WHERE ct.transactionType = 'PAYMENT' AND ct.status = 'COMPLETED' AND ct.createdAt >= :startDate")
    java.math.BigDecimal findRevenueFromDate(@Param("startDate") java.time.LocalDateTime startDate);

    // Date-based analytics queries for trend charts
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CustomerTransaction ct WHERE ct.transactionType = 'PAYMENT' AND ct.status = 'COMPLETED' AND DATE(ct.createdAt) = :date")
    java.math.BigDecimal findRevenueByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COUNT(ct) FROM CustomerTransaction ct WHERE ct.transactionType = 'PAYMENT' AND ct.status = 'COMPLETED' AND DATE(ct.createdAt) = :date")
    Long countTransactionsByDate(@Param("date") java.time.LocalDate date);

    // Date range queries for seasonal analysis
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CustomerTransaction ct WHERE ct.transactionType = 'PAYMENT' AND ct.status = 'COMPLETED' AND DATE(ct.createdAt) BETWEEN :startDate AND :endDate")
    java.math.BigDecimal findRevenueByDateRange(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

}
