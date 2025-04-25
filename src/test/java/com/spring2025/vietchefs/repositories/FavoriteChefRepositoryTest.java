package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.FavoriteChef;
import com.spring2025.vietchefs.models.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class FavoriteChefRepositoryTest {

    @Autowired
    private FavoriteChefRepository favoriteChefRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChefRepository chefRepository;

    private User testUser;
    private Chef testChef;
    private FavoriteChef testFavoriteChef;

    @BeforeEach
    void setUp() {
        // Cleanup previous test data
        favoriteChefRepository.deleteAll();
        
        // Create test user
        testUser = new User();
        testUser.setFullName("Test User");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("1234567890");
        testUser.setPassword("password");
        testUser.setGender("Male");
        testUser.setDob(LocalDate.now());
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        // Create test chef
        testChef = new Chef();
        testChef.setUser(testUser);
        testChef.setBio("Test Chef Bio");
        testChef.setDescription("Test Chef Description");
        testChef.setAddress("123 Test Street");
        testChef.setStatus("APPROVED");
        testChef.setIsDeleted(false);
        testChef.setSpecialization("Vietnamese Cuisine");
        testChef = chefRepository.save(testChef);

        // Create favorite chef entry
        testFavoriteChef = new FavoriteChef();
        testFavoriteChef.setUser(testUser);
        testFavoriteChef.setChef(testChef);
        testFavoriteChef.setIsDeleted(false);
        testFavoriteChef = favoriteChefRepository.save(testFavoriteChef);
    }

    @Test
    void findByUserAndChefAndIsDeletedFalse_ReturnsMatch() {
        // Act
        Optional<FavoriteChef> result = favoriteChefRepository.findByUserAndChefAndIsDeletedFalse(testUser, testChef);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testFavoriteChef.getId(), result.get().getId());
    }

    @Test
    void findByUserAndChefAndIsDeletedFalse_NoMatch_ReturnsEmpty() {
        // Arrange
        testFavoriteChef.setIsDeleted(true);
        favoriteChefRepository.save(testFavoriteChef);

        // Act
        Optional<FavoriteChef> result = favoriteChefRepository.findByUserAndChefAndIsDeletedFalse(testUser, testChef);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findByUserAndIsDeletedFalse_ReturnsMatches() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FavoriteChef> result = favoriteChefRepository.findByUserAndIsDeletedFalse(testUser, pageable);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(testFavoriteChef.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findByUserIdAndIsDeletedFalse_ReturnsMatches() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FavoriteChef> result = favoriteChefRepository.findByUserIdAndIsDeletedFalse(testUser.getId(), pageable);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(testFavoriteChef.getId(), result.getContent().get(0).getId());
    }

    @Test
    void existsByUserAndChefAndIsDeletedFalse_ReturnsTrueWhenExists() {
        // Act
        boolean result = favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(testUser, testChef);

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByUserAndChefAndIsDeletedFalse_ReturnsFalseWhenDeleted() {
        // Arrange
        testFavoriteChef.setIsDeleted(true);
        favoriteChefRepository.save(testFavoriteChef);

        // Act
        boolean result = favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(testUser, testChef);

        // Assert
        assertFalse(result);
    }

    @Test
    void existsByUserAndChefAndIsDeletedFalse_ReturnsFalseWhenNotExists() {
        // Arrange
        favoriteChefRepository.delete(testFavoriteChef);

        // Act
        boolean result = favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(testUser, testChef);

        // Assert
        assertFalse(result);
    }
} 