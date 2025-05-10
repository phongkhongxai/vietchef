package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.ReviewCriteria;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewCriteriaRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewCriteriaResponse;
import com.spring2025.vietchefs.repositories.ReviewCriteriaRepository;
import com.spring2025.vietchefs.services.impl.ReviewCriteriaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.AdditionalMatchers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.not;

@ExtendWith(MockitoExtension.class)
public class ReviewCriteriaServiceTest {

    @Mock
    private ReviewCriteriaRepository reviewCriteriaRepository;

    @InjectMocks
    private ReviewCriteriaServiceImpl reviewCriteriaService;

    @Captor
    private ArgumentCaptor<ReviewCriteria> criteriaCaptor;

    private ReviewCriteria testCriteria1;
    private ReviewCriteria testCriteria2;
    private ReviewCriteriaRequest testCriteriaRequest;
    private List<ReviewCriteria> criteriaList;

    @BeforeEach
    void setUp() {
        // Set up test criteria 1
        testCriteria1 = new ReviewCriteria();
        testCriteria1.setCriteriaId(1L);
        testCriteria1.setName("Food Taste");
        testCriteria1.setDescription("Quality and flavor of the food");
        testCriteria1.setWeight(BigDecimal.valueOf(0.25));
        testCriteria1.setIsActive(true);
        testCriteria1.setDisplayOrder(1);

        // Set up test criteria 2
        testCriteria2 = new ReviewCriteria();
        testCriteria2.setCriteriaId(2L);
        testCriteria2.setName("Professionalism");
        testCriteria2.setDescription("Chef's professional conduct and expertise");
        testCriteria2.setWeight(BigDecimal.valueOf(0.20));
        testCriteria2.setIsActive(false);
        testCriteria2.setDisplayOrder(3);

        // Set up list of criteria
        criteriaList = new ArrayList<>();
        criteriaList.add(testCriteria1);
        criteriaList.add(testCriteria2);

        // Set up test criteria request
        testCriteriaRequest = new ReviewCriteriaRequest();
        testCriteriaRequest.setName("Cleanliness");
        testCriteriaRequest.setDescription("Cleanliness of workspace");
        testCriteriaRequest.setWeight(BigDecimal.valueOf(0.15));
        testCriteriaRequest.setIsActive(true);
        testCriteriaRequest.setDisplayOrder(4);
    }

    // ==================== getAllCriteria Tests ====================

    @Test
    @DisplayName("Test 1: getAllCriteria should return all criteria")
    void getAllCriteria_ShouldReturnAllCriteria() {
        // Arrange
        when(reviewCriteriaRepository.findAll()).thenReturn(criteriaList);

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getAllCriteria();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Food Taste", result.get(0).getName());
        assertEquals("Professionalism", result.get(1).getName());
        verify(reviewCriteriaRepository).findAll();
    }

