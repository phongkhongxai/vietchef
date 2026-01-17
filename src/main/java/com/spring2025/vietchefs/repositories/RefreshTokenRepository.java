package com.spring2025.vietchefs.repositories;




import com.spring2025.vietchefs.models.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String refreshToken);
    @Modifying
    @Transactional // Có thể đặt ở đây hoặc ở Service
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.expired = true WHERE r.user.id = :userId")
    void revokeAllByUserId(Long userId);
    boolean existsByUserId(Long userId);

}
