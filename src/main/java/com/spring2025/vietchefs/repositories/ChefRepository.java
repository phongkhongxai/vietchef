package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface ChefRepository extends JpaRepository<Chef, Long> {
    Optional<Chef> findByUser(User user);
    Optional<Chef> findByUserId(Long userId);
    Page<Chef> findByStatusAndIsDeletedFalse(String status, Pageable pageable);
    @Query(value = """
    SELECT * FROM chefs c
    WHERE c.status = :status
      AND c.is_deleted = false
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
           COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
           COS(RADIANS(c.longitude) - RADIANS(:lng)) +
           SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            countQuery = """
    SELECT COUNT(*) FROM chefs c
    WHERE c.status = :status
      AND c.is_deleted = false
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
           COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
           COS(RADIANS(c.longitude) - RADIANS(:lng)) +
           SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    Page<Chef> findByStatusAndIsDeletedFalseAndDistance(
            @Param("status") String status,
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double maxDistance,
            Pageable pageable
    );


    @Query("SELECT c FROM Chef c LEFT JOIN FETCH c.packages WHERE c.id = :chefId")
    Optional<Chef> findWithPackagesById(@Param("chefId") Long chefId);
    @Query("""
    SELECT c FROM Chef c
    WHERE 
        (LOWER(c.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) 
         OR LOWER(c.bio) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND c.status = :status 
        AND c.isDeleted = false
""")
    Page<Chef> searchByFullNameOrBioAndStatus(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
    @Query(value = """
    SELECT c.* FROM chefs c
    JOIN users u ON c.user_id = u.user_id
    WHERE c.status = :status
      AND c.is_deleted = false
      AND (LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
           OR LOWER(c.bio) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
           COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
           COS(RADIANS(c.longitude) - RADIANS(:lng)) +
           SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            countQuery = """
    SELECT COUNT(*) FROM chefs c
    JOIN users u ON c.user_id = u.user_id
    WHERE c.status = :status
      AND c.is_deleted = false
      AND (LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
           OR LOWER(c.bio) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
           COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
           COS(RADIANS(c.longitude) - RADIANS(:lng)) +
           SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    Page<Chef> searchChefByKeywordStatusAndDistance(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double maxDistance,
            Pageable pageable
    );

    // Statistics queries
    @Query("SELECT COUNT(c) FROM Chef c WHERE c.status = :status AND c.isDeleted = false")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(c) FROM Chef c WHERE c.isDeleted = false")
    long countActiveChefs();

    // Chef ranking queries
    @Query("SELECT c FROM Chef c WHERE c.status = :status AND c.isDeleted = false ORDER BY c.id LIMIT :limit")
    List<Chef> findByStatusAndLimit(@Param("status") String status, @Param("limit") int limit);

    @Query("SELECT c FROM Chef c WHERE c.status = :status AND c.isDeleted = false ORDER BY c.reputationPoints DESC LIMIT :limit")
    List<Chef> findByStatusOrderByReputationDesc(@Param("status") String status, @Param("limit") int limit);

}
