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


    Page<Dish> findByFoodTypeAndIsDeletedFalse(FoodType foodType, Pageable pageable);
    @Query("SELECT d FROM Dish d WHERE d.isDeleted = false AND d.id NOT IN " +
            "(SELECT mi.dish.id FROM MenuItem mi WHERE mi.menu.id = :menuId)")
    Page<Dish> findByNotInMenu(Long menuId, Pageable pageable);

    @Query("SELECT d.cookTime FROM Dish d WHERE d.chef.id = :chefId ORDER BY d.cookTime DESC LIMIT 3")
    List<BigDecimal> findTop3LongestCookTimeByChef(@Param("chefId") Long chefId);

}
