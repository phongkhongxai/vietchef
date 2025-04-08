package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ReviewCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewCriteriaRepository extends JpaRepository<ReviewCriteria, Long> {
    List<ReviewCriteria> findByIsActiveTrue();
    List<ReviewCriteria> findByIsActiveTrueOrderByDisplayOrderAsc();
    Optional<ReviewCriteria> findByName(String name);
} 