package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.entity.WalletRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletRequestRepository extends JpaRepository<WalletRequest, Long> {
    List<WalletRequest> findAllByStatus(String status);

    List<WalletRequest> findAllByUser(User user);
}
