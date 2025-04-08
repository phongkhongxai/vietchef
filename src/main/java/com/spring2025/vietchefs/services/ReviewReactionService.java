package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReaction;
import com.spring2025.vietchefs.models.entity.User;

import java.util.List;
import java.util.Map;

public interface ReviewReactionService {
    ReviewReaction addReaction(Review review, User user, String reactionType);
    ReviewReaction updateReaction(Long reactionId, String reactionType);
    void removeReaction(Long reactionId);
    List<ReviewReaction> getReactionsByReview(Review review);
    boolean hasUserReacted(Review review, User user);
    Map<String, Long> getReactionCountsByReview(Review review);
} 