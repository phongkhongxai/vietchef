package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReaction;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReactionRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReactionResponse;
import com.spring2025.vietchefs.repositories.ReviewReactionRepository;
import com.spring2025.vietchefs.repositories.ReviewRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ReviewReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewReactionServiceImpl implements ReviewReactionService {

    private final ReviewReactionRepository reviewReactionRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReviewReactionServiceImpl(
            ReviewReactionRepository reviewReactionRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository) {
        this.reviewReactionRepository = reviewReactionRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReviewReactionResponse addReaction(Long reviewId, Long userId, ReviewReactionRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Check if user already reacted to this review
        Optional<ReviewReaction> existingReaction = reviewReactionRepository.findByReviewAndUser(review, user);
        
        ReviewReaction reaction;
        if (existingReaction.isPresent()) {
            // Update existing reaction
            reaction = existingReaction.get();
            reaction.setReactionType(request.getReactionType());
        } else {
            // Create new reaction
            reaction = new ReviewReaction();
            reaction.setReview(review);
            reaction.setUser(user);
            reaction.setReactionType(request.getReactionType());
            reaction.setCreatedAt(LocalDateTime.now());
        }
        
        return mapToResponse(reviewReactionRepository.save(reaction));
    }

    @Override
    @Transactional
    public ReviewReactionResponse updateReaction(Long reactionId, ReviewReactionRequest request) {
        ReviewReaction reaction = reviewReactionRepository.findById(reactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reaction not found with id: " + reactionId));
        
        reaction.setReactionType(request.getReactionType());
        return mapToResponse(reviewReactionRepository.save(reaction));
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
    public List<ReviewReactionResponse> getReactionsByReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        return reviewReactionRepository.findByReview(review).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasUserReacted(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return reviewReactionRepository.findByReviewAndUser(review, user).isPresent();
    }

    @Override
    public Map<String, Long> getReactionCountsByReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        Map<String, Long> counts = new HashMap<>();
        
        long helpfulCount = reviewReactionRepository.countByReviewAndReactionType(review, "helpful");
        long notHelpfulCount = reviewReactionRepository.countByReviewAndReactionType(review, "not_helpful");
        
        counts.put("helpful", helpfulCount);
        counts.put("not_helpful", notHelpfulCount);
        
        return counts;
    }
    
    private ReviewReactionResponse mapToResponse(ReviewReaction reaction) {
        return new ReviewReactionResponse(
                reaction.getReactionId(),
                reaction.getReview().getId(),
                reaction.getUser().getId(),
                reaction.getUser().getFullName(),
                reaction.getReactionType(),
                reaction.getCreatedAt()
        );
    }
} 