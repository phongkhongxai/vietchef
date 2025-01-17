package com.spring2025.vietchefs.repositories;




import com.spring2025.vietchefs.models.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String refreshToken);
}
