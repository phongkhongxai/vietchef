package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReaction;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.repositories.ReviewReactionRepository;
import com.spring2025.vietchefs.services.ReviewReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReviewReactionServiceImpl implements ReviewReactionService {

    private final ReviewReactionRepository reviewReactionRepository;

    @Autowired
    public ReviewReactionServiceImpl(ReviewReactionRepository reviewReactionRepository) {
        this.reviewReactionRepository = reviewReactionRepository;
    }

    @Override
    @Transactional
    public ReviewReaction addReaction(Review review, User user, String reactionType) {
        // Check if user already reacted to this review
        Optional<ReviewReaction> existingReaction = reviewReactionRepository.findByReviewAndUser(review, user);
        
        if (existingReaction.isPresent()) {
            // Update existing reaction
            ReviewReaction reaction = existingReaction.get();
            reaction.setReactionType(reactionType);
            return reviewReactionRepository.save(reaction);
        } else {
            // Create new reaction
            ReviewReaction reaction = new ReviewReaction();
            reaction.setReview(review);
            reaction.setUser(user);
            reaction.setReactionType(reactionType);
            reaction.setCreatedAt(LocalDateTime.now());
            
            return reviewReactionRepository.save(reaction);
        }
    }

    @Override
    @Transactional
    public ReviewReaction updateReaction(Long reactionId, String reactionType) {
        ReviewReaction reaction = reviewReactionRepository.findById(reactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reaction not found with id: " + reactionId));
        
        reaction.setReactionType(reactionType);
        return reviewReactionRepository.save(reaction);
    }

    @Override
    @Transactional
    public void removeReaction(Long reactionId) {
        if (!reviewReactionRepository.existsById(reactionId)) {
            throw new ResourceNotFoundException("Reaction not found with id: " + reactionId);
        }
        
        reviewReactionRepository.deleteById(reactionId);
    }

    @Override
    public List<ReviewReaction> getReactionsByReview(Review review) {
        return reviewReactionRepository.findByReview(review);
    }

    @Override
    public boolean hasUserReacted(Review review, User user) {
        return reviewReactionRepository.findByReviewAndUser(review, user).isPresent();
    }

    @Override
    public Map<String, Long> getReactionCountsByReview(Review review) {
        Map<String, Long> counts = new HashMap<>();
        
        long helpfulCount = reviewReactionRepository.countByReviewAndReactionType(review, "helpful");
        long notHelpfulCount = reviewReactionRepository.countByReviewAndReactionType(review, "not_helpful");
        
        counts.put("helpful", helpfulCount);
        counts.put("not_helpful", notHelpfulCount);
        
        return counts;
    }
} 