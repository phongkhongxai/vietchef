package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.ReviewCriteria;

import java.util.List;

public interface ReviewCriteriaService {
    List<ReviewCriteria> getAllCriteria();
    List<ReviewCriteria> getActiveCriteria();
    ReviewCriteria getCriteriaById(Long id);
    ReviewCriteria createCriteria(ReviewCriteria criteria);
    ReviewCriteria updateCriteria(Long id, ReviewCriteria criteria);
    void deleteCriteria(Long id);
    void initDefaultCriteria();
} 