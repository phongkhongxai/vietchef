package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.ReviewCriteria;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCriteriaRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.repositories.ReviewCriteriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewCriteriaServiceImplTest {

    @Mock
    private ReviewCriteriaRepository reviewCriteriaRepository;

    @InjectMocks
    private ReviewCriteriaServiceImpl reviewCriteriaService;

    private ReviewCriteria tasteCriteria;
    private ReviewCriteria presentationCriteria;
    private ReviewCriteriaRequest criteriaRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        tasteCriteria = new ReviewCriteria();
        tasteCriteria.setCriteriaId(1L);
        tasteCriteria.setName("Food Taste");
        tasteCriteria.setDescription("Quality of the food taste");
        tasteCriteria.setWeight(new BigDecimal("0.3"));
        tasteCriteria.setIsActive(true);
        tasteCriteria.setDisplayOrder(1);

        presentationCriteria = new ReviewCriteria();
        presentationCriteria.setCriteriaId(2L);
        presentationCriteria.setName("Food Presentation");
        presentationCriteria.setDescription("Visual appeal of the food");
        presentationCriteria.setWeight(new BigDecimal("0.2"));
        presentationCriteria.setIsActive(true);
        presentationCriteria.setDisplayOrder(2);

        criteriaRequest = new ReviewCriteriaRequest();
        criteriaRequest.setName("New Criteria");
        criteriaRequest.setDescription("Description of new criteria");
        criteriaRequest.setWeight(new BigDecimal("0.25"));
        criteriaRequest.setIsActive(true);
        criteriaRequest.setDisplayOrder(3);
    }

    @Test
    void getAllCriteria_ShouldReturnListOfCriteria() {
        // Arrange
        List<ReviewCriteria> criteriaList = Arrays.asList(tasteCriteria, presentationCriteria);
        when(reviewCriteriaRepository.findAll()).thenReturn(criteriaList);

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getAllCriteria();

        // Assert
        assertEquals(2, result.size());
        assertEquals(tasteCriteria.getName(), result.get(0).getName());
        assertEquals(presentationCriteria.getName(), result.get(1).getName());
    }

    @Test
    void getActiveCriteria_ShouldReturnOnlyActiveCriteria() {
        // Arrange
        List<ReviewCriteria> activeCriteriaList = Arrays.asList(tasteCriteria, presentationCriteria);
        when(reviewCriteriaRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(activeCriteriaList);

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getActiveCriteria();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsActive());
        assertTrue(result.get(1).getIsActive());
    }

    @Test
    void getCriteriaById_ShouldReturnCriteria_WhenExists() {
        // Arrange
        when(reviewCriteriaRepository.findById(tasteCriteria.getCriteriaId())).thenReturn(Optional.of(tasteCriteria));

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.getCriteriaById(tasteCriteria.getCriteriaId());

        // Assert
        assertNotNull(result);
        assertEquals(tasteCriteria.getCriteriaId(), result.getCriteriaId());
        assertEquals(tasteCriteria.getName(), result.getName());
    }

    @Test
    void getCriteriaById_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(reviewCriteriaRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewCriteriaService.getCriteriaById(999L);
        });
    }

    @Test
    void createCriteria_ShouldReturnCreatedCriteria() {
        // Arrange
        ReviewCriteria newCriteria = new ReviewCriteria();
        newCriteria.setCriteriaId(3L);
        newCriteria.setName(criteriaRequest.getName());
        newCriteria.setDescription(criteriaRequest.getDescription());
        newCriteria.setWeight(criteriaRequest.getWeight());
        newCriteria.setIsActive(criteriaRequest.getIsActive());
        newCriteria.setDisplayOrder(criteriaRequest.getDisplayOrder());

        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenReturn(newCriteria);

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.createCriteria(criteriaRequest);

        // Assert
        assertNotNull(result);
        assertEquals(newCriteria.getCriteriaId(), result.getCriteriaId());
        assertEquals(newCriteria.getName(), result.getName());
        assertEquals(newCriteria.getDescription(), result.getDescription());
        verify(reviewCriteriaRepository).save(any(ReviewCriteria.class));
    }

    @Test
    void updateCriteria_ShouldReturnUpdatedCriteria() {
        // Arrange
        when(reviewCriteriaRepository.findById(tasteCriteria.getCriteriaId())).thenReturn(Optional.of(tasteCriteria));
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenReturn(tasteCriteria);

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.updateCriteria(tasteCriteria.getCriteriaId(), criteriaRequest);

        // Assert
        assertNotNull(result);
        assertEquals(tasteCriteria.getCriteriaId(), result.getCriteriaId());
        assertEquals(criteriaRequest.getName(), tasteCriteria.getName());
        assertEquals(criteriaRequest.getDescription(), tasteCriteria.getDescription());
        verify(reviewCriteriaRepository).save(tasteCriteria);
    }

    @Test
    void deleteCriteria_ShouldDeactivateCriteria() {
        // Arrange
        when(reviewCriteriaRepository.findById(tasteCriteria.getCriteriaId())).thenReturn(Optional.of(tasteCriteria));

        // Act
        reviewCriteriaService.deleteCriteria(tasteCriteria.getCriteriaId());

        // Assert
        assertFalse(tasteCriteria.getIsActive());
        verify(reviewCriteriaRepository).save(tasteCriteria);
    }

    @Test
    void initDefaultCriteria_ShouldCreateDefaultCriteria_WhenRepositoryIsEmpty() {
        // Arrange
        when(reviewCriteriaRepository.count()).thenReturn(0L);
        when(reviewCriteriaRepository.findByName(anyString())).thenReturn(Optional.empty());

        // Act
        reviewCriteriaService.initDefaultCriteria();

        // Assert
        // Should save 6 default criteria
        verify(reviewCriteriaRepository, times(6)).save(any(ReviewCriteria.class));
    }

    @Test
    void initDefaultCriteria_ShouldNotCreateCriteria_WhenRepositoryIsNotEmpty() {
        // Arrange
        when(reviewCriteriaRepository.count()).thenReturn(6L);

        // Act
        reviewCriteriaService.initDefaultCriteria();

        // Assert
        // Should not save any criteria
        verify(reviewCriteriaRepository, never()).save(any(ReviewCriteria.class));
    }
} 