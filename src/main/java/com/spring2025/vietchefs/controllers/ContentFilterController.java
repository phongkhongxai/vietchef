package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.services.ContentFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/content-filter")
public class ContentFilterController {

    private final ContentFilterService contentFilterService;

    @Autowired
    public ContentFilterController(ContentFilterService contentFilterService) {
        this.contentFilterService = contentFilterService;
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Lấy danh sách từ ngữ không phù hợp",
            description = "Chỉ Admin có quyền truy cập"
    )
    @GetMapping("/profanity-words")
    public ResponseEntity<Set<String>> getAllProfanityWords() {
        return ResponseEntity.ok(contentFilterService.getAllProfanityWords());
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Thêm từ mới vào danh sách từ ngữ không phù hợp",
            description = "Chỉ Admin có quyền truy cập"
    )
    @PostMapping("/profanity-words")
    public ResponseEntity<Map<String, String>> addProfanityWord(@Valid @RequestBody Map<String, String> request) {
        String word = request.get("word");
        if (word == null || word.trim().isEmpty()) {
            return new ResponseEntity<>(createErrorResponse("Từ không được để trống"), HttpStatus.BAD_REQUEST);
        }
        
        contentFilterService.addProfanityWord(word);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã thêm từ vào danh sách");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Xóa từ khỏi danh sách từ ngữ không phù hợp",
            description = "Chỉ Admin có quyền truy cập"
    )
    @DeleteMapping("/profanity-words/{word}")
    public ResponseEntity<Map<String, String>> removeProfanityWord(@PathVariable String word) {
        contentFilterService.removeProfanityWord(word);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã xóa từ khỏi danh sách");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Kiểm tra văn bản có chứa từ ngữ không phù hợp",
            description = "Chỉ Admin có quyền truy cập"
    )
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkContent(@Valid @RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Văn bản không được để trống");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
        
        boolean containsProfanity = contentFilterService.containsProfanity(text);
        String filteredText = contentFilterService.filterText(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("containsProfanity", containsProfanity);
        response.put("original", text);
        response.put("filtered", filteredText);
        response.put("profanityWords", contentFilterService.findProfanityWords(text));
        
        return ResponseEntity.ok(response);
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
} 