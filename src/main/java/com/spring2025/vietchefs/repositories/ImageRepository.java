package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
