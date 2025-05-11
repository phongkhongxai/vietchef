package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.FavoriteChef;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface FavoriteChefRepository extends JpaRepository<FavoriteChef, Long> {
    Optional<FavoriteChef> findByUserAndChefAndIsDeletedFalse(User user, Chef chef);
    Optional<FavoriteChef> findByUserAndChef(User user, Chef chef);
    Page<FavoriteChef> findByUserAndIsDeletedFalse(User user, Pageable pageable);
    Page<FavoriteChef> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);
    @Query(value = """
    SELECT fc.* FROM favorite_chefs fc
    JOIN chefs c ON fc.chef_id = c.id
    WHERE fc.user_id = :userId
      AND fc.is_deleted = false
      AND c.status = :status
      AND c.is_deleted = false
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
           COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
           COS(RADIANS(c.longitude) - RADIANS(:lng)) +
           SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            countQuery = """
    SELECT COUNT(*) FROM favorite_chefs fc
    JOIN chefs c ON fc.chef_id = c.id
    WHERE fc.user_id = :userId
      AND fc.is_deleted = false
      AND c.status = :status
      AND c.is_deleted = false
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
           COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
           COS(RADIANS(c.longitude) - RADIANS(:lng)) +
           SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    Page<FavoriteChef> findFavoriteChefsNearBy(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double maxDistance,
            Pageable pageable
    );

    boolean existsByUserAndChefAndIsDeletedFalse(User user, Chef chef);
} 