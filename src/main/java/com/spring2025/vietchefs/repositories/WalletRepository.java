package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    boolean existsByUserId(Long userId);
    Optional<Wallet> findByUserId(Long userId);
}
