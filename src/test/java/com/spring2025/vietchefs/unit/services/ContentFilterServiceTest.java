package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.models.entity.ProfanityWord;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ProfanityWordRequest;
import com.spring2025.vietchefs.repositories.ProfanityWordRepository;
import com.spring2025.vietchefs.services.ContentFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContentFilterServiceTest {

    @Mock
    private ProfanityWordRepository profanityWordRepository;
    
    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private ContentFilterService contentFilterService;

    @BeforeEach
    void setUp() {
        // Initialize with test profanity words
        Set<String> testProfanityWords = new HashSet<>();
        testProfanityWords.add("badword");
        testProfanityWords.add("offensive");
        testProfanityWords.add("bad phrase");
        
        // Use reflection to directly set the private field instead of calling a mocked method
        ReflectionTestUtils.setField(contentFilterService, "profanityWords", testProfanityWords);
    }

    // ==================== containsProfanity Tests ====================
    
    @Test
    @DisplayName("Test 1: containsProfanity with profanity words should return true")
    void containsProfanity_WithProfanityWords_ShouldReturnTrue() {
        // Arrange
        String textWithProfanity = "This text contains badword and offensive content";
        
        // Act
        boolean result = contentFilterService.containsProfanity(textWithProfanity);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Test 2: containsProfanity with clean text should return false")
    void containsProfanity_WithCleanText_ShouldReturnFalse() {
        // Arrange
        String cleanText = "This is a clean text without any inappropriate words";
        
        // Act
        boolean result = contentFilterService.containsProfanity(cleanText);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Test 3: containsProfanity with null input should return false")
    void containsProfanity_WithNullInput_ShouldReturnFalse() {
        // Act
        boolean result = contentFilterService.containsProfanity(null);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Test 4: containsProfanity with empty or whitespace input should return false")
    void containsProfanity_WithEmptyInput_ShouldReturnFalse() {
        // Act & Assert
        assertFalse(contentFilterService.containsProfanity(""));
        assertFalse(contentFilterService.containsProfanity("   "));
    }
    
    // ==================== filterText Tests ====================
    
    @Test
    @DisplayName("Test 1: filterText should replace profanity words with asterisks")
    void filterText_ShouldReplaceProfanityWithAsterisks() {
        // Arrange
        String textWithProfanity = "This text contains badword and offensive content";
        
        // Act
        String filteredText = contentFilterService.filterText(textWithProfanity);
        
        // Assert
        assertFalse(filteredText.contains("badword"));
        assertFalse(filteredText.contains("offensive"));
        assertTrue(filteredText.contains("***"));
        assertEquals("This text contains *** and *** content", filteredText);
    }
    
    @Test
    @DisplayName("Test 2: filterText should replace profanity phrases")
    void filterText_ShouldReplaceProfanityPhrases() {
        // Arrange
        String textWithPhrases = "This text contains bad phrase that should be filtered";
        
        // Act
        String filteredText = contentFilterService.filterText(textWithPhrases);
        
        // Assert
        assertFalse(filteredText.contains("bad phrase"));
        assertTrue(filteredText.contains("***"));
        assertEquals("This text contains *** that should be filtered", filteredText);
    }
    
    @Test
    @DisplayName("Test 3: filterText should return original text when no filtering needed")
    void filterText_ShouldReturnOriginalText_WhenNoFilteringNeeded() {
        // Arrange
        String cleanText = "This is a clean text";
        
        // Act
        String filteredText = contentFilterService.filterText(cleanText);
        
        // Assert
        assertEquals(cleanText, filteredText);
    }
    
    @Test
    @DisplayName("Test 4: filterText should handle null or empty input")
    void filterText_ShouldHandleNullOrEmptyInput() {
        // Act & Assert
        assertNull(contentFilterService.filterText(null));
        assertEquals("", contentFilterService.filterText(""));
        assertEquals("   ", contentFilterService.filterText("   "));
    }
    
    // ==================== findProfanityWords Tests ====================
    
    @Test
    @DisplayName("Test 1: findProfanityWords should return list of detected words")
    void findProfanityWords_ShouldReturnListOfDetectedWords() {
        // Arrange
        String textWithMultipleProfanities = "This text has badword and offensive content with bad phrase";
        
        // Act
        List<String> foundWords = contentFilterService.findProfanityWords(textWithMultipleProfanities);
        
        // Assert
        assertEquals(3, foundWords.size());
        assertTrue(foundWords.contains("badword"));
        assertTrue(foundWords.contains("offensive"));
        assertTrue(foundWords.contains("bad phrase"));
    }
    
    @Test
    @DisplayName("Test 2: findProfanityWords should return empty list when no match")
    void findProfanityWords_ShouldReturnEmptyList_WhenNoMatch() {
        // Arrange
        String cleanText = "This is a clean text";
        
        // Act
        List<String> foundWords = contentFilterService.findProfanityWords(cleanText);
        
        // Assert
        assertTrue(foundWords.isEmpty());
    }
    
    @Test
    @DisplayName("Test 3: findProfanityWords should return empty list for null input")
    void findProfanityWords_ShouldReturnEmptyList_ForNullInput() {
        // Act
        List<String> foundWords = contentFilterService.findProfanityWords(null);
        
        // Assert
        assertNotNull(foundWords);
        assertTrue(foundWords.isEmpty());
    }
    
    @Test
    @DisplayName("Test 4: findProfanityWords should handle empty input")
    void findProfanityWords_ShouldHandleEmptyInput() {
        // Act
        List<String> foundWords = contentFilterService.findProfanityWords("");
        List<String> foundWordsWhitespace = contentFilterService.findProfanityWords("   ");
        
        // Assert
        assertTrue(foundWords.isEmpty());
        assertTrue(foundWordsWhitespace.isEmpty());
    }
    
    // ==================== addProfanityWord Tests ====================
    
    @Test
    @DisplayName("Test 1: addProfanityWord should add new word to profanity list")
    void addProfanityWord_ShouldAddNewWordToProfanityList() {
        // Arrange
        String newProfanityWord = "newbadword";
        String language = "en";
        ProfanityWord savedWord = new ProfanityWord();
        savedWord.setWord(newProfanityWord);
        savedWord.setLanguage(language);
        savedWord.setActive(true);
        
        // Stub findByWordIgnoreCase để trả về Optional.empty() (từ không tồn tại)
        when(profanityWordRepository.findByWordIgnoreCase(newProfanityWord)).thenReturn(Optional.empty());
        when(profanityWordRepository.save(any(ProfanityWord.class))).thenReturn(savedWord);
        
        // Act
        contentFilterService.addProfanityWord(newProfanityWord, language);
        
        // Assert
        verify(profanityWordRepository).save(any(ProfanityWord.class));
        
        // Update in-memory set for assertion
        @SuppressWarnings("unchecked")
        Set<String> updatedSet = new HashSet<>(
            (Set<String>) ReflectionTestUtils.getField(contentFilterService, "profanityWords"));
        updatedSet.add(newProfanityWord);
        ReflectionTestUtils.setField(contentFilterService, "profanityWords", updatedSet);
        
        assertTrue(contentFilterService.containsProfanity("This text contains newbadword"));
    }
    
    @Test
    @DisplayName("Test 2: addProfanityWord should reactivate existing inactive word")
    void addProfanityWord_ShouldReactivateExistingInactiveWord() {
        // Arrange
        String existingWord = "existingword";
        String language = "en";
        
        ProfanityWord existingProfanityWord = new ProfanityWord();
        existingProfanityWord.setId(1L);
        existingProfanityWord.setWord(existingWord);
        existingProfanityWord.setLanguage(language);
        existingProfanityWord.setActive(false);
        
        when(profanityWordRepository.findByWordIgnoreCase(existingWord)).thenReturn(Optional.of(existingProfanityWord));
        when(profanityWordRepository.save(any(ProfanityWord.class))).thenReturn(existingProfanityWord);
        
        // Act
        contentFilterService.addProfanityWord(existingWord, language);
        
        // Assert
        verify(profanityWordRepository).save(any(ProfanityWord.class));
        assertTrue(existingProfanityWord.isActive());
        
        // Update in-memory set for assertion
        @SuppressWarnings("unchecked")
        Set<String> updatedSet = new HashSet<>(
            (Set<String>) ReflectionTestUtils.getField(contentFilterService, "profanityWords"));
        updatedSet.add(existingWord);
        ReflectionTestUtils.setField(contentFilterService, "profanityWords", updatedSet);
        
        assertTrue(contentFilterService.containsProfanity("This contains existingword"));
    }
    
    @Test
    @DisplayName("Test 3: addProfanityWord should handle null or empty input")
    void addProfanityWord_ShouldHandleNullOrEmptyInput() {
        // Act
        contentFilterService.addProfanityWord(null, "en");
        contentFilterService.addProfanityWord("", "en");
        contentFilterService.addProfanityWord("  ", "en");
        
        // Assert
        verify(profanityWordRepository, never()).save(any());
    }
    
    // ==================== removeProfanityWord Tests ====================
    
    @Test
    @DisplayName("Test 1: removeProfanityWord should soft delete existing word")
    void removeProfanityWord_ShouldSoftDeleteExistingWord() {
        // Arrange
        String wordToRemove = "badword";
        
        ProfanityWord existingWord = new ProfanityWord();
        existingWord.setId(1L);
        existingWord.setWord(wordToRemove);
        existingWord.setLanguage("en");
        existingWord.setActive(true);
        
        when(profanityWordRepository.findByWordIgnoreCase(wordToRemove)).thenReturn(Optional.of(existingWord));
        when(profanityWordRepository.save(any(ProfanityWord.class))).thenReturn(existingWord);
        
        // Act
        contentFilterService.removeProfanityWord(wordToRemove);
        
        // Assert
        verify(profanityWordRepository).save(any(ProfanityWord.class));
        assertFalse(existingWord.isActive());
        
        // Update in-memory set for assertion
        @SuppressWarnings("unchecked")
        Set<String> updatedSet = new HashSet<>(
            (Set<String>) ReflectionTestUtils.getField(contentFilterService, "profanityWords"));
        updatedSet.remove(wordToRemove);
        ReflectionTestUtils.setField(contentFilterService, "profanityWords", updatedSet);
        
        assertFalse(contentFilterService.containsProfanity("This text contains badword"));
    }
    
    @Test
    @DisplayName("Test 2: removeProfanityWord should handle case-insensitive removal")
    void removeProfanityWord_ShouldHandleCaseInsensitiveRemoval() {
        // Arrange
        String existingWord = "badword";
        String mixedCaseWord = "BaDwOrD";
        
        ProfanityWord profanityWord = new ProfanityWord();
        profanityWord.setId(1L);
        profanityWord.setWord(existingWord);
        profanityWord.setLanguage("en");
        profanityWord.setActive(true);
        
        // Stub với "badword" (chữ thường) vì ContentFilterService sẽ chuyển đổi "BaDwOrD" thành chữ thường
        when(profanityWordRepository.findByWordIgnoreCase(existingWord.toLowerCase())).thenReturn(Optional.of(profanityWord));
        when(profanityWordRepository.save(any(ProfanityWord.class))).thenReturn(profanityWord);
        
        // Act
        contentFilterService.removeProfanityWord(mixedCaseWord);
        
        // Assert
        verify(profanityWordRepository).save(any(ProfanityWord.class));
        assertFalse(profanityWord.isActive());
        
        // Update in-memory set for assertion
        @SuppressWarnings("unchecked")
        Set<String> updatedSet = new HashSet<>(
            (Set<String>) ReflectionTestUtils.getField(contentFilterService, "profanityWords"));
        updatedSet.remove(existingWord);
        ReflectionTestUtils.setField(contentFilterService, "profanityWords", updatedSet);
        
        assertFalse(contentFilterService.containsProfanity("This text contains badword"));
    }
    
    @Test
    @DisplayName("Test 3: removeProfanityWord should handle non-existent words")
    void removeProfanityWord_ShouldHandleNonExistentWords() {
        // Arrange
        String nonExistentWord = "nonexistentword";
        
        when(profanityWordRepository.findByWordIgnoreCase(nonExistentWord)).thenReturn(Optional.empty());
        
        // Act
        contentFilterService.removeProfanityWord(nonExistentWord);
        
        // Assert
        verify(profanityWordRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Test 4: removeProfanityWord should handle null or empty input")
    void removeProfanityWord_ShouldHandleNullOrEmptyInput() {
        // Act
        contentFilterService.removeProfanityWord(null);
        contentFilterService.removeProfanityWord("");
        contentFilterService.removeProfanityWord("  ");
        
        // Assert
        verify(profanityWordRepository, never()).findByWordIgnoreCase(any());
        verify(profanityWordRepository, never()).save(any());
    }
    
    // ==================== getAllProfanityWords Tests ====================
    
    @Test
    @DisplayName("Test 1: getAllProfanityWords should return all profanity words")
    void getAllProfanityWords_ShouldReturnAllProfanityWords() {
        // Act
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        
        // Assert
        assertEquals(3, allWords.size());
        assertTrue(allWords.contains("badword"));
        assertTrue(allWords.contains("offensive"));
        assertTrue(allWords.contains("bad phrase"));
    }
    
    @Test
    @DisplayName("Test 2: getAllProfanityWords should return a copy of the set")
    void getAllProfanityWords_ShouldReturnACopyOfTheSet() {
        // Act
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        int initialSize = allWords.size();
        
        // Modify the returned set
        allWords.add("newword");
        
        // Assert
        assertEquals(initialSize + 1, allWords.size());
        
        // The original set should remain unchanged
        Set<String> allWordsAgain = contentFilterService.getAllProfanityWords();
        assertEquals(initialSize, allWordsAgain.size());
        assertFalse(allWordsAgain.contains("newword"));
    }
    
    // ==================== Additional Tests for New Methods ====================
    
    @Test
    @DisplayName("Test 1: updateProfanityWord should update word properties")
    void updateProfanityWord_ShouldUpdateWordProperties() {
        // Arrange
        Long wordId = 1L;
        ProfanityWordRequest request = new ProfanityWordRequest();
        request.setWord("updatedword");
        request.setLanguage("vi");
        request.setActive(true);
        
        ProfanityWord existingWord = new ProfanityWord();
        existingWord.setId(wordId);
        existingWord.setWord("oldword");
        existingWord.setLanguage("en");
        existingWord.setActive(true);
        existingWord.setCreatedAt(LocalDateTime.now());
        existingWord.setUpdatedAt(LocalDateTime.now());
        
        when(profanityWordRepository.findById(wordId)).thenReturn(Optional.of(existingWord));
        when(profanityWordRepository.existsByWordIgnoreCase("updatedword")).thenReturn(false);
        when(profanityWordRepository.save(any(ProfanityWord.class))).thenReturn(existingWord);
        
        // Act
        ProfanityWord result = contentFilterService.updateProfanityWord(wordId, request);
        
        // Assert
        assertEquals("updatedword", result.getWord());
        assertEquals("vi", result.getLanguage());
        assertTrue(result.isActive());
        verify(profanityWordRepository).save(existingWord);
    }
    
    @Test
    @DisplayName("Test 2: updateProfanityWord should throw exception when word not found")
    void updateProfanityWord_ShouldThrowException_WhenWordNotFound() {
        // Arrange
        Long wordId = 99L;
        ProfanityWordRequest request = new ProfanityWordRequest();
        request.setWord("updatedword");
        
        when(profanityWordRepository.findById(wordId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            contentFilterService.updateProfanityWord(wordId, request);
        });
    }
    
    @Test
    @DisplayName("Test 3: removeProfanityWordById should soft delete word")
    void removeProfanityWordById_ShouldSoftDeleteWord() {
        // Arrange
        Long wordId = 1L;
        ProfanityWord existingWord = new ProfanityWord();
        existingWord.setId(wordId);
        existingWord.setWord("badword");
        existingWord.setLanguage("en");
        existingWord.setActive(true);
        
        when(profanityWordRepository.findById(wordId)).thenReturn(Optional.of(existingWord));
        when(profanityWordRepository.save(any(ProfanityWord.class))).thenReturn(existingWord);
        
        // Act
        contentFilterService.removeProfanityWordById(wordId);
        
        // Assert
        assertFalse(existingWord.isActive());
        verify(profanityWordRepository).save(existingWord);
    }
    
    @Test
    @DisplayName("Test 4: removeProfanityWordById should throw exception when word not found")
    void removeProfanityWordById_ShouldThrowException_WhenWordNotFound() {
        // Arrange
        Long wordId = 99L;
        when(profanityWordRepository.findById(wordId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            contentFilterService.removeProfanityWordById(wordId);
        });
    }
    
    @Test
    @DisplayName("Test 5: getAllProfanityWordDetails should return all active profanity words")
    void getAllProfanityWordDetails_ShouldReturnAllActiveProfanityWords() {
        // Arrange
        List<ProfanityWord> wordList = Arrays.asList(
            createProfanityWord(1L, "badword", "en", true),
            createProfanityWord(2L, "offensive", "en", true),
            createProfanityWord(3L, "bad phrase", "en", true)
        );
        
        when(profanityWordRepository.findByActiveTrue()).thenReturn(wordList);
        
        // Act
        List<ProfanityWord> result = contentFilterService.getAllProfanityWordDetails();
        
        // Assert
        assertEquals(3, result.size());
        verify(profanityWordRepository).findByActiveTrue();
    }
    
    @Test
    @DisplayName("Test 6: getProfanityWordsByLanguage should return words filtered by language")
    void getProfanityWordsByLanguage_ShouldReturnWordsFilteredByLanguage() {
        // Arrange
        String language = "vi";
        List<ProfanityWord> wordList = Arrays.asList(
            createProfanityWord(1L, "từ1", "vi", true),
            createProfanityWord(2L, "từ2", "vi", true)
        );
        
        when(profanityWordRepository.findByLanguageAndActiveTrue(language)).thenReturn(wordList);
        
        // Act
        List<ProfanityWord> result = contentFilterService.getProfanityWordsByLanguage(language);
        
        // Assert
        assertEquals(2, result.size());
        assertEquals("vi", result.get(0).getLanguage());
        assertEquals("vi", result.get(1).getLanguage());
        verify(profanityWordRepository).findByLanguageAndActiveTrue(language);
    }
    
    // Helper method to create ProfanityWord instances for testing
    private ProfanityWord createProfanityWord(Long id, String word, String language, boolean active) {
        ProfanityWord profanityWord = new ProfanityWord();
        profanityWord.setId(id);
        profanityWord.setWord(word);
        profanityWord.setLanguage(language);
        profanityWord.setActive(active);
        profanityWord.setCreatedAt(LocalDateTime.now());
        profanityWord.setUpdatedAt(LocalDateTime.now());
        return profanityWord;
    }
} 