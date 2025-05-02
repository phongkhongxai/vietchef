package com.spring2025.vietchefs.services;

import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý việc lọc nội dung không phù hợp từ các bài đánh giá
 */
@Service
public class ContentFilterService {
    
    private final Set<String> profanityWords = new HashSet<>();
    private static final String REPLACEMENT = "***";
    
    /**
     * Constructor khởi tạo danh sách từ ngữ cần lọc từ file
     */
    public ContentFilterService() {
        loadProfanityWords();
    }
    
    /**
     * Phương thức tải danh sách từ ngữ cần lọc từ file resource
     */
    private void loadProfanityWords() {
        try {
            // Đọc từ file chứa danh sách từ ngữ cần lọc
            InputStream is = getClass().getResourceAsStream("/profanity/vietnamese_profanity.txt");
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            profanityWords.add(line.toLowerCase());
                        }
                    }
                }
            }
            
            // Thêm từ tiếng Anh nếu cần
            InputStream isEn = getClass().getResourceAsStream("/profanity/english_profanity.txt");
            if (isEn != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(isEn, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            profanityWords.add(line.toLowerCase());
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Log the error
            System.err.println("Error loading profanity words: " + e.getMessage());
        }
    }
    
    /**
     * Kiểm tra một văn bản có chứa từ ngữ không phù hợp hay không
     * 
     * @param text Văn bản cần kiểm tra
     * @return true nếu văn bản chứa từ ngữ không phù hợp
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        
        // Kiểm tra từng từ trong danh sách
        for (String word : profanityWords) {
            // Tạo pattern để tìm từ nguyên bản
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(lowerText);
            if (matcher.find()) {
                return true;
            }
            
            // Nếu từ có khoảng trắng, kiểm tra cả phiên bản không có khoảng trắng
            if (word.contains(" ")) {
                String noSpaceWord = word.replaceAll("\\s+", "");
                // Kiểm tra nếu noSpaceWord có trong văn bản
                Pattern noSpacePattern = Pattern.compile("\\b" + Pattern.quote(noSpaceWord) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher noSpaceMatcher = noSpacePattern.matcher(lowerText);
                if (noSpaceMatcher.find()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Lọc các từ ngữ không phù hợp trong văn bản
     * 
     * @param text Văn bản cần lọc
     * @return Văn bản đã được lọc
     */
    public String filterText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String filteredText = text;
        
        for (String word : profanityWords) {
            // Xử lý từ thường
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(filteredText);
            filteredText = matcher.replaceAll(REPLACEMENT);
            
            // Xử lý từ viết liền không có khoảng trắng
            if (word.contains(" ")) {
                String noSpaceWord = word.replaceAll("\\s+", "");
                Pattern noSpacePattern = Pattern.compile("\\b" + Pattern.quote(noSpaceWord) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher noSpaceMatcher = noSpacePattern.matcher(filteredText);
                filteredText = noSpaceMatcher.replaceAll(REPLACEMENT);
            }
        }
        
        return filteredText;
    }
    
    /**
     * Trả về danh sách các từ ngữ không phù hợp được tìm thấy trong văn bản
     * 
     * @param text Văn bản cần kiểm tra
     * @return Danh sách các từ ngữ không phù hợp
     */
    public List<String> findProfanityWords(String text) {
        List<String> foundWords = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return foundWords;
        }
        
        String lowerText = text.toLowerCase();
        
        for (String word : profanityWords) {
            boolean isFound = false;
            
            // Kiểm tra từ nguyên bản
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(lowerText);
            if (matcher.find()) {
                foundWords.add(word);
                isFound = true;
            }
            
            // Kiểm tra từ viết liền không có khoảng trắng nếu chưa tìm thấy
            if (!isFound && word.contains(" ")) {
                String noSpaceWord = word.replaceAll("\\s+", "");
                Pattern noSpacePattern = Pattern.compile("\\b" + Pattern.quote(noSpaceWord) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher noSpaceMatcher = noSpacePattern.matcher(lowerText);
                if (noSpaceMatcher.find()) {
                    foundWords.add(word);
                }
            }
        }
        
        return foundWords;
    }
    
    /**
     * Phương thức thêm từ mới vào danh sách từ ngữ cần lọc
     * 
     * @param word Từ cần thêm
     */
    public void addProfanityWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            profanityWords.add(word.toLowerCase().trim());
        }
    }
    
    /**
     * Phương thức xóa từ khỏi danh sách từ ngữ cần lọc
     * 
     * @param word Từ cần xóa
     */
    public void removeProfanityWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            profanityWords.remove(word.toLowerCase().trim());
        }
    }
    
    /**
     * Lấy tất cả các từ ngữ cần lọc
     * 
     * @return Danh sách các từ
     */
    public Set<String> getAllProfanityWords() {
        return new HashSet<>(profanityWords);
    }
} 