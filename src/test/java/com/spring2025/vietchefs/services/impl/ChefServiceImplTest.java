package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefsResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChefServiceImplTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private DistanceService distanceService;

    @Mock
    private CalculateService calculateService;

    @InjectMocks
    private ChefServiceImpl chefService;

    private List<Chef> chefsList;
    private List<ChefResponseDto> chefResponseDtoList;
    private Page<Chef> chefsPage;

    @BeforeEach
    public void setup() {
        // Set up test chefs
        chefsList = new ArrayList<>();
        chefResponseDtoList = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            User user = new User();
            user.setId((long) i);
            user.setFullName("Test User " + i);
            user.setEmail("user" + i + "@example.com");

            Chef chef = new Chef();
            chef.setId((long) i);
            chef.setUser(user);
            chef.setBio("Chef Bio " + i);
            chef.setDescription("Chef Description " + i);
            chef.setAddress("Address " + i);
            chef.setCountry("Country " + i);
            chef.setPrice(BigDecimal.valueOf(20 + i * 5));
            chef.setMaxServingSize(10 + i);
            chef.setSpecialization("Vietnamese Cuisine");
            chef.setYearsOfExperience(i + 5);
            chef.setCertification("Certification " + i);
            chef.setStatus("ACTIVE");
            chef.setIsDeleted(false);
            chef.setLatitude(10.0 + i);
            chef.setLongitude(106.0 + i);

            ChefResponseDto dto = new ChefResponseDto();
            dto.setId((long) i);
            dto.setBio("Chef Bio " + i);
            dto.setDescription("Chef Description " + i);
            dto.setAddress("Address " + i);
            dto.setCountry("Country " + i);
            dto.setPrice(BigDecimal.valueOf(20 + i * 5));
            dto.setMaxServingSize(10 + i);
            dto.setSpecialization("Vietnamese Cuisine");
            dto.setYearsOfExperience(i + 5);
            dto.setCertification("Certification " + i);
            dto.setStatus("ACTIVE");
            dto.setAverageRating(BigDecimal.valueOf(i + 2.5)); // Average rating for each chef

            chefsList.add(chef);
            chefResponseDtoList.add(dto);
        }

        // Create a page of chefs
        chefsPage = new PageImpl<>(chefsList, PageRequest.of(0, 10), chefsList.size());
    }

    @Test
    public void getAllChefs_ShouldReturnChefsWithAverageRating() {
        // Arrange
        when(chefRepository.findByStatusAndIsDeletedFalse(eq("ACTIVE"), any(Pageable.class))).thenReturn(chefsPage);
        
        // Setup modelMapper to convert Chef to ChefResponseDto
        for (int i = 0; i < chefsList.size(); i++) {
            when(modelMapper.map(chefsList.get(i), ChefResponseDto.class)).thenReturn(chefResponseDtoList.get(i));
            
            // Setup reviewService to return average ratings
            when(reviewService.getAverageRatingForChef(chefsList.get(i).getId())).thenReturn(chefResponseDtoList.get(i).getAverageRating());
        }

        // Act
        ChefsResponse result = chefService.getAllChefs(0, 10, "id", "asc");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(0, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(3, result.getTotalElements());
        
        // Verify average ratings are included in response
        for (int i = 0; i < result.getContent().size(); i++) {
            ChefResponseDto dto = result.getContent().get(i);
            assertEquals(BigDecimal.valueOf(i + 1 + 2.5), dto.getAverageRating());
        }
        
        // Verify service calls
        verify(chefRepository).findByStatusAndIsDeletedFalse(eq("ACTIVE"), any(Pageable.class));
        verify(reviewService, times(3)).getAverageRatingForChef(anyLong());
    }

    @Test
    public void getAllChefs_WithRatingsSorting_ShouldOrderByAverageRating() {
        // Arrange
        when(chefRepository.findByStatusAndIsDeletedFalse(eq("ACTIVE"), any(Pageable.class))).thenReturn(chefsPage);
        
        // Setup modelMapper to convert Chef to ChefResponseDto
        for (int i = 0; i < chefsList.size(); i++) {
            when(modelMapper.map(chefsList.get(i), ChefResponseDto.class)).thenReturn(chefResponseDtoList.get(i));
            
            // Setup reviewService to return average ratings
            when(reviewService.getAverageRatingForChef(chefsList.get(i).getId())).thenReturn(chefResponseDtoList.get(i).getAverageRating());
        }

        // Act - sort by rating in descending order
        ChefsResponse result = chefService.getAllChefs(0, 10, "rating", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        
        // Verify the results are sorted by rating in descending order
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            ChefResponseDto current = result.getContent().get(i);
            ChefResponseDto next = result.getContent().get(i + 1);
            assertTrue(current.getAverageRating().compareTo(next.getAverageRating()) >= 0);
        }
    }
    
    @Test
    public void getAllChefs_WithRatingDescConstant_ShouldOrderByRatingDesc() {
        // Arrange
        when(chefRepository.findByStatusAndIsDeletedFalse(eq("ACTIVE"), any(Pageable.class))).thenReturn(chefsPage);
        
        // Setup modelMapper to convert Chef to ChefResponseDto
        for (int i = 0; i < chefsList.size(); i++) {
            when(modelMapper.map(chefsList.get(i), ChefResponseDto.class)).thenReturn(chefResponseDtoList.get(i));
            
            // Setup reviewService to return average ratings
            when(reviewService.getAverageRatingForChef(chefsList.get(i).getId())).thenReturn(chefResponseDtoList.get(i).getAverageRating());
        }

        // Act - use the rating_desc constant
        ChefsResponse result = chefService.getAllChefs(0, 10, "id", AppConstants.DEFAULT_SORT_RATING_DESC);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        
        // Verify the results are sorted by rating in descending order
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            ChefResponseDto current = result.getContent().get(i);
            ChefResponseDto next = result.getContent().get(i + 1);
            assertTrue(current.getAverageRating().compareTo(next.getAverageRating()) >= 0);
        }
    }

    
} 