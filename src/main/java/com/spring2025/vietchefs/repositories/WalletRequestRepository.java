package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.entity.WalletRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WalletRequestRepository extends JpaRepository<WalletRequest, Long> {
    List<WalletRequest> findAllByStatus(String status);

    List<WalletRequest> findAllByUser(User user);
    @Query("SELECT wr FROM WalletRequest wr WHERE wr.user.id = :userId AND wr.createdAt >= :cutoffTime")
    List<WalletRequest> findRecentRequestsByUserId(@Param("userId") Long userId, @Param("cutoffTime") LocalDateTime cutoffTime);

}
