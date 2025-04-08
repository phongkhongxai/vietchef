package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ReviewReactionRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReactionResponse;

import java.util.List;
import java.util.Map;

public interface ReviewReactionService {
    ReviewReactionResponse addReaction(Long reviewId, Long userId, ReviewReactionRequest request);
    ReviewReactionResponse updateReaction(Long reactionId, ReviewReactionRequest request);
    void removeReaction(Long reactionId);
    List<ReviewReactionResponse> getReactionsByReview(Long reviewId);
    boolean hasUserReacted(Long reviewId, Long userId);
    Map<String, Long> getReactionCountsByReview(Long reviewId);
} 