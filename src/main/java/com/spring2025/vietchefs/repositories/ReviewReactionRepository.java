package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewReaction;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, Long> {
    List<ReviewReaction> findByReview(Review review);
    List<ReviewReaction> findByReviewAndReactionType(Review review, String reactionType);
    Optional<ReviewReaction> findByReviewAndUser(Review review, User user);
    
    @Query("SELECT COUNT(r) FROM ReviewReaction r WHERE r.review = ?1 AND r.reactionType = ?2")
    long countByReviewAndReactionType(Review review, String reactionType);
} 