package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.FavoriteChef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.FavoriteChefDto;
import com.spring2025.vietchefs.models.payload.responseModel.FavoriteChefsResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.FavoriteChefRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteChefServiceImplTest {

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

    private User testUser;
    private Chef testChef;
    private FavoriteChef testFavoriteChef;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        // Setup test chef
        testChef = new Chef();
        testChef.setId(1L);
        testChef.setStatus("APPROVED");
        testChef.setUser(testUser); // Use the same user for simplicity
        testChef.setSpecialization("Vietnamese Cuisine");
        testChef.setAddress("123 Test Street");

        // Setup test favorite chef
        testFavoriteChef = new FavoriteChef();
        testFavoriteChef.setId(1L);
        testFavoriteChef.setUser(testUser);
        testFavoriteChef.setChef(testChef);
        testFavoriteChef.setIsDeleted(false);
        testFavoriteChef.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void addFavoriteChef_Success() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(any(User.class), any(Chef.class))).thenReturn(false);
        when(favoriteChefRepository.save(any(FavoriteChef.class))).thenReturn(testFavoriteChef);

        // Act
        FavoriteChefDto result = favoriteChefService.addFavoriteChef(1L, 1L);

        // Assert
        assertNotNull(result);
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).existsByUserAndChefAndIsDeletedFalse(testUser, testChef);
        verify(favoriteChefRepository).save(any(FavoriteChef.class));
    }

    @Test
    void addFavoriteChef_UserNotFound() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(null);

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.addFavoriteChef(1L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository, never()).findById(anyLong());
    }

    @Test
    void addFavoriteChef_ChefNotFound() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.addFavoriteChef(1L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Chef not found"));
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
    }

    @Test
    void addFavoriteChef_ChefNotActive() {
        // Arrange
        testChef.setStatus("PENDING");
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.of(testChef));

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.addFavoriteChef(1L, 1L);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Chef is not active"));
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
    }

    @Test
    void addFavoriteChef_AlreadyFavorite() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(any(User.class), any(Chef.class))).thenReturn(true);

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.addFavoriteChef(1L, 1L);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Chef already in favorites"));
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).existsByUserAndChefAndIsDeletedFalse(testUser, testChef);
    }

    @Test
    void removeFavoriteChef_Success() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.findByUserAndChefAndIsDeletedFalse(any(User.class), any(Chef.class)))
                .thenReturn(Optional.of(testFavoriteChef));

        // Act
        favoriteChefService.removeFavoriteChef(1L, 1L);

        // Assert
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).findByUserAndChefAndIsDeletedFalse(testUser, testChef);
        verify(favoriteChefRepository).save(any(FavoriteChef.class));
        assertTrue(testFavoriteChef.getIsDeleted());
    }

    @Test
    void removeFavoriteChef_NotFound() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.findByUserAndChefAndIsDeletedFalse(any(User.class), any(Chef.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        VchefApiException exception = assertThrows(VchefApiException.class, () -> {
            favoriteChefService.removeFavoriteChef(1L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("Chef not found in favorites"));
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).findByUserAndChefAndIsDeletedFalse(testUser, testChef);
    }

    @Test
    void getFavoriteChefs_Success() {
        // Arrange
        List<FavoriteChef> favoriteChefs = new ArrayList<>();
        favoriteChefs.add(testFavoriteChef);
        Page<FavoriteChef> favoriteChefsPage = new PageImpl<>(favoriteChefs);

        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(favoriteChefRepository.findByUserIdAndIsDeletedFalse(anyLong(), any(Pageable.class)))
                .thenReturn(favoriteChefsPage);

        // Act
        FavoriteChefsResponse response = favoriteChefService.getFavoriteChefs(1L, 0, 10, "id", "asc");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(userRepository).findExistUserById(1L);
        verify(favoriteChefRepository).findByUserIdAndIsDeletedFalse(eq(1L), any(Pageable.class));
    }

    @Test
    void isChefFavorite_True() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(any(User.class), any(Chef.class)))
                .thenReturn(true);

        // Act
        boolean result = favoriteChefService.isChefFavorite(1L, 1L);

        // Assert
        assertTrue(result);
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).existsByUserAndChefAndIsDeletedFalse(testUser, testChef);
    }

    @Test
    void isChefFavorite_False() {
        // Arrange
        when(userRepository.findExistUserById(anyLong())).thenReturn(testUser);
        when(chefRepository.findById(anyLong())).thenReturn(Optional.of(testChef));
        when(favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(any(User.class), any(Chef.class)))
                .thenReturn(false);

        // Act
        boolean result = favoriteChefService.isChefFavorite(1L, 1L);

        // Assert
        assertFalse(result);
        verify(userRepository).findExistUserById(1L);
        verify(chefRepository).findById(1L);
        verify(favoriteChefRepository).existsByUserAndChefAndIsDeletedFalse(testUser, testChef);
    }
} 