package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.ReviewCriteria;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCriteriaRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.repositories.ReviewCriteriaRepository;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewCriteriaServiceImpl implements ReviewCriteriaService {

    private final ReviewCriteriaRepository reviewCriteriaRepository;

    @Autowired
    public ReviewCriteriaServiceImpl(ReviewCriteriaRepository reviewCriteriaRepository) {
        this.reviewCriteriaRepository = reviewCriteriaRepository;
    }

    @PostConstruct
    public void init() {
        initDefaultCriteria();
    }

    @Override
    public List<ReviewCriteriaResponse> getAllCriteria() {
        return reviewCriteriaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewCriteriaResponse> getActiveCriteria() {
        return reviewCriteriaRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewCriteriaResponse getCriteriaById(Long id) {
        ReviewCriteria criteria = reviewCriteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review criteria not found with id: " + id));
        return mapToResponse(criteria);
    }

    @Override
    public ReviewCriteriaResponse createCriteria(ReviewCriteriaRequest request) {
        ReviewCriteria criteria = new ReviewCriteria();
        mapToEntity(request, criteria);
        return mapToResponse(reviewCriteriaRepository.save(criteria));
    }

    @Override
    public ReviewCriteriaResponse updateCriteria(Long id, ReviewCriteriaRequest request) {
        ReviewCriteria existingCriteria = reviewCriteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review criteria not found with id: " + id));
        
        mapToEntity(request, existingCriteria);
        return mapToResponse(reviewCriteriaRepository.save(existingCriteria));
    }

    @Override
    public void deleteCriteria(Long id) {
        ReviewCriteria criteria = reviewCriteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review criteria not found with id: " + id));
        criteria.setIsActive(false);
        reviewCriteriaRepository.save(criteria);
    }

    @Override
    public void initDefaultCriteria() {
        if (reviewCriteriaRepository.count() == 0) {
            // Create default criteria as specified in requirements
            createDefaultCriterion("Food Taste", "Quality and flavor of the food", BigDecimal.valueOf(0.25), 1);
            createDefaultCriterion("Food Presentation", "Visual appeal and arrangement of the dishes", BigDecimal.valueOf(0.15), 2);
            createDefaultCriterion("Professionalism", "Chef's professional conduct and expertise", BigDecimal.valueOf(0.20), 3);
            createDefaultCriterion("Cleanliness & Hygiene", "Cleanliness of workspace and personal hygiene", BigDecimal.valueOf(0.20), 4);
            createDefaultCriterion("Communication & Attitude", "Quality of communication and overall attitude", BigDecimal.valueOf(0.10), 5);
            createDefaultCriterion("Punctuality", "Timeliness and adherence to schedule", BigDecimal.valueOf(0.10), 6);
        }
    }
    
    private void createDefaultCriterion(String name, String description, BigDecimal weight, int displayOrder) {
        Optional<ReviewCriteria> existingCriteria = reviewCriteriaRepository.findByName(name);
        
        if (existingCriteria.isEmpty()) {
            ReviewCriteria criteria = new ReviewCriteria();
            criteria.setName(name);
            criteria.setDescription(description);
            criteria.setWeight(weight);
            criteria.setIsActive(true);
            criteria.setDisplayOrder(displayOrder);
            
            reviewCriteriaRepository.save(criteria);
        }
    }
    
    private ReviewCriteriaResponse mapToResponse(ReviewCriteria criteria) {
        return new ReviewCriteriaResponse(
                criteria.getCriteriaId(),
                criteria.getName(),
                criteria.getDescription(),
                criteria.getWeight(),
                criteria.getIsActive(),
                criteria.getDisplayOrder()
        );
    }
    
    private void mapToEntity(ReviewCriteriaRequest request, ReviewCriteria criteria) {
        criteria.setName(request.getName());
        criteria.setDescription(request.getDescription());
        criteria.setWeight(request.getWeight());
        criteria.setIsActive(request.getIsActive());
        criteria.setDisplayOrder(request.getDisplayOrder());
    }
} 