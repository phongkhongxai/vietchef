package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.FoodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface DishRepository extends JpaRepository<Dish, Long> {
    @Query("SELECT p FROM Dish p WHERE p.isDeleted = false")
    Page<Dish> findAllNotDeleted(Pageable pageable);

    @Query("SELECT p FROM Dish p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Dish> findByIdNotDeleted(Long id);

    Page<Dish> findByChefAndIsDeletedFalse(Chef chef, Pageable pageable);
    List<Dish> findByChefAndIsDeletedFalse(Chef chef);


    Page<Dish> findByFoodTypesContainingAndIsDeletedFalse(FoodType foodType, Pageable pageable);
    Page<Dish> findDistinctByFoodTypesInAndIsDeletedFalse(List<FoodType> foodTypes, Pageable pageable);
    @Query(value = """
    SELECT DISTINCT d.* FROM dishes d
    JOIN chefs c ON d.chef_id = c.id
    JOIN dish_food_types dft ON d.id = dft.dish_id
    WHERE d.is_deleted = false
      AND dft.food_types_id IN (:foodTypeIds)
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            countQuery = """
    SELECT COUNT(DISTINCT d.id) FROM dishes d
    JOIN chefs c ON d.chef_id = c.id
    JOIN dish_food_types dft ON d.id = dft.dish_id
    WHERE d.is_deleted = false
      AND dft.food_types_id IN (:foodTypeIds)
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    Page<Dish> findDishesByFoodTypesAndDistance(
            @Param("foodTypeIds") List<Long> foodTypeIds,
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double distance,
            Pageable pageable
    );



    @Query("SELECT d FROM Dish d WHERE d.isDeleted = false AND d.chef.id = :chefId AND d.id NOT IN " +
            "(SELECT mi.dish.id FROM MenuItem mi WHERE mi.menu.id = :menuId)")
    Page<Dish> findByNotInMenuAndIsDeletedFalseAndChefId(Long menuId, Long chefId, Pageable pageable);


    @Query("SELECT d.cookTime FROM Dish d WHERE d.chef.id = :chefId ORDER BY d.cookTime DESC LIMIT 3")
    List<BigDecimal> findTop3LongestCookTimeByChef(@Param("chefId") Long chefId);
    Page<Dish> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsDeletedFalse(String nameKeyword, String descKeyword, Pageable pageable);
    @Query(value = """
    SELECT d.* FROM dishes d
    JOIN chefs c ON d.chef_id = c.id
    WHERE d.is_deleted = false
      AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            countQuery = """
    SELECT COUNT(*) FROM dishes d
    JOIN chefs c ON d.chef_id = c.id
    WHERE d.is_deleted = false
      AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    Page<Dish> searchDishesByKeywordAndDistance(
            @Param("keyword") String keyword,
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double maxDistance,
            Pageable pageable
    );


    @Query(value = """
    SELECT d.* FROM dishes d
    JOIN chefs c ON d.chef_id = c.id
    WHERE d.is_deleted = false
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            countQuery = """
    SELECT COUNT(*) FROM dishes d
    JOIN chefs c ON d.chef_id = c.id
    WHERE d.is_deleted = false
      AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
      AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(c.latitude)) *
            COS(RADIANS(c.longitude) - RADIANS(:lng)) +
            SIN(RADIANS(:lat)) * SIN(RADIANS(c.latitude))
      )) <= :distance
    """,
            nativeQuery = true)
    Page<Dish> findDishesNearCustomer(
            @Param("lat") double customerLat,
            @Param("lng") double customerLng,
            @Param("distance") double distance,
            Pageable pageable
    );


}
