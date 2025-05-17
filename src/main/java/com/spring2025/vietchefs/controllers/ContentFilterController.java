package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.entity.ProfanityWord;
import com.spring2025.vietchefs.models.payload.requestModel.ProfanityWordRequest;
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
import java.util.List;
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
            summary = "Get all profanity words",
            description = "Admin access only"
    )
    @GetMapping("/profanity-words")
    public ResponseEntity<Set<String>> getAllProfanityWords() {
        return ResponseEntity.ok(contentFilterService.getAllProfanityWords());
    }
    
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Get detailed list of profanity words",
            description = "Admin access only"
    )
    @GetMapping("/profanity-words/details")
    public ResponseEntity<List<ProfanityWord>> getAllProfanityWordDetails() {
        return ResponseEntity.ok(contentFilterService.getAllProfanityWordDetails());
    }
    
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Get profanity words by language",
            description = "Admin access only"
    )
    @GetMapping("/profanity-words/{language}")
    public ResponseEntity<List<ProfanityWord>> getProfanityWordsByLanguage(@PathVariable String language) {
        return ResponseEntity.ok(contentFilterService.getProfanityWordsByLanguage(language));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Add new word to profanity list",
            description = "Admin access only"
    )
    @PostMapping("/profanity-words")
    public ResponseEntity<Map<String, String>> addProfanityWord(@Valid @RequestBody ProfanityWordRequest request) {
        contentFilterService.addProfanityWord(request.getWord(), request.getLanguage());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Word added to profanity list");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Update word in profanity list",
            description = "Admin access only"
    )
    @PutMapping("/profanity-words/{id}")
    public ResponseEntity<Map<String, String>> updateProfanityWord(
            @PathVariable Long id, 
            @Valid @RequestBody ProfanityWordRequest request) {
        
        contentFilterService.updateProfanityWord(id, request);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Word updated in profanity list");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Remove word from profanity list",
            description = "Admin access only"
    )
    @DeleteMapping("/profanity-words/{word}")
    public ResponseEntity<Map<String, String>> removeProfanityWord(@PathVariable String word) {
        contentFilterService.removeProfanityWord(word);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Word removed from profanity list");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Remove word from profanity list by ID",
            description = "Admin access only"
    )
    @DeleteMapping("/profanity-words/id/{id}")
    public ResponseEntity<Map<String, String>> removeProfanityWordById(@PathVariable Long id) {
        contentFilterService.removeProfanityWordById(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Word removed from profanity list");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Check if text contains profanity",
            description = "Admin access only"
    )
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkContent(@Valid @RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Text cannot be empty");
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