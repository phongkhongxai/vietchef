package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ChefTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChefTransactionRepository extends JpaRepository<ChefTransaction, Long> {
}
