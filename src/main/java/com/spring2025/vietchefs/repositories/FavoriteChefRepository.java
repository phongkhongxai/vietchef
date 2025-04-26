package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.FavoriteChef;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface FavoriteChefRepository extends JpaRepository<FavoriteChef, Long> {
    Optional<FavoriteChef> findByUserAndChefAndIsDeletedFalse(User user, Chef chef);
    Page<FavoriteChef> findByUserAndIsDeletedFalse(User user, Pageable pageable);
    Page<FavoriteChef> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);
    boolean existsByUserAndChefAndIsDeletedFalse(User user, Chef chef);
} 