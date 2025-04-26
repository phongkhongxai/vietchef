package com.spring2025.vietchefs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.FavoriteChef;
import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.FavoriteChefRepository;
import com.spring2025.vietchefs.repositories.RoleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FavoriteChefIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChefRepository chefRepository;

    @Autowired
    private FavoriteChefRepository favoriteChefRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Chef testChef;
    private Long userId;
    private Long chefId;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        favoriteChefRepository.deleteAll();
        chefRepository.deleteAll();
        userRepository.deleteAll();

        // Create roles if not exist
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName("ROLE_CUSTOMER");
                    return roleRepository.save(role);
                });

        // Create test user
        testUser = new User();
        testUser.setFullName("Test Customer");
        testUser.setUsername("testcustomer");
        testUser.setEmail("customer@example.com");
        testUser.setPhone("1234567890");
        testUser.setPassword("password");
        testUser.setGender("Male");
        testUser.setDob(LocalDate.now());
        testUser.setEmailVerified(true);
        testUser.setRole(customerRole);
        testUser = userRepository.save(testUser);
        userId = testUser.getId();

        // Create test chef user
        User chefUser = new User();
        chefUser.setFullName("Test Chef");
        chefUser.setUsername("testchef");
        chefUser.setEmail("chef@example.com");
        chefUser.setPhone("0987654321");
        chefUser.setPassword("password");
        chefUser.setGender("Male");
        chefUser.setDob(LocalDate.now());
        chefUser.setEmailVerified(true);
        chefUser.setRole(customerRole); // Chef users also have customer roles
        chefUser = userRepository.save(chefUser);

        // Create test chef
        testChef = new Chef();
        testChef.setUser(chefUser);
        testChef.setBio("Test Chef Bio");
        testChef.setDescription("Test Chef Description");
        testChef.setAddress("123 Test Street");
        testChef.setStatus("APPROVED");
        testChef.setIsDeleted(false);
        testChef.setSpecialization("Vietnamese Cuisine");
        testChef.setPrice(new BigDecimal("50.00"));
        testChef.setCountry("Vietnam");
        testChef = chefRepository.save(testChef);
        chefId = testChef.getId();
    }

    @Test
    @WithMockUser(username = "testcustomer", roles = {"CUSTOMER"})
    void addFavoriteChef_Success() throws Exception {
        mockMvc.perform(post("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.chefId").value(chefId))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @WithMockUser(username = "testcustomer", roles = {"CUSTOMER"})
    void removeFavoriteChef_Success() throws Exception {
        // First add to favorites
        FavoriteChef favoriteChef = new FavoriteChef();
        favoriteChef.setUser(testUser);
        favoriteChef.setChef(testChef);
        favoriteChef.setIsDeleted(false);
        favoriteChefRepository.save(favoriteChef);

        // Then remove
        mockMvc.perform(delete("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Chef removed from favorites successfully"))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @WithMockUser(username = "testcustomer", roles = {"CUSTOMER"})
    void getFavoriteChefs_Success() throws Exception {
        // Add to favorites
        FavoriteChef favoriteChef = new FavoriteChef();
        favoriteChef.setUser(testUser);
        favoriteChef.setChef(testChef);
        favoriteChef.setIsDeleted(false);
        favoriteChefRepository.save(favoriteChef);

        // Get favorites
        mockMvc.perform(get("/api/v1/favorite-chefs/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(userId))
                .andExpect(jsonPath("$.content[0].chefId").value(chefId))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @WithMockUser(username = "testcustomer", roles = {"CUSTOMER"})
    void isChefFavorite_True() throws Exception {
        // Add to favorites
        FavoriteChef favoriteChef = new FavoriteChef();
        favoriteChef.setUser(testUser);
        favoriteChef.setChef(testChef);
        favoriteChef.setIsDeleted(false);
        favoriteChefRepository.save(favoriteChef);

        // Check if favorite
        mockMvc.perform(get("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @WithMockUser(username = "testcustomer", roles = {"CUSTOMER"})
    void isChefFavorite_False() throws Exception {
        // Check if favorite without adding
        mockMvc.perform(get("/api/v1/favorite-chefs/{userId}/chefs/{chefId}", userId, chefId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"))
                .andDo(MockMvcResultHandlers.print());
    }
} 