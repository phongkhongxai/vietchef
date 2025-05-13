package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@EnableJpaRepositories
public interface MenuRepository extends JpaRepository<Menu, Long> {
    @Query("SELECT p FROM Menu p WHERE p.isDeleted = false")
    Page<Menu> findAllNotDeleted(Pageable pageable);
    Page<Menu> findByChefAndIsDeletedFalse(Chef chef, Pageable pageable);
    List<Menu> findByChefAndIsDeletedFalse(Chef chef);
    @Query(value = """
    SELECT m.* FROM menus m
    JOIN chefs c ON m.chef_id = c.id
    WHERE m.is_deleted = false
      AND c.status = 'ACTIVE'
      AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    List<Menu> searchMenusByKeywordAndDistance(
            @Param("keyword") String keyword,
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double maxDistance
    );
    @Query(value = """
    SELECT m.* FROM menus m
    JOIN chefs c ON m.chef_id = c.id
    WHERE m.is_deleted = false
      AND c.status = 'ACTIVE'
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    List<Menu> findMenusNearCustomer(
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double maxDistance
    );


}
