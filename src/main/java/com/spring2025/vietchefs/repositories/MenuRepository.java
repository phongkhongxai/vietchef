package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@EnableJpaRepositories
public interface MenuRepository extends JpaRepository<Menu, Long> {
    Page<Menu> findByChefAndIsDeletedFalse(Chef chef, Pageable pageable);
    List<Menu> findByChefAndIsDeletedFalse(Chef chef);
}
