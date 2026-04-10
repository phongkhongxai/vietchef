package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.InvalidatedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvalidatedAccessTokenRepository extends JpaRepository<InvalidatedAccessToken, String> {
}