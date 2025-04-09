package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReply;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
    List<ReviewReply> findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(Review review);
    List<ReviewReply> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user);
} 