    @Test
    @DisplayName("Test 2: getAllCriteria with empty database should return empty list")
    void getAllCriteria_WithEmptyDatabase_ShouldReturnEmptyList() {
        // Arrange
        when(reviewCriteriaRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getAllCriteria();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(reviewCriteriaRepository).findAll();
    }

    @Test
    @DisplayName("Test 3: getAllCriteria should correctly map entity fields to response")
    void getAllCriteria_ShouldCorrectlyMapEntityFieldsToResponse() {
        // Arrange
        when(reviewCriteriaRepository.findAll()).thenReturn(List.of(testCriteria1));

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getAllCriteria();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        ReviewCriteriaResponse response = result.get(0);
        assertEquals(testCriteria1.getCriteriaId(), response.getCriteriaId());
        assertEquals(testCriteria1.getName(), response.getName());
        assertEquals(testCriteria1.getDescription(), response.getDescription());
        assertEquals(testCriteria1.getWeight(), response.getWeight());
        assertEquals(testCriteria1.getIsActive(), response.getIsActive());
        assertEquals(testCriteria1.getDisplayOrder(), response.getDisplayOrder());
        verify(reviewCriteriaRepository).findAll();
    }

    @Test
    @DisplayName("Test 4: getAllCriteria with multiple records should maintain order")
    void getAllCriteria_WithMultipleRecords_ShouldMaintainOrder() {
        // Arrange
        List<ReviewCriteria> orderedList = new ArrayList<>();
        
        ReviewCriteria criteria1 = new ReviewCriteria();
        criteria1.setCriteriaId(1L);
        criteria1.setName("Food Taste");
        criteria1.setDisplayOrder(1);
        
        ReviewCriteria criteria2 = new ReviewCriteria();
        criteria2.setCriteriaId(2L);
        criteria2.setName("Food Presentation");
        criteria2.setDisplayOrder(2);
        
        ReviewCriteria criteria3 = new ReviewCriteria();
        criteria3.setCriteriaId(3L);
        criteria3.setName("Professionalism");
        criteria3.setDisplayOrder(3);
        
        orderedList.add(criteria1);
        orderedList.add(criteria2);
        orderedList.add(criteria3);
        
        when(reviewCriteriaRepository.findAll()).thenReturn(orderedList);

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getAllCriteria();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Food Taste", result.get(0).getName());
        assertEquals("Food Presentation", result.get(1).getName());
        assertEquals("Professionalism", result.get(2).getName());
        verify(reviewCriteriaRepository).findAll();
    }

    // ==================== getActiveCriteria Tests ====================

    @Test
    @DisplayName("Test 1: getActiveCriteria should return only active criteria")
    void getActiveCriteria_ShouldReturnOnlyActiveCriteria() {
        // Arrange
        List<ReviewCriteria> activeCriteriaList = List.of(testCriteria1);
        when(reviewCriteriaRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(activeCriteriaList);

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getActiveCriteria();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Food Taste", result.get(0).getName());
        assertTrue(result.get(0).getIsActive());
        verify(reviewCriteriaRepository).findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("Test 2: getActiveCriteria with no active criteria should return empty list")
    void getActiveCriteria_WithNoActiveCriteria_ShouldReturnEmptyList() {
        // Arrange
        when(reviewCriteriaRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(new ArrayList<>());

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getActiveCriteria();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(reviewCriteriaRepository).findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("Test 3: getActiveCriteria should return criteria ordered by display order")
    void getActiveCriteria_ShouldReturnCriteriaOrderedByDisplayOrder() {
        // Arrange
        List<ReviewCriteria> orderedActiveCriteriaList = new ArrayList<>();
        
        ReviewCriteria criteria1 = new ReviewCriteria();
        criteria1.setCriteriaId(1L);
        criteria1.setName("Food Taste");
        criteria1.setIsActive(true);
        criteria1.setDisplayOrder(1);
        
        ReviewCriteria criteria2 = new ReviewCriteria();
        criteria2.setCriteriaId(2L);
        criteria2.setName("Food Presentation");
        criteria2.setIsActive(true);
        criteria2.setDisplayOrder(2);
        
        orderedActiveCriteriaList.add(criteria1);
        orderedActiveCriteriaList.add(criteria2);
        
        when(reviewCriteriaRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(orderedActiveCriteriaList);

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getActiveCriteria();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Food Taste", result.get(0).getName());
        assertEquals(1, result.get(0).getDisplayOrder());
        assertEquals("Food Presentation", result.get(1).getName());
        assertEquals(2, result.get(1).getDisplayOrder());
        verify(reviewCriteriaRepository).findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("Test 4: getActiveCriteria should correctly map entity fields to response")
    void getActiveCriteria_ShouldCorrectlyMapEntityFieldsToResponse() {
        // Arrange
        when(reviewCriteriaRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(testCriteria1));

        // Act
        List<ReviewCriteriaResponse> result = reviewCriteriaService.getActiveCriteria();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        ReviewCriteriaResponse response = result.get(0);
        assertEquals(testCriteria1.getCriteriaId(), response.getCriteriaId());
        assertEquals(testCriteria1.getName(), response.getName());
        assertEquals(testCriteria1.getDescription(), response.getDescription());
        assertEquals(testCriteria1.getWeight(), response.getWeight());
        assertEquals(testCriteria1.getIsActive(), response.getIsActive());
        assertEquals(testCriteria1.getDisplayOrder(), response.getDisplayOrder());
        verify(reviewCriteriaRepository).findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    // ==================== getCriteriaById Tests ====================

    @Test
    @DisplayName("Test 1: getCriteriaById with valid id should return criteria")
    void getCriteriaById_WithValidId_ShouldReturnCriteria() {
        // Arrange
        when(reviewCriteriaRepository.findById(1L)).thenReturn(Optional.of(testCriteria1));

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.getCriteriaById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testCriteria1.getCriteriaId(), result.getCriteriaId());
        assertEquals(testCriteria1.getName(), result.getName());
        verify(reviewCriteriaRepository).findById(1L);
    }

    @Test
    @DisplayName("Test 2: getCriteriaById with non-existent id should throw exception")
    void getCriteriaById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(reviewCriteriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewCriteriaService.getCriteriaById(99L);
        });

        assertTrue(exception.getMessage().contains("Review criteria not found"));
        verify(reviewCriteriaRepository).findById(99L);
    }

    @Test
    @DisplayName("Test 3: getCriteriaById should retrieve inactive criteria")
    void getCriteriaById_ShouldRetrieveInactiveCriteria() {
        // Arrange
        when(reviewCriteriaRepository.findById(2L)).thenReturn(Optional.of(testCriteria2));

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.getCriteriaById(2L);

        // Assert
        assertNotNull(result);
        assertEquals(testCriteria2.getCriteriaId(), result.getCriteriaId());
        assertEquals(testCriteria2.getName(), result.getName());
        assertFalse(result.getIsActive());
        verify(reviewCriteriaRepository).findById(2L);
    }

    @Test
    @DisplayName("Test 4: getCriteriaById should correctly map entity fields to response")
    void getCriteriaById_ShouldCorrectlyMapEntityFieldsToResponse() {
        // Arrange
        when(reviewCriteriaRepository.findById(1L)).thenReturn(Optional.of(testCriteria1));

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.getCriteriaById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testCriteria1.getCriteriaId(), result.getCriteriaId());
        assertEquals(testCriteria1.getName(), result.getName());
        assertEquals(testCriteria1.getDescription(), result.getDescription());
        assertEquals(testCriteria1.getWeight(), result.getWeight());
        assertEquals(testCriteria1.getIsActive(), result.getIsActive());
        assertEquals(testCriteria1.getDisplayOrder(), result.getDisplayOrder());
        verify(reviewCriteriaRepository).findById(1L);
    }

    // ==================== createCriteria Tests ====================

    @Test
    @DisplayName("Test 1: createCriteria with valid request should create criteria")
    void createCriteria_WithValidRequest_ShouldCreateCriteria() {
        // Arrange
        ReviewCriteria newCriteria = new ReviewCriteria();
        newCriteria.setCriteriaId(3L);
        newCriteria.setName(testCriteriaRequest.getName());
        newCriteria.setDescription(testCriteriaRequest.getDescription());
        newCriteria.setWeight(testCriteriaRequest.getWeight());
        newCriteria.setIsActive(testCriteriaRequest.getIsActive());
        newCriteria.setDisplayOrder(testCriteriaRequest.getDisplayOrder());

        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenReturn(newCriteria);

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.createCriteria(testCriteriaRequest);

        // Assert
        assertNotNull(result);
        assertEquals(newCriteria.getCriteriaId(), result.getCriteriaId());
        assertEquals(newCriteria.getName(), result.getName());
        assertEquals(newCriteria.getDescription(), result.getDescription());
        assertEquals(newCriteria.getWeight(), result.getWeight());
        assertEquals(newCriteria.getIsActive(), result.getIsActive());
        assertEquals(newCriteria.getDisplayOrder(), result.getDisplayOrder());

        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals(testCriteriaRequest.getName(), capturedCriteria.getName());
        assertEquals(testCriteriaRequest.getDescription(), capturedCriteria.getDescription());
        assertEquals(testCriteriaRequest.getWeight(), capturedCriteria.getWeight());
        assertEquals(testCriteriaRequest.getIsActive(), capturedCriteria.getIsActive());
        assertEquals(testCriteriaRequest.getDisplayOrder(), capturedCriteria.getDisplayOrder());
    }

    @Test
    @DisplayName("Test 2: createCriteria should set all properties correctly")
    void createCriteria_ShouldSetAllPropertiesCorrectly() {
        // Arrange
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> {
            ReviewCriteria saved = invocation.getArgument(0);
            saved.setCriteriaId(3L); // Simulate DB generating ID
            return saved;
        });

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.createCriteria(testCriteriaRequest);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getCriteriaId());
        assertEquals("Cleanliness", result.getName());
        assertEquals("Cleanliness of workspace", result.getDescription());
        assertEquals(BigDecimal.valueOf(0.15), result.getWeight());
        assertTrue(result.getIsActive());
        assertEquals(4, result.getDisplayOrder());

        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals("Cleanliness", capturedCriteria.getName());
        assertEquals("Cleanliness of workspace", capturedCriteria.getDescription());
        assertEquals(BigDecimal.valueOf(0.15), capturedCriteria.getWeight());
        assertTrue(capturedCriteria.getIsActive());
        assertEquals(4, capturedCriteria.getDisplayOrder());
    }

    @Test
    @DisplayName("Test 3: createCriteria with null values should use defaults when appropriate")
    void createCriteria_WithNullValues_ShouldUseDefaultsWhenAppropriate() {
        // Arrange
        ReviewCriteriaRequest requestWithNulls = new ReviewCriteriaRequest();
        requestWithNulls.setName("Test Criteria");
        requestWithNulls.setDescription(null);
        requestWithNulls.setWeight(null);
        requestWithNulls.setIsActive(null);
        requestWithNulls.setDisplayOrder(5);

        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> {
            ReviewCriteria saved = invocation.getArgument(0);
            saved.setCriteriaId(4L);
            return saved;
        });

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.createCriteria(requestWithNulls);

        // Assert
        assertNotNull(result);
        assertEquals("Test Criteria", result.getName());
        assertNull(result.getDescription());
        assertNull(result.getWeight());
        assertNull(result.getIsActive());
        assertEquals(5, result.getDisplayOrder());

        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals("Test Criteria", capturedCriteria.getName());
        assertNull(capturedCriteria.getDescription());
        assertNull(capturedCriteria.getWeight());
        assertNull(capturedCriteria.getIsActive());
        assertEquals(5, capturedCriteria.getDisplayOrder());
    }

    @Test
    @DisplayName("Test 4: createCriteria with detailed properties should save them correctly")
    void createCriteria_WithDetailedProperties_ShouldSaveThemCorrectly() {
        // Arrange
        ReviewCriteriaRequest detailedRequest = new ReviewCriteriaRequest();
        detailedRequest.setName("Detailed Criteria");
        detailedRequest.setDescription("A very detailed description of this criteria that explains what it's all about");
        detailedRequest.setWeight(BigDecimal.valueOf(0.333));
        detailedRequest.setIsActive(true);
        detailedRequest.setDisplayOrder(10);

        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> {
            ReviewCriteria saved = invocation.getArgument(0);
            saved.setCriteriaId(5L);
            return saved;
        });

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.createCriteria(detailedRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Detailed Criteria", result.getName());
        assertEquals("A very detailed description of this criteria that explains what it's all about", result.getDescription());
        assertEquals(BigDecimal.valueOf(0.333), result.getWeight());
        assertTrue(result.getIsActive());
        assertEquals(10, result.getDisplayOrder());

        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals("Detailed Criteria", capturedCriteria.getName());
        assertEquals("A very detailed description of this criteria that explains what it's all about", capturedCriteria.getDescription());
        assertEquals(BigDecimal.valueOf(0.333), capturedCriteria.getWeight());
        assertTrue(capturedCriteria.getIsActive());
        assertEquals(10, capturedCriteria.getDisplayOrder());
    }

    // ==================== updateCriteria Tests ====================

    @Test
    @DisplayName("Test 1: updateCriteria with valid id and request should update criteria")
    void updateCriteria_WithValidIdAndRequest_ShouldUpdateCriteria() {
        // Arrange
        ReviewCriteriaRequest updateRequest = new ReviewCriteriaRequest();
        updateRequest.setName("Updated Food Taste");
        updateRequest.setDescription("Updated description");
        updateRequest.setWeight(BigDecimal.valueOf(0.30));
        updateRequest.setIsActive(true);
        updateRequest.setDisplayOrder(2);

        when(reviewCriteriaRepository.findById(1L)).thenReturn(Optional.of(testCriteria1));
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.updateCriteria(1L, updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getCriteriaId());
        assertEquals("Updated Food Taste", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(BigDecimal.valueOf(0.30), result.getWeight());
        assertTrue(result.getIsActive());
        assertEquals(2, result.getDisplayOrder());

        verify(reviewCriteriaRepository).findById(1L);
        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals("Updated Food Taste", capturedCriteria.getName());
        assertEquals("Updated description", capturedCriteria.getDescription());
        assertEquals(BigDecimal.valueOf(0.30), capturedCriteria.getWeight());
        assertTrue(capturedCriteria.getIsActive());
        assertEquals(2, capturedCriteria.getDisplayOrder());
    }

    @Test
    @DisplayName("Test 2: updateCriteria with non-existent id should throw exception")
    void updateCriteria_WithNonExistentId_ShouldThrowException() {
        // Arrange
        ReviewCriteriaRequest updateRequest = new ReviewCriteriaRequest();
        updateRequest.setName("Updated Food Taste");
        
        when(reviewCriteriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewCriteriaService.updateCriteria(99L, updateRequest);
        });

        assertTrue(exception.getMessage().contains("Review criteria not found"));
        verify(reviewCriteriaRepository).findById(99L);
        verify(reviewCriteriaRepository, never()).save(any(ReviewCriteria.class));
    }

    @Test
    @DisplayName("Test 3: updateCriteria with partial data should only update provided fields")
    void updateCriteria_WithPartialData_ShouldOnlyUpdateProvidedFields() {
        // Arrange
        ReviewCriteriaRequest partialUpdateRequest = new ReviewCriteriaRequest();
        partialUpdateRequest.setName("Updated Food Taste");
        partialUpdateRequest.setDescription(null);
        partialUpdateRequest.setWeight(null);
        partialUpdateRequest.setIsActive(null);
        partialUpdateRequest.setDisplayOrder(null);

        ReviewCriteria existingCriteria = new ReviewCriteria();
        existingCriteria.setCriteriaId(1L);
        existingCriteria.setName("Food Taste");
        existingCriteria.setDescription("Original description");
        existingCriteria.setWeight(BigDecimal.valueOf(0.25));
        existingCriteria.setIsActive(true);
        existingCriteria.setDisplayOrder(1);

        when(reviewCriteriaRepository.findById(1L)).thenReturn(Optional.of(existingCriteria));
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.updateCriteria(1L, partialUpdateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getCriteriaId());
        assertEquals("Updated Food Taste", result.getName());
        assertNull(result.getDescription());
        assertNull(result.getWeight());
        assertNull(result.getIsActive());
        assertNull(result.getDisplayOrder());

        verify(reviewCriteriaRepository).findById(1L);
        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals("Updated Food Taste", capturedCriteria.getName());
        assertNull(capturedCriteria.getDescription());
        assertNull(capturedCriteria.getWeight());
        assertNull(capturedCriteria.getIsActive());
        assertNull(capturedCriteria.getDisplayOrder());
    }

    @Test
    @DisplayName("Test 4: updateCriteria to inactive should update isActive flag")
    void updateCriteria_ToInactive_ShouldUpdateIsActiveFlag() {
        // Arrange
        ReviewCriteriaRequest deactivateRequest = new ReviewCriteriaRequest();
        deactivateRequest.setName("Food Taste");
        deactivateRequest.setDescription("Quality and flavor of the food");
        deactivateRequest.setWeight(BigDecimal.valueOf(0.25));
        deactivateRequest.setIsActive(false);
        deactivateRequest.setDisplayOrder(1);

        when(reviewCriteriaRepository.findById(1L)).thenReturn(Optional.of(testCriteria1));
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewCriteriaResponse result = reviewCriteriaService.updateCriteria(1L, deactivateRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getCriteriaId());
        assertEquals("Food Taste", result.getName());
        assertFalse(result.getIsActive());

        verify(reviewCriteriaRepository).findById(1L);
        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals("Food Taste", capturedCriteria.getName());
        assertFalse(capturedCriteria.getIsActive());
    }

    // ==================== deleteCriteria Tests ====================

    @Test
    @DisplayName("Test 1: deleteCriteria with valid id should set isActive to false")
    void deleteCriteria_WithValidId_ShouldSetIsActiveToFalse() {
        // Arrange
        when(reviewCriteriaRepository.findById(1L)).thenReturn(Optional.of(testCriteria1));
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        reviewCriteriaService.deleteCriteria(1L);

        // Assert
        verify(reviewCriteriaRepository).findById(1L);
        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertFalse(capturedCriteria.getIsActive());
    }

    @Test
    @DisplayName("Test 2: deleteCriteria with non-existent id should throw exception")
    void deleteCriteria_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(reviewCriteriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewCriteriaService.deleteCriteria(99L);
        });

