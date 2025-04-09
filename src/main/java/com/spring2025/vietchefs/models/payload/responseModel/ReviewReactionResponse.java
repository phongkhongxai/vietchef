package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReactionResponse {
    private Long reactionId;
    private Long reviewId;
    private Long userId;
    private String userName;
    private String reactionType;
    private LocalDateTime createdAt;
} 