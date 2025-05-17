package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfanityWordRequest {
    
    @NotBlank(message = "Word cannot be empty")
    private String word;
    
    @Pattern(regexp = "^(vi|en)$", message = "Language must be 'vi' or 'en'")
    private String language = "en"; // Default is "en" if not specified
    
    private Boolean active; // Used only for updates, can be null (no change)
} 