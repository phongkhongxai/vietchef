package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.ReviewCriteria;
import com.spring2025.vietchefs.models.entity.ReviewDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewDetailRepository extends JpaRepository<ReviewDetail, Long> {
    List<ReviewDetail> findByReviewId(Long reviewId);
    List<ReviewDetail> findByReview(Review review);
    Optional<ReviewDetail> findByReviewAndCriteria(Review review, ReviewCriteria criteria);
} 