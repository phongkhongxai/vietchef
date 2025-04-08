package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReply;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.ResourceNotFoundException;
import com.spring2025.vietchefs.repositories.ReviewReplyRepository;
import com.spring2025.vietchefs.services.ReviewReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewReplyServiceImpl implements ReviewReplyService {

    private final ReviewReplyRepository reviewReplyRepository;

    @Autowired
    public ReviewReplyServiceImpl(ReviewReplyRepository reviewReplyRepository) {
        this.reviewReplyRepository = reviewReplyRepository;
    }

    @Override
    @Transactional
    public ReviewReply addReply(Review review, User user, String content) {
        ReviewReply reply = new ReviewReply();
        reply.setReview(review);
        reply.setUser(user);
        reply.setContent(content);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setIsDeleted(false);
        
        return reviewReplyRepository.save(reply);
    }

    @Override
    @Transactional
    public ReviewReply updateReply(Long replyId, String content) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found with id: " + replyId));
        
        reply.setContent(content);
        return reviewReplyRepository.save(reply);
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
    public List<ReviewReply> getRepliesByReview(Review review) {
        return reviewReplyRepository.findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(review);
    }

    @Override
    public List<ReviewReply> getRepliesByUser(User user) {
        return reviewReplyRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user);
    }
} 