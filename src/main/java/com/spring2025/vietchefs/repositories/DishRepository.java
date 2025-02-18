package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.FoodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
@EnableJpaRepositories
public interface DishRepository extends JpaRepository<Dish, Long> {
    @Query("SELECT p FROM Dish p WHERE p.isDeleted = false")
    Page<Dish> findAllNotDeleted(Pageable pageable);

    Page<Dish> findByChefAndIsDeletedFalse(Chef chef, Pageable pageable);

    Page<Dish> findByFoodTypeAndIsDeletedFalse(FoodType foodType, Pageable pageable);

}
