package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Payment;
import com.spring2025.vietchefs.models.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByWalletAndIsDeletedFalse(Wallet wallet);
    Optional<Payment> findTopByWalletAndPaymentTypeOrderByCreatedAtDesc(Wallet wallet, String paymentType);
}

