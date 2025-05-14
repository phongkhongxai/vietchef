package com.spring2025.vietchefs.unit.services;

import com.spring2025.vietchefs.services.ContentFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContentFilterServiceTest {

    @InjectMocks
    private ContentFilterService contentFilterService;

    @BeforeEach
    void setUp() {
        // Initialize with test profanity words
        Set<String> testProfanityWords = new HashSet<>();
        testProfanityWords.add("badword");
        testProfanityWords.add("offensive");
        testProfanityWords.add("bad phrase");
        
        // Use reflection to set the private field
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
        
        // Act
        contentFilterService.addProfanityWord(newProfanityWord);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertTrue(allWords.contains(newProfanityWord));
        assertTrue(contentFilterService.containsProfanity("This text contains newbadword"));
    }
    
    @Test
    @DisplayName("Test 2: addProfanityWord should convert word to lowercase")
    void addProfanityWord_ShouldConvertWordToLowercase() {
        // Arrange
        String mixedCaseWord = "BadWord123";
        
        // Act
        contentFilterService.addProfanityWord(mixedCaseWord);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertTrue(allWords.contains("badword123"));
        assertFalse(allWords.contains("BadWord123"));
        assertTrue(contentFilterService.containsProfanity("This contains BadWord123"));
    }
    
    @Test
    @DisplayName("Test 3: addProfanityWord should trim whitespace")
    void addProfanityWord_ShouldTrimWhitespace() {
        // Arrange
        String wordWithWhitespace = "  inappropriate  ";
        
        // Act
        contentFilterService.addProfanityWord(wordWithWhitespace);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertTrue(allWords.contains("inappropriate"));
        assertFalse(allWords.contains("  inappropriate  "));
        assertTrue(contentFilterService.containsProfanity("This contains inappropriate language"));
    }
    
    @Test
    @DisplayName("Test 4: addProfanityWord should handle null or empty input")
    void addProfanityWord_ShouldHandleNullOrEmptyInput() {
        // Arrange
        int initialSize = contentFilterService.getAllProfanityWords().size();
        
        // Act
        contentFilterService.addProfanityWord(null);
        contentFilterService.addProfanityWord("");
        contentFilterService.addProfanityWord("  ");
        
        // Assert
        assertEquals(initialSize, contentFilterService.getAllProfanityWords().size());
    }
    
    // ==================== removeProfanityWord Tests ====================
    
    @Test
    @DisplayName("Test 1: removeProfanityWord should remove existing word from profanity list")
    void removeProfanityWord_ShouldRemoveExistingWordFromProfanityList() {
        // Arrange
        String wordToRemove = "badword";
        
        // Act
        contentFilterService.removeProfanityWord(wordToRemove);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertFalse(allWords.contains(wordToRemove));
        assertFalse(contentFilterService.containsProfanity("This text contains badword"));
    }
    
    @Test
    @DisplayName("Test 2: removeProfanityWord should handle case-insensitive removal")
    void removeProfanityWord_ShouldHandleCaseInsensitiveRemoval() {
        // Arrange
        String existingWord = "badword";
        String mixedCaseWord = "BaDwOrD";
        
        // Act
        contentFilterService.removeProfanityWord(mixedCaseWord);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertFalse(allWords.contains(existingWord));
        assertFalse(contentFilterService.containsProfanity("This text contains badword"));
    }
    
    @Test
    @DisplayName("Test 3: removeProfanityWord should handle non-existent words")
    void removeProfanityWord_ShouldHandleNonExistentWords() {
        // Arrange
        int initialSize = contentFilterService.getAllProfanityWords().size();
        String nonExistentWord = "nonexistentword";
        
        // Act
        contentFilterService.removeProfanityWord(nonExistentWord);
        
        // Assert
        assertEquals(initialSize, contentFilterService.getAllProfanityWords().size());
    }
    
    @Test
    @DisplayName("Test 4: removeProfanityWord should handle null or empty input")
    void removeProfanityWord_ShouldHandleNullOrEmptyInput() {
        // Arrange
        int initialSize = contentFilterService.getAllProfanityWords().size();
        
        // Act
        contentFilterService.removeProfanityWord(null);
        contentFilterService.removeProfanityWord("");
        contentFilterService.removeProfanityWord("  ");
        
        // Assert
        assertEquals(initialSize, contentFilterService.getAllProfanityWords().size());
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
    
    @Test
    @DisplayName("Test 3: getAllProfanityWords should reflect added words")
    void getAllProfanityWords_ShouldReflectAddedWords() {
        // Arrange
        int initialSize = contentFilterService.getAllProfanityWords().size();
        contentFilterService.addProfanityWord("newbadword");
        
        // Act
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        
        // Assert
        assertEquals(initialSize + 1, allWords.size());
        assertTrue(allWords.contains("newbadword"));
    }
    
    @Test
    @DisplayName("Test 4: getAllProfanityWords should reflect removed words")
    void getAllProfanityWords_ShouldReflectRemovedWords() {
        // Arrange
        int initialSize = contentFilterService.getAllProfanityWords().size();
        contentFilterService.removeProfanityWord("badword");
        
        // Act
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        
        // Assert
        assertEquals(initialSize - 1, allWords.size());
        assertFalse(allWords.contains("badword"));
    }
} 