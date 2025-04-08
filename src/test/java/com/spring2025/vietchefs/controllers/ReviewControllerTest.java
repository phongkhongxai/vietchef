package com.spring2025.vietchefs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.*;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.services.*;
import com.spring2025.vietchefs.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private ReviewCriteriaService reviewCriteriaService;

    @MockBean
    private ReviewReplyService reviewReplyService;

    @MockBean
    private ReviewReactionService reviewReactionService;

    @MockBean
    private UserService userService;

    @MockBean
    private ChefService chefService;

    @MockBean
    private RoleService roleService;
    
    // Security-related mock beans
    @MockBean
    private UserDetailsService userDetailsService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto currentUser;
    private ReviewCreateRequest createRequest;
    private ReviewUpdateRequest updateRequest;
    private ReviewResponse reviewResponse;
    private List<ReviewResponse> reviewResponseList;
    private ReviewCriteriaResponse criteriaResponse;
    private List<ReviewCriteriaResponse> criteriaResponseList;
    private ReviewReplyRequest replyRequest;
    private ReviewReplyResponse replyResponse;
    private ReviewReactionRequest reactionRequest;
    private ReviewReactionResponse reactionResponse;
    private ChefResponseDto chefResponse;

    @BeforeEach
    void setUp() {
        // Setup current user
        currentUser = new UserDto();
        currentUser.setId(1L);
        currentUser.setEmail("user@example.com");
        currentUser.setUsername("testuser");
        currentUser.setRoleId(2L); // ROLE_CUSTOMER

        // Setup review create request
        createRequest = new ReviewCreateRequest();
        createRequest.setChefId(10L);
        createRequest.setBookingId(100L);
        createRequest.setDescription("Great experience with the chef!");
        createRequest.setOverallExperience("Excellent service and food");
        createRequest.setPhotos("photo1.jpg,photo2.jpg");
        Map<Long, BigDecimal> criteriaRatings = new HashMap<>();
        criteriaRatings.put(1L, new BigDecimal("5.0"));
        criteriaRatings.put(2L, new BigDecimal("4.0"));
        createRequest.setCriteriaRatings(criteriaRatings);
        Map<Long, String> criteriaComments = new HashMap<>();
        criteriaComments.put(1L, "The food was amazing");
        criteriaComments.put(2L, "Service was professional");
        createRequest.setCriteriaComments(criteriaComments);

        // Setup review update request
        updateRequest = new ReviewUpdateRequest();
        updateRequest.setDescription("Updated comment: Excellent experience!");
        updateRequest.setOverallExperience("Even better than expected");
        updateRequest.setPhotos("photo1.jpg,photo2.jpg,photo3.jpg");
        updateRequest.setCriteriaRatings(criteriaRatings);
        updateRequest.setCriteriaComments(criteriaComments);

        // Setup review response
        reviewResponse = new ReviewResponse();
        reviewResponse.setId(1L);
        reviewResponse.setUserId(1L);
        reviewResponse.setUserName("testuser");
        reviewResponse.setChefId(10L);
        reviewResponse.setBookingId(100L);
        reviewResponse.setRating(new BigDecimal("4.5"));
        reviewResponse.setDescription("Great experience with the chef!");
        reviewResponse.setOverallExperience("Excellent service and food");
        reviewResponse.setPhotos("photo1.jpg,photo2.jpg");
        reviewResponse.setVerified(true);
        reviewResponse.setResponse("Thank you for your review");
        reviewResponse.setChefResponseAt(LocalDateTime.now());
        reviewResponse.setCreateAt(LocalDateTime.now());
        Map<String, Long> reactionCounts = new HashMap<>();
        reactionCounts.put("helpful", 5L);
        reactionCounts.put("not_helpful", 1L);
        reviewResponse.setReactionCounts(reactionCounts);

        // Setup review response list
        ReviewResponse reviewResponse2 = new ReviewResponse();
        reviewResponse2.setId(2L);
        reviewResponse2.setUserId(2L);
        reviewResponse2.setUserName("anotheruser");
        reviewResponse2.setChefId(10L);
        reviewResponse2.setBookingId(101L);
        reviewResponse2.setRating(new BigDecimal("5.0"));
        reviewResponse2.setDescription("Best chef ever!");
        reviewResponse2.setOverallExperience("Amazing experience");
        reviewResponse2.setPhotos("photo1.jpg");
        reviewResponse2.setVerified(true);
        reviewResponse2.setCreateAt(LocalDateTime.now());
        
        reviewResponseList = Arrays.asList(reviewResponse, reviewResponse2);

        // Setup criteria response
        criteriaResponse = new ReviewCriteriaResponse();
        criteriaResponse.setCriteriaId(1L);
        criteriaResponse.setName("Food Quality");
        criteriaResponse.setDescription("Quality of the prepared food");
        criteriaResponse.setWeight(new BigDecimal("0.5"));
        criteriaResponse.setIsActive(true);
        criteriaResponse.setDisplayOrder(1);

        // Setup criteria response list
        ReviewCriteriaResponse criteriaResponse2 = new ReviewCriteriaResponse();
        criteriaResponse2.setCriteriaId(2L);
        criteriaResponse2.setName("Service");
        criteriaResponse2.setDescription("Quality of service provided");
        criteriaResponse2.setWeight(new BigDecimal("0.3"));
        criteriaResponse2.setIsActive(true);
        criteriaResponse2.setDisplayOrder(2);
        
        criteriaResponseList = Arrays.asList(criteriaResponse, criteriaResponse2);

        // Setup reply request
        replyRequest = new ReviewReplyRequest();
        replyRequest.setContent("Thank you for your review!");

        // Setup reply response
        replyResponse = new ReviewReplyResponse();
        replyResponse.setReplyId(1L);
        replyResponse.setReviewId(1L);
        replyResponse.setUserId(1L);
        replyResponse.setUserName("testuser");
        replyResponse.setUserAvatar("avatar.jpg");
        replyResponse.setContent("Thank you for your review!");
        replyResponse.setCreatedAt(LocalDateTime.now());

        // Setup reaction request
        reactionRequest = new ReviewReactionRequest();
        reactionRequest.setReactionType("helpful");

        // Setup reaction response
        reactionResponse = new ReviewReactionResponse();
        reactionResponse.setReactionId(1L);
        reactionResponse.setReviewId(1L);
        reactionResponse.setUserId(1L);
        reactionResponse.setUserName("testuser");
        reactionResponse.setReactionType("helpful");
        reactionResponse.setCreatedAt(LocalDateTime.now());

        // Setup chef response
        chefResponse = new ChefResponseDto();
        chefResponse.setId(10L);
        chefResponse.setUser(currentUser);
    }

    @Test
    @DisplayName("Should get all review criteria")
    void shouldGetAllReviewCriteria() throws Exception {
        when(reviewCriteriaService.getActiveCriteria()).thenReturn(criteriaResponseList);

        mockMvc.perform(get("/api/review-criteria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].criteriaId", is(1)))
                .andExpect(jsonPath("$[0].name", is("Food Quality")))
                .andExpect(jsonPath("$[1].criteriaId", is(2)))
                .andExpect(jsonPath("$[1].name", is("Service")));

        verify(reviewCriteriaService, times(1)).getActiveCriteria();
    }

    @Test
    @DisplayName("Should create review criteria when authenticated as ADMIN")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateReviewCriteriaWhenAdmin() throws Exception {
        when(reviewCriteriaService.createCriteria(any(ReviewCriteriaRequest.class))).thenReturn(criteriaResponse);

        ReviewCriteriaRequest criteriaRequest = new ReviewCriteriaRequest();
        criteriaRequest.setName("Food Quality");
        criteriaRequest.setDescription("Quality of the prepared food");
        criteriaRequest.setWeight(new BigDecimal("0.5"));
        criteriaRequest.setIsActive(true);
        criteriaRequest.setDisplayOrder(1);

        mockMvc.perform(post("/api/review-criteria")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criteriaRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criteriaId", is(1)))
                .andExpect(jsonPath("$.name", is("Food Quality")))
                .andExpect(jsonPath("$.description", is("Quality of the prepared food")))
                .andExpect(jsonPath("$.isActive", is(true)));

        verify(reviewCriteriaService, times(1)).createCriteria(any(ReviewCriteriaRequest.class));
    }

    @Test
    @DisplayName("Should get reviews for a chef")
    void shouldGetReviewsByChef() throws Exception {
        Page<ReviewResponse> reviewPage = new PageImpl<>(reviewResponseList);
        
        when(reviewService.getReviewsByChef(eq(10L), any(Pageable.class))).thenReturn(reviewPage);
        when(reviewService.getAverageRatingForChef(10L)).thenReturn(new BigDecimal("4.75"));
        when(reviewService.getReviewCountForChef(10L)).thenReturn(2L);
        Map<String, Long> ratingDistribution = Map.of("5", 1L, "4", 1L);
        when(reviewService.getRatingDistributionForChef(10L)).thenReturn(ratingDistribution);

        mockMvc.perform(get("/api/reviews/chef/10")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews", hasSize(2)))
                .andExpect(jsonPath("$.reviews[0].id", is(1)))
                .andExpect(jsonPath("$.reviews[1].id", is(2)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.averageRating", is(4.75)))
                .andExpect(jsonPath("$.totalReviews", is(2)))
                .andExpect(jsonPath("$.ratingDistribution.\"5\"", is(1)))
                .andExpect(jsonPath("$.ratingDistribution.\"4\"", is(1)));

        verify(reviewService, times(1)).getReviewsByChef(eq(10L), any(Pageable.class));
        verify(reviewService, times(1)).getAverageRatingForChef(10L);
        verify(reviewService, times(1)).getReviewCountForChef(10L);
        verify(reviewService, times(1)).getRatingDistributionForChef(10L);
    }

    @Test
    @DisplayName("Should get a review by ID")
    void shouldGetReviewById() throws Exception {
        when(reviewService.getReviewById(1L)).thenReturn(reviewResponse);

        mockMvc.perform(get("/api/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.chefId", is(10)))
                .andExpect(jsonPath("$.rating", is(4.5)))
                .andExpect(jsonPath("$.description", is("Great experience with the chef!")));

        verify(reviewService, times(1)).getReviewById(1L);
    }

    @Test
    @DisplayName("Should create a review when authenticated as CUSTOMER")
    @WithMockUser(roles = "CUSTOMER", username = "testuser")
    void shouldCreateReviewWhenCustomer() throws Exception {
        when(userService.getProfileUserByUsernameOrEmail("testuser", "testuser")).thenReturn(currentUser);
        when(reviewService.createReview(any(ReviewCreateRequest.class), eq(1L))).thenReturn(reviewResponse);

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.chefId", is(10)))
                .andExpect(jsonPath("$.rating", is(4.5)))
                .andExpect(jsonPath("$.description", is("Great experience with the chef!")));

        verify(userService, times(1)).getProfileUserByUsernameOrEmail("testuser", "testuser");
        verify(reviewService, times(1)).createReview(any(ReviewCreateRequest.class), eq(1L));
    }

    @Test
    @DisplayName("Should update a review when authenticated as CUSTOMER and owner")
    @WithMockUser(roles = "CUSTOMER", username = "testuser")
    void shouldUpdateReviewWhenCustomerAndOwner() throws Exception {
        when(userService.getProfileUserByUsernameOrEmail("testuser", "testuser")).thenReturn(currentUser);
        
        ReviewResponse updatedReview = new ReviewResponse();
        updatedReview.setId(1L);
        updatedReview.setUserId(1L);
        updatedReview.setUserName("testuser");
        updatedReview.setChefId(10L);
        updatedReview.setBookingId(100L);
        updatedReview.setRating(new BigDecimal("5.0"));
        updatedReview.setDescription("Updated comment: Excellent experience!");
        updatedReview.setOverallExperience("Even better than expected");
        updatedReview.setPhotos("photo1.jpg,photo2.jpg,photo3.jpg");
        updatedReview.setVerified(true);
        updatedReview.setCreateAt(LocalDateTime.now());
        
        when(reviewService.updateReview(eq(1L), any(ReviewUpdateRequest.class), eq(1L))).thenReturn(updatedReview);

        mockMvc.perform(put("/api/reviews/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.chefId", is(10)))
                .andExpect(jsonPath("$.rating", is(5.0)))
                .andExpect(jsonPath("$.description", is("Updated comment: Excellent experience!")));

        verify(userService, times(1)).getProfileUserByUsernameOrEmail("testuser", "testuser");
        verify(reviewService, times(1)).updateReview(eq(1L), any(ReviewUpdateRequest.class), eq(1L));
    }

    @Test
    @DisplayName("Should delete a review when authenticated as CUSTOMER and owner")
    @WithMockUser(roles = "CUSTOMER", username = "testuser")
    void shouldDeleteReviewWhenCustomerAndOwner() throws Exception {
        when(userService.getProfileUserByUsernameOrEmail("testuser", "testuser")).thenReturn(currentUser);
        when(reviewService.getReviewById(1L)).thenReturn(reviewResponse);
        doNothing().when(reviewService).deleteReview(1L);

        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).getProfileUserByUsernameOrEmail("testuser", "testuser");
        verify(reviewService, times(1)).getReviewById(1L);
        verify(reviewService, times(1)).deleteReview(1L);
    }

    @Test
    @DisplayName("Should add chef response to a review when authenticated as CHEF")
    @WithMockUser(roles = "CHEF", username = "chefuser")
    void shouldAddChefResponseWhenChef() throws Exception {
        UserDto chefUser = new UserDto();
        chefUser.setId(3L);
        chefUser.setEmail("chef@example.com");
        chefUser.setUsername("chefuser");
        chefUser.setRoleId(3L); // ROLE_CHEF
        
        when(userService.getProfileUserByUsernameOrEmail("chefuser", "chefuser")).thenReturn(chefUser);
        when(reviewService.getReviewById(1L)).thenReturn(reviewResponse);
        
        ChefResponseDto chef = new ChefResponseDto();
        chef.setId(10L);
        chef.setUser(chefUser);
        when(chefService.getChefById(10L)).thenReturn(chef);
        
        ReviewResponse reviewWithResponse = new ReviewResponse();
        reviewWithResponse.setId(1L);
        reviewWithResponse.setUserId(1L);
        reviewWithResponse.setUserName("testuser");
        reviewWithResponse.setChefId(10L);
        reviewWithResponse.setRating(new BigDecimal("4.5"));
        reviewWithResponse.setDescription("Great experience with the chef!");
        reviewWithResponse.setResponse("Thank you for your kind review!");
        reviewWithResponse.setChefResponseAt(LocalDateTime.now());
        
        when(reviewService.addChefResponse(eq(1L), eq("Thank you for your kind review!"), eq(3L)))
                .thenReturn(reviewWithResponse);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("response", "Thank you for your kind review!");

        mockMvc.perform(post("/api/reviews/1/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(responseMap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.chefId", is(10)))
                .andExpect(jsonPath("$.response", is("Thank you for your kind review!")));

        verify(userService, times(1)).getProfileUserByUsernameOrEmail("chefuser", "chefuser");
        verify(reviewService, times(1)).getReviewById(1L);
        verify(chefService, times(1)).getChefById(10L);
        verify(reviewService, times(1)).addChefResponse(eq(1L), eq("Thank you for your kind review!"), eq(3L));
    }

    @Test
    @DisplayName("Should add a reply to a review when authenticated")
    @WithMockUser(username = "testuser")
    void shouldAddReplyWhenAuthenticated() throws Exception {
        when(userService.getProfileUserByUsernameOrEmail("testuser", "testuser")).thenReturn(currentUser);
        when(reviewReplyService.addReply(eq(1L), eq(1L), any(ReviewReplyRequest.class))).thenReturn(replyResponse);

        mockMvc.perform(post("/api/reviews/1/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replyId", is(1)))
                .andExpect(jsonPath("$.reviewId", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.content", is("Thank you for your review!")));

        verify(userService, times(1)).getProfileUserByUsernameOrEmail("testuser", "testuser");
        verify(reviewReplyService, times(1)).addReply(eq(1L), eq(1L), any(ReviewReplyRequest.class));
    }

    @Test
    @DisplayName("Should add a reaction to a review when authenticated")
    @WithMockUser(username = "testuser")
    void shouldAddReactionWhenAuthenticated() throws Exception {
        when(userService.getProfileUserByUsernameOrEmail("testuser", "testuser")).thenReturn(currentUser);
        when(reviewReactionService.addReaction(eq(1L), eq(1L), any(ReviewReactionRequest.class))).thenReturn(reactionResponse);
        
        Map<String, Long> reactionCounts = Map.of("helpful", 3L, "not_helpful", 1L);
        when(reviewReactionService.getReactionCountsByReview(1L)).thenReturn(reactionCounts);

        mockMvc.perform(post("/api/reviews/1/reaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reaction.reactionId", is(1)))
                .andExpect(jsonPath("$.reaction.reviewId", is(1)))
                .andExpect(jsonPath("$.reaction.userId", is(1)))
                .andExpect(jsonPath("$.reaction.reactionType", is("helpful")))
                .andExpect(jsonPath("$.counts.helpful", is(3)))
                .andExpect(jsonPath("$.counts.not_helpful", is(1)));

        verify(userService, times(1)).getProfileUserByUsernameOrEmail("testuser", "testuser");
        verify(reviewReactionService, times(1)).addReaction(eq(1L), eq(1L), any(ReviewReactionRequest.class));
        verify(reviewReactionService, times(1)).getReactionCountsByReview(1L);
    }
} 