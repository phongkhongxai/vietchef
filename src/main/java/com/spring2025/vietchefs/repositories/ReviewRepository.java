package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Review;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByChefAndIsDeletedFalseOrderByCreateAtDesc(Chef chef);
    Page<Review> findByChefAndIsDeletedFalse(Chef chef, Pageable pageable);
    List<Review> findByUserAndIsDeletedFalseOrderByCreateAtDesc(User user);
    Optional<Review> findByBookingAndIsDeletedFalse(Booking booking);
    
    // New filter methods
    Page<Review> findByChefAndRatingBetweenAndCreateAtBetweenAndIsDeletedFalse(
            Chef chef, 
            BigDecimal minRating, 
            BigDecimal maxRating, 
            LocalDateTime fromDate, 
            LocalDateTime toDate, 
            Pageable pageable);
            
    Page<Review> findByChefAndRatingBetweenAndIsDeletedFalse(
            Chef chef, 
            BigDecimal minRating, 
            BigDecimal maxRating, 
            Pageable pageable);
            
    Page<Review> findByChefAndCreateAtBetweenAndIsDeletedFalse(
            Chef chef, 
            LocalDateTime fromDate, 
            LocalDateTime toDate, 
            Pageable pageable);
            
    Page<Review> findByChefAndRatingGreaterThanEqualAndIsDeletedFalse(
            Chef chef, 
            BigDecimal minRating, 
            Pageable pageable);
            
    Page<Review> findByChefAndRatingLessThanEqualAndIsDeletedFalse(
            Chef chef, 
            BigDecimal maxRating, 
            Pageable pageable);
            
    Page<Review> findByChefAndCreateAtGreaterThanEqualAndIsDeletedFalse(
            Chef chef, 
            LocalDateTime fromDate, 
            Pageable pageable);
            
    Page<Review> findByChefAndCreateAtLessThanEqualAndIsDeletedFalse(
            Chef chef, 
            LocalDateTime toDate, 
            Pageable pageable);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.chef = ?1 AND r.isDeleted = false")
    Optional<BigDecimal> findAverageRatingByChef(Chef chef);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.chef = ?1 AND r.isDeleted = false")
    long countByChef(Chef chef);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.chef = ?1 AND r.rating >= ?2 AND r.isDeleted = false")
    long countByChefAndRatingGreaterThanEqual(Chef chef, BigDecimal rating);
} 