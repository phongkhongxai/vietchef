package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a review given by a user to a chef after booking
 */
@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chef_id", nullable = false)
    private Chef chef;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(nullable = false)
    private BigDecimal rating;

    /**
     * The complete review text content written by the user.
     * Contains the user's full feedback about their experience with the chef,
     * including details about food quality, service, professionalism, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String overallExperience;
    
    private String imageUrl;
    
    private String response;
    
    private LocalDateTime chefResponseAt;

    @Column(nullable = false)
    private LocalDateTime createAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;
    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewDetail> reviewDetails = new ArrayList<>();
    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewReply> replies = new ArrayList<>();
    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewReaction> reactions = new ArrayList<>();
}