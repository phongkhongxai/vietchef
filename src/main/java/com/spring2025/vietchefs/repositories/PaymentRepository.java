package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Payment;
import com.spring2025.vietchefs.models.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByWalletAndIsDeletedFalse(Wallet wallet);
    Optional<Payment> findTopByWalletAndPaymentTypeOrderByCreatedAtDesc(Wallet wallet, String paymentType);
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE DATE(p.createdAt) = :date AND p.status = 'COMPLETED' AND p.paymentType = 'DEPOSIT'")
    BigDecimal getTotalDepositByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE DATE(p.createdAt) = :date AND p.status = 'COMPLETED' AND p.paymentType = 'PAYOUT'")
    BigDecimal getTotalPayoutByDate(@Param("date") LocalDate date);
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'COMPLETED' AND p.paymentType = 'DEPOSIT'")
    BigDecimal getTotalDepositAllTime();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'COMPLETED' AND p.paymentType = 'PAYOUT'")
    BigDecimal getTotalPayoutAllTime();


}

