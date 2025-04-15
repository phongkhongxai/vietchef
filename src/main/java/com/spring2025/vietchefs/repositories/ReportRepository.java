package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

@Repository
@EnableJpaRepositories
public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("SELECT p FROM Report p WHERE p.isDeleted = false")
    Page<Report> findAllNotDeleted(Pageable pageable);
    Page<Report> findByReasonAndIsDeletedFalse(String reason, Pageable pageable);
}
