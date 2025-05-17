package com.spring2025.vietchefs.services;

import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service xử lý việc lọc nội dung không phù hợp từ các bài đánh giá
 */
@Service
public class ContentFilterService {
    
    private final Set<String> profanityWords = new HashSet<>();
    private static final String REPLACEMENT = "***";
    
    private final ResourceLoader resourceLoader;
    
    @Value("${app.profanity.custom-file-path:}")
    private String customFilePath;
    
    @Value("${app.profanity.vietnamese-file:classpath:/profanity/vietnamese_profanity.txt}")
    private String vietnameseProfanityPath;
    
    @Value("${app.profanity.english-file:classpath:/profanity/english_profanity.txt}")
    private String englishProfanityPath;
    
    /**
     * Constructor khởi tạo danh sách từ ngữ cần lọc từ file
     */
    public ContentFilterService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
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
            
            // Nạp từ tệp tùy chỉnh nếu có
            loadCustomProfanityWords();
            
        } catch (IOException e) {
            // Log the error
            System.err.println("Error loading profanity words: " + e.getMessage());
        }
    }
    
    /**
     * Phương thức tải danh sách từ ngữ tùy chỉnh (đã thêm hoặc xóa) từ file
     */
    private void loadCustomProfanityWords() {
        if (customFilePath == null || customFilePath.isEmpty()) {
            // Sử dụng thư mục data trong ứng dụng nếu không có đường dẫn tùy chỉnh
            customFilePath = "data/custom_profanity.txt";
        }
        
        File customFile = new File(customFilePath);
        if (customFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(customFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("-")) {
                        // Đây là từ đã bị xóa
                        profanityWords.remove(line.substring(1).trim().toLowerCase());
                    } else if (!line.isEmpty() && !line.startsWith("#")) {
                        // Đây là từ được thêm vào
                        profanityWords.add(line.toLowerCase());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading custom profanity words: " + e.getMessage());
            }
        }
    }
    
    /**
     * Lưu danh sách thay đổi vào file tùy chỉnh
     * 
     * @param addedWord Từ mới được thêm vào, null nếu không có
     * @param removedWord Từ được xóa, null nếu không có
     */
    private void saveProfanityChanges(String addedWord, String removedWord) {
        if (customFilePath == null || customFilePath.isEmpty()) {
            customFilePath = "data/custom_profanity.txt";
        }
        
        try {
            // Tạo thư mục nếu không tồn tại
            File parentDir = new File(customFilePath).getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            File customFile = new File(customFilePath);
            boolean fileExists = customFile.exists();
            
            try (FileWriter writer = new FileWriter(customFile, fileExists)) {
                if (!fileExists) {
                    writer.write("# Custom profanity words file\n");
                    writer.write("# Format: '+word' for added, '-word' for removed\n\n");
                }
                
                if (addedWord != null && !addedWord.isEmpty()) {
                    writer.write(addedWord.toLowerCase().trim() + "\n");
                }
                
                if (removedWord != null && !removedWord.isEmpty()) {
                    writer.write("-" + removedWord.toLowerCase().trim() + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving profanity words changes: " + e.getMessage());
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
            String trimmedWord = word.toLowerCase().trim();
            if (profanityWords.add(trimmedWord)) {
                // Chỉ lưu khi từ đó chưa có trong danh sách
                saveProfanityChanges(trimmedWord, null);
            }
        }
    }
    
    /**
     * Phương thức xóa từ khỏi danh sách từ ngữ cần lọc
     * 
     * @param word Từ cần xóa
     */
    public void removeProfanityWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            String trimmedWord = word.toLowerCase().trim();
            if (profanityWords.remove(trimmedWord)) {
                // Chỉ lưu khi từ đó có trong danh sách và đã xóa thành công
                saveProfanityChanges(null, trimmedWord);
            }
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