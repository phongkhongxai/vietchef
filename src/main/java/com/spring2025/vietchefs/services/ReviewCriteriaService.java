package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ReviewCriteriaRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;

import java.util.List;

public interface ReviewCriteriaService {
    List<ReviewCriteriaResponse> getAllCriteria();
    List<ReviewCriteriaResponse> getActiveCriteria();
    ReviewCriteriaResponse getCriteriaById(Long id);
    ReviewCriteriaResponse createCriteria(ReviewCriteriaRequest request);
    ReviewCriteriaResponse updateCriteria(Long id, ReviewCriteriaRequest request);
    void deleteCriteria(Long id);
    void initDefaultCriteria();
} 