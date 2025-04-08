package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.ReviewCriteria;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.repositories.ReviewCriteriaRepository;
import com.spring2025.vietchefs.services.ReviewCriteriaService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    public List<ReviewCriteria> getAllCriteria() {
        return reviewCriteriaRepository.findAll();
    }

    @Override
    public List<ReviewCriteria> getActiveCriteria() {
        return reviewCriteriaRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public ReviewCriteria getCriteriaById(Long id) {
        return reviewCriteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review criteria not found with id: " + id));
    }

    @Override
    public ReviewCriteria createCriteria(ReviewCriteria criteria) {
        return reviewCriteriaRepository.save(criteria);
    }

    @Override
    public ReviewCriteria updateCriteria(Long id, ReviewCriteria criteria) {
        ReviewCriteria existingCriteria = getCriteriaById(id);
        
        existingCriteria.setName(criteria.getName());
        existingCriteria.setDescription(criteria.getDescription());
        existingCriteria.setWeight(criteria.getWeight());
        existingCriteria.setIsActive(criteria.getIsActive());
        existingCriteria.setDisplayOrder(criteria.getDisplayOrder());
        
        return reviewCriteriaRepository.save(existingCriteria);
    }

    @Override
    public void deleteCriteria(Long id) {
        ReviewCriteria criteria = getCriteriaById(id);
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
} 