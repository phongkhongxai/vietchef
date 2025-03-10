package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.ChefSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChefScheduleRepository extends JpaRepository<ChefSchedule, Long> {
    List<ChefSchedule> findByChefAndIsDeletedFalse(Chef chef);
}