        assertTrue(exception.getMessage().contains("Review criteria not found"));
        verify(reviewCriteriaRepository).findById(99L);
        verify(reviewCriteriaRepository, never()).save(any(ReviewCriteria.class));
    }

    @Test
    @DisplayName("Test 3: deleteCriteria for already inactive criteria should keep it inactive")
    void deleteCriteria_ForAlreadyInactiveCriteria_ShouldKeepItInactive() {
        // Arrange
        when(reviewCriteriaRepository.findById(2L)).thenReturn(Optional.of(testCriteria2));
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        reviewCriteriaService.deleteCriteria(2L);

        // Assert
        verify(reviewCriteriaRepository).findById(2L);
        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertFalse(capturedCriteria.getIsActive());
    }

    @Test
    @DisplayName("Test 4: deleteCriteria should not modify other properties")
    void deleteCriteria_ShouldNotModifyOtherProperties() {
        // Arrange
        ReviewCriteria criteria = new ReviewCriteria();
        criteria.setCriteriaId(3L);
        criteria.setName("Test Criteria");
        criteria.setDescription("Test Description");
        criteria.setWeight(BigDecimal.valueOf(0.5));
        criteria.setIsActive(true);
        criteria.setDisplayOrder(3);

        when(reviewCriteriaRepository.findById(3L)).thenReturn(Optional.of(criteria));
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        reviewCriteriaService.deleteCriteria(3L);

        // Assert
        verify(reviewCriteriaRepository).findById(3L);
        verify(reviewCriteriaRepository).save(criteriaCaptor.capture());
        ReviewCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals(3L, capturedCriteria.getCriteriaId());
        assertEquals("Test Criteria", capturedCriteria.getName());
        assertEquals("Test Description", capturedCriteria.getDescription());
        assertEquals(BigDecimal.valueOf(0.5), capturedCriteria.getWeight());
        assertFalse(capturedCriteria.getIsActive());
        assertEquals(3, capturedCriteria.getDisplayOrder());
    }

    // ==================== initDefaultCriteria Tests ====================

    @Test
    @DisplayName("Test 1: initDefaultCriteria with empty database should create default criteria")
    void initDefaultCriteria_WithEmptyDatabase_ShouldCreateDefaultCriteria() {
        // Arrange
        when(reviewCriteriaRepository.count()).thenReturn(0L);
        when(reviewCriteriaRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> {
            ReviewCriteria saved = invocation.getArgument(0);
            saved.setCriteriaId(1L); // Simulate DB generating ID
            return saved;
        });

        // Act
        reviewCriteriaService.initDefaultCriteria();

        // Assert
        verify(reviewCriteriaRepository).count();
        verify(reviewCriteriaRepository, times(6)).findByName(anyString());
        verify(reviewCriteriaRepository, times(6)).save(any(ReviewCriteria.class));
    }

    @Test
    @DisplayName("Test 2: initDefaultCriteria with existing criteria should not create new ones")
    void initDefaultCriteria_WithExistingCriteria_ShouldNotCreateNewOnes() {
        // Arrange
        when(reviewCriteriaRepository.count()).thenReturn(6L);

        // Act
        reviewCriteriaService.initDefaultCriteria();

        // Assert
        verify(reviewCriteriaRepository).count();
        verify(reviewCriteriaRepository, never()).findByName(anyString());
        verify(reviewCriteriaRepository, never()).save(any(ReviewCriteria.class));
    }

    @Test
    @DisplayName("Test 3: initDefaultCriteria should create six default criteria with correct values")
    void initDefaultCriteria_ShouldCreateSixDefaultCriteriaWithCorrectValues() {
        // Arrange
        when(reviewCriteriaRepository.count()).thenReturn(0L);
        when(reviewCriteriaRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        reviewCriteriaService.initDefaultCriteria();

        // Assert
        verify(reviewCriteriaRepository).count();
        
        // Capture all criteria saves
        ArgumentCaptor<ReviewCriteria> criteriaCaptor = ArgumentCaptor.forClass(ReviewCriteria.class);
        verify(reviewCriteriaRepository, times(6)).save(criteriaCaptor.capture());
        
        List<ReviewCriteria> capturedCriteria = criteriaCaptor.getAllValues();
        assertEquals(6, capturedCriteria.size());
        
        // Verify "Food Taste" criteria
        ReviewCriteria foodTaste = capturedCriteria.stream()
                .filter(c -> "Food Taste".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(foodTaste);
        assertEquals(BigDecimal.valueOf(0.25), foodTaste.getWeight());
        assertTrue(foodTaste.getIsActive());
        assertEquals(1, foodTaste.getDisplayOrder());
        
        // Verify "Professionalism" criteria
        ReviewCriteria professionalism = capturedCriteria.stream()
                .filter(c -> "Professionalism".equals(c.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(professionalism);
        assertEquals(BigDecimal.valueOf(0.20), professionalism.getWeight());
        assertTrue(professionalism.getIsActive());
        assertEquals(3, professionalism.getDisplayOrder());
    }

    @Test
    @DisplayName("Test 4: initDefaultCriteria should not duplicate existing criteria")
    void initDefaultCriteria_ShouldNotDuplicateExistingCriteria() {
        // Arrange
        when(reviewCriteriaRepository.count()).thenReturn(0L);
        
        // Simulate "Food Taste" already exists
        ReviewCriteria existingCriteria = new ReviewCriteria();
        existingCriteria.setCriteriaId(1L);
        existingCriteria.setName("Food Taste");
        existingCriteria.setDescription("Existing description");
        existingCriteria.setWeight(BigDecimal.valueOf(0.5)); // Different weight
        existingCriteria.setIsActive(true);
        existingCriteria.setDisplayOrder(10); // Different order
        
        when(reviewCriteriaRepository.findByName("Food Taste")).thenReturn(Optional.of(existingCriteria));
        when(reviewCriteriaRepository.findByName(not(eq("Food Taste")))).thenReturn(Optional.empty());
        when(reviewCriteriaRepository.save(any(ReviewCriteria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        reviewCriteriaService.initDefaultCriteria();

        // Assert
        verify(reviewCriteriaRepository).count();
        verify(reviewCriteriaRepository, times(6)).findByName(anyString());
        
        // Should only save 5 criteria (not "Food Taste")
        verify(reviewCriteriaRepository, times(5)).save(any(ReviewCriteria.class));
        
        // Verify "Food Taste" was checked but not saved
        verify(reviewCriteriaRepository).findByName("Food Taste");
        
        // Capture all criteria saves to verify "Food Taste" is not among them
        ArgumentCaptor<ReviewCriteria> criteriaCaptor = ArgumentCaptor.forClass(ReviewCriteria.class);
        verify(reviewCriteriaRepository, times(5)).save(criteriaCaptor.capture());
        
        List<ReviewCriteria> capturedCriteria = criteriaCaptor.getAllValues();
        boolean containsFoodTaste = capturedCriteria.stream()
                .anyMatch(c -> "Food Taste".equals(c.getName()));
        assertFalse(containsFoodTaste);
    }
} 