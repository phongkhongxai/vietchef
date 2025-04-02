package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefTimeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChefTimeSettingsRepository extends JpaRepository<ChefTimeSettings, Long> {
    /**
     * Tìm cài đặt thời gian theo chef
     */
    Optional<ChefTimeSettings> findByChef(Chef chef);
} 