package com.spring2025.vietchefs.repositories;

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
public interface FoodTypeRepository extends JpaRepository<FoodType, Long> {
    FoodType findByName(String name);

    @Query("SELECT p FROM FoodType p WHERE p.isDeleted = false")
    List<FoodType> findAllNotDeleted();
}
