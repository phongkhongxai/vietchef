package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ReviewReplyRequest;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewReplyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewReplyService {
    ReviewReplyResponse addReply(Long reviewId, Long userId, ReviewReplyRequest request);
    ReviewReplyResponse updateReply(Long replyId, ReviewReplyRequest request);
    void deleteReply(Long replyId);
    List<ReviewReplyResponse> getRepliesByReview(Long reviewId);
    List<ReviewReplyResponse> getRepliesByUser(Long userId);
    Page<ReviewReplyResponse> getRepliesByReviewPaginated(Long reviewId, Pageable pageable);
} 