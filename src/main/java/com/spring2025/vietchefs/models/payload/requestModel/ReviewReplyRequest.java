package com.spring2025.vietchefs.models.payload.requestModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyRequest {
    @NotBlank(message = "Reply content cannot be empty")
    @Size(min = 3, max = 1000, message = "Reply content must be between 3 and 1000 characters")
    private String content;
} 