package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReply;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.models.payload.requestModel.ReviewReplyRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReplyResponse;
import com.spring2025.vietchefs.repositories.ReviewReplyRepository;
import com.spring2025.vietchefs.repositories.ReviewRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ReviewReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewReplyServiceImpl implements ReviewReplyService {

    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReviewReplyServiceImpl(
            ReviewReplyRepository reviewReplyRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository) {
        this.reviewReplyRepository = reviewReplyRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReviewReplyResponse addReply(Long reviewId, Long userId, ReviewReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        ReviewReply reply = new ReviewReply();
        reply.setReview(review);
        reply.setUser(user);
        reply.setContent(request.getContent());
        reply.setCreatedAt(LocalDateTime.now());
        reply.setIsDeleted(false);
        
        ReviewReply savedReply = reviewReplyRepository.save(reply);
        return mapToResponse(savedReply);
    }

    @Override
    @Transactional
    public ReviewReplyResponse updateReply(Long replyId, ReviewReplyRequest request) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));
        
        reply.setContent(request.getContent());
        ReviewReply updatedReply = reviewReplyRepository.save(reply);
        return mapToResponse(updatedReply);
    }

    @Override
    @Transactional
    public void deleteReply(Long replyId) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));
        
        reply.setIsDeleted(true);
        reviewReplyRepository.save(reply);
    }

    @Override
    public List<ReviewReplyResponse> getRepliesByReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        
        return reviewReplyRepository.findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(review)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewReplyResponse> getRepliesByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return reviewReplyRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private ReviewReplyResponse mapToResponse(ReviewReply reply) {
        return new ReviewReplyResponse(
                reply.getReplyId(),
                reply.getReview().getId(),
                reply.getUser().getId(),
                reply.getUser().getFullName(),
                reply.getUser().getAvatarUrl(),
                reply.getContent(),
                reply.getCreatedAt()
        );
    }
} 