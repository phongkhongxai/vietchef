package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ChefTransaction;
import com.spring2025.vietchefs.models.entity.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChefTransactionRepository extends JpaRepository<ChefTransaction, Long> {
    List<ChefTransaction> findByBookingDetailIdAndTransactionTypeAndIsDeletedFalseAndStatus(Long bookingDetailId, String transactionType, String status);
    boolean existsByBookingDetailIdAndTransactionType(Long bookingDetailId, String transactionType);
}
