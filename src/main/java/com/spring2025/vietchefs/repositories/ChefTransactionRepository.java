package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ChefTransaction;
import com.spring2025.vietchefs.models.entity.CustomerTransaction;
import com.spring2025.vietchefs.models.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChefTransactionRepository extends JpaRepository<ChefTransaction, Long> {
    List<ChefTransaction> findByBookingDetailIdAndTransactionTypeAndIsDeletedFalseAndStatus(Long bookingDetailId, String transactionType, String status);
    boolean existsByBookingDetailIdAndTransactionType(Long bookingDetailId, String transactionType);
    Page<ChefTransaction> findByWalletAndIsDeletedFalse(Wallet wallet, Pageable pageable);

    // Statistics queries
    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM ChefTransaction ct WHERE ct.wallet.user.id = :chefUserId AND ct.transactionType = 'EARNING' AND ct.status = 'COMPLETED'")
    java.math.BigDecimal findTotalEarningsByChef(@Param("chefUserId") Long chefUserId);

    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM ChefTransaction ct WHERE ct.wallet.user.id = :chefUserId AND ct.transactionType = 'EARNING' AND ct.status = 'COMPLETED' AND ct.createdAt >= :startDate")
    java.math.BigDecimal findEarningsByChefFromDate(@Param("chefUserId") Long chefUserId, @Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM ChefTransaction ct WHERE ct.wallet.user.id = :chefUserId AND ct.transactionType = 'EARNING' AND ct.status = 'COMPLETED' AND DATE(ct.createdAt) = CURRENT_DATE")
    java.math.BigDecimal findTodayEarningsByChef(@Param("chefUserId") Long chefUserId);

}
