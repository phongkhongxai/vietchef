package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    List<CustomerTransaction> findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(Long bookingId, String transactionType, String status);
    boolean existsByBookingIdAndTransactionType(Long bookingId, String transactionType);
}
