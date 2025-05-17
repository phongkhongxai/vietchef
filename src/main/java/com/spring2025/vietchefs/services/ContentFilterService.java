package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.ProfanityWord;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ProfanityWordRequest;
import com.spring2025.vietchefs.repositories.ProfanityWordRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service xử lý việc lọc nội dung không phù hợp từ các bài đánh giá
 */
@Service
public class ContentFilterService {
    
    private final Set<String> profanityWords = new HashSet<>();
    private static final String REPLACEMENT = "***";
    
    private final ProfanityWordRepository profanityWordRepository;
    private final ResourceLoader resourceLoader;
    
    @Autowired
    public ContentFilterService(ProfanityWordRepository profanityWordRepository, ResourceLoader resourceLoader) {
        this.profanityWordRepository = profanityWordRepository;
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Khởi tạo danh sách từ ngữ từ database sau khi bean được tạo
     */
    @PostConstruct
    public void init() {
        loadProfanityWordsFromDb();
    }
    
    /**
     * Tải danh sách từ ngữ từ database vào bộ nhớ để tìm kiếm nhanh
     */
    private void loadProfanityWordsFromDb() {
        List<String> activeWords = profanityWordRepository.findAllActiveWords();
        profanityWords.clear();
        profanityWords.addAll(activeWords);
    }
    
    /**
     * Phương thức khởi tạo từ ngữ ban đầu từ file nếu database trống
     */
    @Transactional
    public void initializeProfanityWordsIfEmpty() {
        // Nếu đã có từ trong database, không cần tải lại
        if (profanityWordRepository.countActiveWords() > 0) {
            return;
        }
        
        try {
            // Đọc từ file chứa danh sách từ ngữ tiếng Việt cần lọc
            Resource viResource = resourceLoader.getResource("classpath:/profanity/vietnamese_profanity.txt");
            if (viResource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(viResource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            addWordToDatabase(line.toLowerCase(), "vi");
                        }
                    }
                }
            }
            
            // Đọc từ file chứa danh sách từ ngữ tiếng Anh cần lọc
            Resource enResource = resourceLoader.getResource("classpath:/profanity/english_profanity.txt");
            if (enResource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(enResource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            addWordToDatabase(line.toLowerCase(), "en");
                        }
                    }
                }
            }
            
            // Tải lại danh sách từ ngữ từ database sau khi đã thêm
            loadProfanityWordsFromDb();
            
        } catch (IOException e) {
            System.err.println("Error loading profanity words from files: " + e.getMessage());
        }
    }
    
    /**
     * Thêm từ vào database nếu chưa tồn tại
     */
    private void addWordToDatabase(String word, String language) {
        if (!profanityWordRepository.existsByWordIgnoreCase(word)) {
            ProfanityWord profanityWord = new ProfanityWord();
            profanityWord.setWord(word.toLowerCase().trim());
            profanityWord.setLanguage(language);
            profanityWord.setActive(true);
            profanityWord.setCreatedAt(LocalDateTime.now());
            profanityWord.setUpdatedAt(LocalDateTime.now());
            profanityWordRepository.save(profanityWord);
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
     * @param language Ngôn ngữ của từ (vi, en)
     */
    @Transactional
    public void addProfanityWord(String word, String language) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        
        String trimmedWord = word.toLowerCase().trim();
        
        // Kiểm tra từ đã tồn tại chưa
        ProfanityWord existingWord = profanityWordRepository.findByWordIgnoreCase(trimmedWord)
                .orElse(null);
                
        if (existingWord != null) {
            // Nếu từ đã tồn tại nhưng không active, kích hoạt lại
            if (!existingWord.isActive()) {
                existingWord.setActive(true);
                existingWord.setUpdatedAt(LocalDateTime.now());
                profanityWordRepository.save(existingWord);
                
                // Cập nhật cache trong bộ nhớ
                profanityWords.add(trimmedWord);
            }
        } else {
            // Nếu từ chưa tồn tại, thêm mới
            ProfanityWord newWord = new ProfanityWord();
            newWord.setWord(trimmedWord);
            newWord.setLanguage(language);
            newWord.setActive(true);
            newWord.setCreatedAt(LocalDateTime.now());
            newWord.setUpdatedAt(LocalDateTime.now());
            profanityWordRepository.save(newWord);
            
            // Cập nhật cache trong bộ nhớ
            profanityWords.add(trimmedWord);
        }
    }
    
    /**
     * Phương thức thêm từ mới vào danh sách từ ngữ cần lọc (ngôn ngữ mặc định là "other")
     * 
     * @param word Từ cần thêm
     */
    @Transactional
    public void addProfanityWord(String word) {
        addProfanityWord(word, "other");
    }
    
    /**
     * Phương thức xóa từ khỏi danh sách từ ngữ cần lọc
     * 
     * @param word Từ cần xóa
     */
    @Transactional
    public void removeProfanityWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }
        
        String trimmedWord = word.toLowerCase().trim();
        
        // Tìm từ trong database
        ProfanityWord existingWord = profanityWordRepository.findByWordIgnoreCase(trimmedWord)
                .orElse(null);
                
        if (existingWord != null && existingWord.isActive()) {
            // Soft delete bằng cách đặt active = false
            existingWord.setActive(false);
            existingWord.setUpdatedAt(LocalDateTime.now());
            profanityWordRepository.save(existingWord);
            
            // Cập nhật cache trong bộ nhớ
            profanityWords.remove(trimmedWord);
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
    
    /**
     * Lấy danh sách chi tiết các từ ngữ không phù hợp
     * 
     * @return Danh sách ProfanityWord
     */
    public List<ProfanityWord> getAllProfanityWordDetails() {
        return profanityWordRepository.findByActiveTrue();
    }
    
    /**
     * Lấy danh sách từ ngữ không phù hợp theo ngôn ngữ
     * 
     * @param language Ngôn ngữ cần lọc (vi, en, other)
     * @return Danh sách ProfanityWord
     */
    public List<ProfanityWord> getProfanityWordsByLanguage(String language) {
        return profanityWordRepository.findByLanguageAndActiveTrue(language);
    }
    
    /**
     * Cập nhật thông tin của từ ngữ cần lọc
     * 
     * @param id ID của từ cần cập nhật
     * @param request Thông tin cập nhật
     * @return Từ đã được cập nhật
     */
    @Transactional
    public ProfanityWord updateProfanityWord(Long id, ProfanityWordRequest request) {
        ProfanityWord existingWord = profanityWordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profanity word not found with ID: " + id));
        
        // Kiểm tra nếu đang cập nhật nội dung từ
        if (!existingWord.getWord().equalsIgnoreCase(request.getWord())) {
            // Kiểm tra xem từ mới đã tồn tại chưa
            if (profanityWordRepository.existsByWordIgnoreCase(request.getWord())) {
                throw new RuntimeException("Word '" + request.getWord() + "' already exists in the system");
            }
            
            // Cập nhật từ trong bộ nhớ cache
            profanityWords.remove(existingWord.getWord().toLowerCase());
            profanityWords.add(request.getWord().toLowerCase());
            
            // Cập nhật từ trong entity
            existingWord.setWord(request.getWord().toLowerCase().trim());
        }
        
        // Cập nhật các thông tin khác
        if (request.getLanguage() != null) {
            existingWord.setLanguage(request.getLanguage());
        }
        
        if (request.getActive() != null) {
            boolean wasActive = existingWord.isActive();
            existingWord.setActive(request.getActive());
            
            // Cập nhật cache trong bộ nhớ nếu trạng thái active thay đổi
            if (wasActive && !request.getActive()) {
                // Từ active -> không active: xóa khỏi cache
                profanityWords.remove(existingWord.getWord().toLowerCase());
            } else if (!wasActive && request.getActive()) {
                // Từ không active -> active: thêm vào cache
                profanityWords.add(existingWord.getWord().toLowerCase());
            }
        }
        
        existingWord.setUpdatedAt(LocalDateTime.now());
        return profanityWordRepository.save(existingWord);
    }
    
    /**
     * Xóa từ ngữ không phù hợp theo ID
     * 
     * @param id ID của từ cần xóa
     */
    @Transactional
    public void removeProfanityWordById(Long id) {
        ProfanityWord existingWord = profanityWordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profanity word not found with ID: " + id));
        
        if (existingWord.isActive()) {
            // Soft delete bằng cách đặt active = false
            existingWord.setActive(false);
            existingWord.setUpdatedAt(LocalDateTime.now());
            profanityWordRepository.save(existingWord);
            
            // Cập nhật cache trong bộ nhớ
            profanityWords.remove(existingWord.getWord().toLowerCase());
        }
    }
} 