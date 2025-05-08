package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.FavoriteChef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.FavoriteChefDto;
import com.spring2025.vietchefs.models.payload.responseModel.FavoriteChefsResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.FavoriteChefRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.impl.FavoriteChefServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteChefServiceTest {

    @Mock
    private FavoriteChefRepository favoriteChefRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChefRepository chefRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private FavoriteChefServiceImpl favoriteChefService;
    
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private User testUser;
    private Chef testChef;
    private FavoriteChef testFavoriteChef;
    private FavoriteChefDto testFavoriteChefDto;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setAvatarUrl("avatar.jpg");

        // Set up test chef
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setStatus("ACTIVE");
        testChef.setSpecialization("Italian");
        testChef.setAddress("123 Chef St");
        
        User chefUser = new User();
        chefUser.setId(2L);
        chefUser.setUsername("testchef");
        chefUser.setFullName("Test Chef");
        chefUser.setAvatarUrl("chef_avatar.jpg");
        testChef.setUser(chefUser);

        // Set up test favorite chef
        testFavoriteChef = new FavoriteChef();
        testFavoriteChef.setId(1L);
        testFavoriteChef.setUser(testUser);
        testFavoriteChef.setChef(testChef);
        testFavoriteChef.setIsDeleted(false);
        testFavoriteChef.setCreatedAt(LocalDateTime.now());

        // Set up test favorite chef DTO
        testFavoriteChefDto = new FavoriteChefDto();
        testFavoriteChefDto.setId(1L);
        testFavoriteChefDto.setUserId(testUser.getId());
        testFavoriteChefDto.setChefId(testChef.getId());
        testFavoriteChefDto.setChefName("Test Chef");
        testFavoriteChefDto.setChefAvatar("chef_avatar.jpg");
        testFavoriteChefDto.setChefSpecialization("Italian");
        testFavoriteChefDto.setChefAddress("123 Chef St");
        testFavoriteChefDto.setCreatedAt(testFavoriteChef.getCreatedAt());
    }

    // ==================== addFavoriteChef Tests ====================

    @Test
    @DisplayName("Test 1: addFavoriteChef with valid data should add chef to favorites")
    void addFavoriteChef_WithValidData_ShouldAddChefToFavorites() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.findByUserAndChef(testUser, testChef)).thenReturn(Optional.empty());
        when(favoriteChefRepository.save(any(FavoriteChef.class))).thenReturn(testFavoriteChef);

        // Act
        FavoriteChefDto result = favoriteChefService.addFavoriteChef(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(testFavoriteChef.getId(), result.getId());
        assertEquals(testFavoriteChef.getUser().getId(), result.getUserId());
        assertEquals(testFavoriteChef.getChef().getId(), result.getChefId());

        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).findByUserAndChef(testUser, testChef);
        verify(favoriteChefRepository).save(any(FavoriteChef.class));
    }

    @Test
    @DisplayName("Test 2: addFavoriteChef with non-existent user should throw exception")
    void addFavoriteChef_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(99L)).thenReturn(null);

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.addFavoriteChef(99L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("User not found"));
        
        verify(userRepository).findExistUserById(99L);
        verifyNoInteractions(chefRepository);
        verifyNoInteractions(favoriteChefRepository);
    }

    @Test
    @DisplayName("Test 3: addFavoriteChef with non-existent chef should throw exception")
    void addFavoriteChef_WithNonExistentChef_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.addFavoriteChef(1L, 99L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Chef not found"));
        
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(99L);
        verifyNoInteractions(favoriteChefRepository);
    }

    @Test
    @DisplayName("Test 4: addFavoriteChef with inactive chef should throw exception")
    void addFavoriteChef_WithInactiveChef_ShouldThrowException() {
        // Arrange
        testChef.setStatus("INACTIVE");
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.addFavoriteChef(1L, 1L);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("not active"));
        
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verifyNoInteractions(favoriteChefRepository);
    }

    // ==================== removeFavoriteChef Tests ====================

    @Test
    @DisplayName("Test 1: removeFavoriteChef with valid data should remove chef from favorites")
    void removeFavoriteChef_WithValidData_ShouldRemoveChefFromFavorites() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.findByUserAndChefAndIsDeletedFalse(testUser, testChef))
                .thenReturn(Optional.of(testFavoriteChef));
        
        // Act
        favoriteChefService.removeFavoriteChef(1L, 1L);

        // Assert
        assertTrue(testFavoriteChef.getIsDeleted());
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).findByUserAndChefAndIsDeletedFalse(testUser, testChef);
        verify(favoriteChefRepository).save(testFavoriteChef);
    }

    @Test
    @DisplayName("Test 2: removeFavoriteChef with non-existent user should throw exception")
    void removeFavoriteChef_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(99L)).thenReturn(null);

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.removeFavoriteChef(99L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("User not found"));
        
        verify(userRepository).findExistUserById(99L);
        verifyNoInteractions(chefRepository);
        verifyNoInteractions(favoriteChefRepository);
    }

    @Test
    @DisplayName("Test 3: removeFavoriteChef with non-existent chef should throw exception")
    void removeFavoriteChef_WithNonExistentChef_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.removeFavoriteChef(1L, 99L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Chef not found"));
        
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(99L);
        verifyNoInteractions(favoriteChefRepository);
    }

    @Test
    @DisplayName("Test 4: removeFavoriteChef with chef not in favorites should throw exception")
    void removeFavoriteChef_WithChefNotInFavorites_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.findByUserAndChefAndIsDeletedFalse(testUser, testChef))
                .thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.removeFavoriteChef(1L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("not found in favorites"));
        
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).findByUserAndChefAndIsDeletedFalse(testUser, testChef);
    }

    // ==================== getFavoriteChefs Tests ====================

    @Test
    @DisplayName("Test 1: getFavoriteChefs with valid data should return favorites list")
    void getFavoriteChefs_WithValidData_ShouldReturnFavoritesList() {
        // Arrange
        int requestedPageSize = 10;
        List<FavoriteChef> favoriteChefs = new ArrayList<>();
        favoriteChefs.add(testFavoriteChef);
        
        // Create a sample page with 1 favorite chef and pageSize = 10
        Page<FavoriteChef> page = new PageImpl<>(favoriteChefs, 
                PageRequest.of(0, requestedPageSize, Sort.by("id").ascending()), 1);
        
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(favoriteChefRepository.findByUserIdAndIsDeletedFalse(eq(1L), any(Pageable.class))).thenReturn(page);

        // Act
        FavoriteChefsResponse response = favoriteChefService.getFavoriteChefs(1L, 0, requestedPageSize, "id", "asc");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPageNo());
        assertEquals(requestedPageSize, response.getPageSize());
        assertEquals(1, response.getTotalElements());
        
        // Capture the pageable argument to verify it was created correctly
        verify(favoriteChefRepository).findByUserIdAndIsDeletedFalse(eq(1L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(requestedPageSize, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "id"), pageable.getSort());
    }

    @Test
    @DisplayName("Test 2: getFavoriteChefs with non-existent user should throw exception")
    void getFavoriteChefs_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(99L)).thenReturn(null);

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.getFavoriteChefs(99L, 0, 10, "id", "asc");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("User not found"));
        
        verify(userRepository).findExistUserById(99L);
        verifyNoInteractions(favoriteChefRepository);
    }

    @Test
    @DisplayName("Test 3: getFavoriteChefs with descending sort should sort correctly")
    void getFavoriteChefs_WithDescendingSort_ShouldSortCorrectly() {
        // Arrange
        List<FavoriteChef> favoriteChefs = new ArrayList<>();
        favoriteChefs.add(testFavoriteChef);
        
        Page<FavoriteChef> page = new PageImpl<>(favoriteChefs);
        
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(favoriteChefRepository.findByUserIdAndIsDeletedFalse(eq(1L), any(Pageable.class))).thenReturn(page);

        // Act
        FavoriteChefsResponse response = favoriteChefService.getFavoriteChefs(1L, 0, 10, "id", "desc");

        // Assert
        assertNotNull(response);
        
        // Verify that the sort direction was set to DESC
        verify(favoriteChefRepository).findByUserIdAndIsDeletedFalse(eq(1L), argThat(pageable -> 
            pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "id"))
        ));
    }

    @Test
    @DisplayName("Test 4: getFavoriteChefs with empty favorites should return empty list")
    void getFavoriteChefs_WithEmptyFavorites_ShouldReturnEmptyList() {
        // Arrange
        List<FavoriteChef> favoriteChefs = new ArrayList<>();
        Page<FavoriteChef> page = new PageImpl<>(favoriteChefs);
        
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(favoriteChefRepository.findByUserIdAndIsDeletedFalse(eq(1L), any(Pageable.class))).thenReturn(page);

        // Act
        FavoriteChefsResponse response = favoriteChefService.getFavoriteChefs(1L, 0, 10, "id", "asc");

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
        assertEquals(0, response.getTotalElements());
        
        verify(userRepository).findExistUserById(1L);
        verify(favoriteChefRepository).findByUserIdAndIsDeletedFalse(eq(1L), any(Pageable.class));
    }

    // ==================== isChefFavorite Tests ====================

    @Test
    @DisplayName("Test 1: isChefFavorite with chef in favorites should return true")
    void isChefFavorite_WithChefInFavorites_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(testUser, testChef)).thenReturn(true);

        // Act
        boolean result = favoriteChefService.isChefFavorite(1L, 1L);

        // Assert
        assertTrue(result);
        
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).existsByUserAndChefAndIsDeletedFalse(testUser, testChef);
    }

    @Test
    @DisplayName("Test 2: isChefFavorite with chef not in favorites should return false")
    void isChefFavorite_WithChefNotInFavorites_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(testUser, testChef)).thenReturn(false);

        // Act
        boolean result = favoriteChefService.isChefFavorite(1L, 1L);

        // Assert
        assertFalse(result);
        
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).existsByUserAndChefAndIsDeletedFalse(testUser, testChef);
    }

    @Test
    @DisplayName("Test 3: isChefFavorite with non-existent user should throw exception")
    void isChefFavorite_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(99L)).thenReturn(null);

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.isChefFavorite(99L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("User not found"));
        
        verify(userRepository).findExistUserById(99L);
        verifyNoInteractions(chefRepository);
        verifyNoInteractions(favoriteChefRepository);
    }

    @Test
    @DisplayName("Test 4: isChefFavorite with non-existent chef should throw exception")
    void isChefFavorite_WithNonExistentChef_ShouldThrowException() {
        // Arrange
        when(userRepository.findExistUserById(1L)).thenReturn(testUser);
        when(chefRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.isChefFavorite(1L, 99L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Chef not found"));
        
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(99L);
        verifyNoInteractions(favoriteChefRepository);
    }
} 