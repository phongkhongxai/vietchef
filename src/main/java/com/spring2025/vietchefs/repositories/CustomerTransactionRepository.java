package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.CustomerTransaction;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    List<CustomerTransaction> findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(Long bookingId, String transactionType, String status);
    boolean existsByBookingIdAndTransactionType(Long bookingId, String transactionType);
    Page<CustomerTransaction> findByWalletAndIsDeletedFalse(Wallet wallet, Pageable pageable);
    List<CustomerTransaction> findByWalletAndIsDeletedFalse(Wallet wallet);


}
