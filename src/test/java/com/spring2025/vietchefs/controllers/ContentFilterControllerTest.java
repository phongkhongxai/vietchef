package com.spring2025.vietchefs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring2025.vietchefs.services.ContentFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContentFilterControllerTest {

    @Mock
    private ContentFilterService contentFilterService;

    @InjectMocks
    private ContentFilterController contentFilterController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contentFilterController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllProfanityWords_ShouldReturnWordList() throws Exception {
        // Arrange
        Set<String> profanityWords = new HashSet<>(Arrays.asList("badword1", "badword2", "offensive"));
        when(contentFilterService.getAllProfanityWords()).thenReturn(profanityWords);

        // Act & Assert
        mockMvc.perform(get("/api/v1/content-filter/profanity-words"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", hasItems("badword1", "badword2", "offensive")));
    }

    @Test
    void addProfanityWord_ShouldAddWord() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("word", "newbadword");

        // Act & Assert
        mockMvc.perform(post("/api/v1/content-filter/profanity-words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Đã thêm từ vào danh sách")));

        verify(contentFilterService).addProfanityWord("newbadword");
    }

    @Test
    void addProfanityWord_ShouldReturnBadRequest_WhenWordIsEmpty() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("word", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/content-filter/profanity-words")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Từ không được để trống")));

        verify(contentFilterService, never()).addProfanityWord(anyString());
    }

    @Test
    void removeProfanityWord_ShouldRemoveWord() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/content-filter/profanity-words/badword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Đã xóa từ khỏi danh sách")));

        verify(contentFilterService).removeProfanityWord("badword");
    }

    @Test
    void checkContent_ShouldReturnFilteredText() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", "This text contains badword and offensive content");

        when(contentFilterService.containsProfanity("This text contains badword and offensive content")).thenReturn(true);
        when(contentFilterService.filterText("This text contains badword and offensive content"))
                .thenReturn("This text contains *** and *** content");
        when(contentFilterService.findProfanityWords("This text contains badword and offensive content"))
                .thenReturn(Arrays.asList("badword", "offensive"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/content-filter/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.containsProfanity", is(true)))
                .andExpect(jsonPath("$.original", is("This text contains badword and offensive content")))
                .andExpect(jsonPath("$.filtered", is("This text contains *** and *** content")))
                .andExpect(jsonPath("$.profanityWords", hasSize(2)))
                .andExpect(jsonPath("$.profanityWords", hasItems("badword", "offensive")));
    }

    @Test
    void checkContent_ShouldReturnBadRequest_WhenTextIsEmpty() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/content-filter/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Văn bản không được để trống")));

        verify(contentFilterService, never()).containsProfanity(anyString());
        verify(contentFilterService, never()).filterText(anyString());
        verify(contentFilterService, never()).findProfanityWords(anyString());
    }
} 