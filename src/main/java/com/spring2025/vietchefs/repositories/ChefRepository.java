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

import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface ChefRepository extends JpaRepository<Chef, Long> {
    Optional<Chef> findByUser(User user);
    Optional<Chef> findByUserId(Long userId);
    Page<Chef> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    @Query("SELECT c FROM Chef c LEFT JOIN FETCH c.packages WHERE c.id = :chefId")
    Optional<Chef> findWithPackagesById(@Param("chefId") Long chefId);
}
