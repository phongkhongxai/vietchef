package com.spring2025.vietchefs.unit.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.spring2025.vietchefs.services.ContentFilterService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ContentFilterServiceTest {

    @Spy
    @InjectMocks
    private ContentFilterService contentFilterService;

    private Set<String> testProfanityWords;

    @BeforeEach
    void setUp() {
        testProfanityWords = new HashSet<>();
        testProfanityWords.add("badword");
        testProfanityWords.add("bad phrase");
        testProfanityWords.add("offensive");
        
        // Inject test profanity words directly using reflection
        ReflectionTestUtils.setField(contentFilterService, "profanityWords", testProfanityWords);
    }

    @Test
    void containsProfanity_ShouldReturnTrue_WhenTextContainsProfanity() {
        // Arrange
        String textWithProfanity1 = "This text contains badword that should be detected";
        String textWithProfanity2 = "This text contains bad phrase that should be detected";
        String textWithProfanity3 = "This text has badphrase without spaces";
        
        // Act
        boolean result1 = contentFilterService.containsProfanity(textWithProfanity1);
        boolean result2 = contentFilterService.containsProfanity(textWithProfanity2);
        boolean result3 = contentFilterService.containsProfanity(textWithProfanity3);
        
        // Assert
        assertTrue(result1, "Should detect 'badword'");
        assertTrue(result2, "Should detect 'bad phrase'");
        assertTrue(result3, "Should detect 'badphrase' as a no-space version of 'bad phrase'");
    }
    
    @Test
    void containsProfanity_ShouldReturnFalse_WhenTextIsClean() {
        // Arrange
        String cleanText = "This is a clean text without any inappropriate content";
        
        // Act
        boolean result = contentFilterService.containsProfanity(cleanText);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void containsProfanity_ShouldReturnFalse_WhenTextIsNull() {
        // Act
        boolean result = contentFilterService.containsProfanity(null);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void containsProfanity_ShouldReturnFalse_WhenTextIsEmpty() {
        // Arrange
        String emptyText = "";
        String whitespaceText = "   ";
        
        // Act
        boolean result1 = contentFilterService.containsProfanity(emptyText);
        boolean result2 = contentFilterService.containsProfanity(whitespaceText);
        
        // Assert
        assertFalse(result1);
        assertFalse(result2);
    }
    
    @Test
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
    void filterText_ShouldReplaceNoSpaceProfanityPhrases() {
        // Arrange
        String textWithNoSpacePhrases = "This text contains badphrase that should be filtered";
        
        // Act
        String filteredText = contentFilterService.filterText(textWithNoSpacePhrases);
        
        // Assert
        assertFalse(filteredText.contains("badphrase"));
        assertTrue(filteredText.contains("***"));
        assertEquals("This text contains *** that should be filtered", filteredText);
    }
    
    @Test
    void filterText_ShouldReturnOriginalText_WhenNoFilteringNeeded() {
        // Arrange
        String cleanText = "This is a clean text";
        
        // Act
        String filteredText = contentFilterService.filterText(cleanText);
        
        // Assert
        assertEquals(cleanText, filteredText);
    }
    
    @Test
    void filterText_ShouldReturnNull_WhenInputIsNull() {
        // Act
        String filteredText = contentFilterService.filterText(null);
        
        // Assert
        assertNull(filteredText);
    }
    
    @Test
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
    void findProfanityWords_ShouldReturnEmptyList_WhenNoMatch() {
        // Arrange
        String cleanText = "This is a clean text";
        
        // Act
        List<String> foundWords = contentFilterService.findProfanityWords(cleanText);
        
        // Assert
        assertTrue(foundWords.isEmpty());
    }
    
    @Test
    void findProfanityWords_ShouldDetectNoSpaceProfanity() {
        // Arrange
        String textWithNoSpaceProfanity = "This text has badphrase without spaces";
        
        // Act
        List<String> foundWords = contentFilterService.findProfanityWords(textWithNoSpaceProfanity);
        
        // Assert
        assertEquals(1, foundWords.size());
        assertTrue(foundWords.contains("bad phrase"), "Should detect 'bad phrase' from 'badphrase'");
    }
    
    @Test
    void addProfanityWord_ShouldAddWordToList() {
        // Arrange
        String newBadWord = "newbadword";
        int initialSize = contentFilterService.getAllProfanityWords().size();
        
        // Act
        contentFilterService.addProfanityWord(newBadWord);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertEquals(initialSize + 1, allWords.size());
        assertTrue(allWords.contains(newBadWord));
    }
    
    @Test
    void addProfanityWord_ShouldTrimAndLowercaseWord() {
        // Arrange
        String wordWithSpaces = "  BAD word  ";
        
        // Act
        contentFilterService.addProfanityWord(wordWithSpaces);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertTrue(allWords.contains("bad word"));
        assertFalse(allWords.contains(wordWithSpaces));
    }
    
    @Test
    void removeProfanityWord_ShouldRemoveWordFromList() {
        // Arrange
        String wordToRemove = "badword";
        int initialSize = contentFilterService.getAllProfanityWords().size();
        
        // Act
        contentFilterService.removeProfanityWord(wordToRemove);
        
        // Assert
        Set<String> allWords = contentFilterService.getAllProfanityWords();
        assertEquals(initialSize - 1, allWords.size());
        assertFalse(allWords.contains(wordToRemove));
    }
    
    @Test
    void getAllProfanityWords_ShouldReturnCopyOfInternalSet() {
        // Act
        Set<String> wordSet1 = contentFilterService.getAllProfanityWords();
        Set<String> wordSet2 = contentFilterService.getAllProfanityWords();
        
        // Assert
        assertNotSame(wordSet1, wordSet2);
        assertEquals(wordSet1, wordSet2);
    }
} 