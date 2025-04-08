package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReply;
import com.spring2025.vietchefs.models.entity.User;

import java.util.List;

public interface ReviewReplyService {
    ReviewReply addReply(Review review, User user, String content);
    ReviewReply updateReply(Long replyId, String content);
    void deleteReply(Long replyId);
    List<ReviewReply> getRepliesByReview(Review review);
    List<ReviewReply> getRepliesByUser(User user);
} 