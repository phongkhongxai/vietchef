# Review System Diagrams

## Introduction
This document provides comprehensive diagrams representing the architecture and flow of the Review System in the VietChef application. These diagrams aim to provide a clear understanding of the system's structure and behavior.

## Class Diagram

```mermaid
classDiagram
    class Review {
        -Long id
        -User user
        -Chef chef
        -Booking booking
        -BigDecimal rating
        -String description
        -String overallExperience
        -String imageUrl
        -String response
        -LocalDateTime chefResponseAt
        -LocalDateTime createAt
        -Boolean isDeleted
        +List~ReviewDetail~ reviewDetails
        +List~ReviewReply~ replies
        +List~ReviewReaction~ reactions
    }

    class ReviewDetail {
        -Long detailId
        -BigDecimal rating
        -Review review
        -ReviewCriteria criteria
    }

    class ReviewCriteria {
        -Long criteriaId
        -String name
        -String description
        -BigDecimal weight
        -Boolean isActive
        -Integer displayOrder
        +List~ReviewDetail~ reviewDetails
    }

    class ReviewReply {
        -Long replyId
        -String content
        -LocalDateTime createdAt
        -Boolean isDeleted
        -Review review
        -User user
    }

    class ReviewReaction {
        -Long reactionId
        -String reactionType
        -LocalDateTime createdAt
        -Review review
        -User user
    }

    class ReviewRepository {
        +findByChefAndIsDeletedFalseOrderByCreateAtDesc(Chef chef)
        +findByChefAndIsDeletedFalse(Chef chef, Pageable pageable)
        +findByUserAndIsDeletedFalseOrderByCreateAtDesc(User user)
        +findByBookingAndIsDeletedFalse(Booking booking)
        +findAverageRatingByChef(Chef chef)
        +countByChef(Chef chef)
        +countByChefAndRatingGreaterThanEqual(Chef chef, BigDecimal rating)
    }

    class ReviewDetailRepository {
        +findByReviewId(Long reviewId)
        +findByReview(Review review)
        +findByReviewAndCriteria(Review review, ReviewCriteria criteria)
    }

    class ReviewCriteriaRepository {
        +findByIsActiveTrue()
        +findByIsActiveTrueOrderByDisplayOrderAsc()
        +findByName(String name)
    }

    class ReviewReplyRepository {
        +findByReviewAndIsDeletedFalseOrderByCreatedAtDesc(Review review)
        +findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user)
    }

    class ReviewReactionRepository {
        +findByReview(Review review)
        +findByReviewAndReactionType(Review review, String reactionType)
        +findByReviewAndUser(Review review, User user)
        +countByReviewAndReactionType(Review review, String reactionType)
    }

    class ReviewController {
        -ReviewService reviewService
        -ReviewCriteriaService reviewCriteriaService
        -ReviewReplyService reviewReplyService
        -ReviewReactionService reviewReactionService
        +createReview(ReviewCreateRequest request, Long userId)
        +updateReview(Long reviewId, ReviewUpdateRequest request, Long userId)
        +deleteReview(Long reviewId)
        +getReviewById(Long reviewId)
        +getReviewsByUserId(Long userId)
        +getReviewsByChefId(Long chefId, Pageable pageable)
        +addChefResponse(Long reviewId, String response, Long chefId)
        +getAverageRatingForChef(Long chefId)
        +getReviewCountForChef(Long chefId)
        +getRatingDistributionForChef(Long chefId)
    }

    class ReviewService {
        <<interface>>
        +getReviewById(Long id)
        +getReviewsByChef(Long chefId)
        +getReviewsByChef(Long chefId, Pageable pageable)
        +getReviewsByUser(Long userId)
        +getReviewByBooking(Long bookingId)
        +createReview(ReviewCreateRequest request, Long userId)
        +updateReview(Long id, ReviewUpdateRequest request, Long userId)
        +deleteReview(Long id)
        +addChefResponse(Long reviewId, String response, Long chefId)
        +calculateWeightedRating(Map~Long, BigDecimal~ criteriaRatings)
        +getAverageRatingForChef(Long chefId)
        +getReviewCountForChef(Long chefId)
        +getRatingDistributionForChef(Long chefId)
    }

    class ReviewServiceImpl {
        -ReviewRepository reviewRepository
        -ReviewDetailRepository reviewDetailRepository
        -ReviewCriteriaService reviewCriteriaService
        -ReviewReactionService reviewReactionService
        -BookingRepository bookingRepository
        -UserRepository userRepository
        -ChefRepository chefRepository
        -ImageRepository imageRepository
        -ImageService imageService
        +getReviewById(Long id)
        +getReviewsByChef(Long chefId)
        +getReviewsByChef(Long chefId, Pageable pageable)
        +getReviewsByUser(Long userId)
        +getReviewByBooking(Long bookingId)
        +createReview(ReviewCreateRequest request, Long userId)
        +updateReview(Long id, ReviewUpdateRequest request, Long userId)
        +deleteReview(Long id)
        +addChefResponse(Long reviewId, String response, Long chefId)
        +calculateWeightedRating(Map~Long, BigDecimal~ criteriaRatings)
        +getAverageRatingForChef(Long chefId)
        +getReviewCountForChef(Long chefId)
        +getRatingDistributionForChef(Long chefId)
        -mapToResponse(Review review)
        -mapToCriteriaEntity(ReviewCriteriaResponse response)
    }

    class ReviewCriteriaService {
        <<interface>>
        +getAllCriteria()
        +getActiveCriteria()
        +getCriteriaById(Long id)
        +createCriteria(ReviewCriteriaRequest request)
        +updateCriteria(Long id, ReviewCriteriaRequest request)
        +deleteCriteria(Long id)
        +initDefaultCriteria()
    }

    class ReviewCriteriaServiceImpl {
        -ReviewCriteriaRepository criteriaRepository
        +getAllCriteria()
        +getActiveCriteria()
        +getCriteriaById(Long id)
        +createCriteria(ReviewCriteriaRequest request)
        +updateCriteria(Long id, ReviewCriteriaRequest request)
        +deleteCriteria(Long id)
        +initDefaultCriteria()
        -createDefaultCriterion(String name, String description, BigDecimal weight, int displayOrder)
        -mapToResponse(ReviewCriteria criteria)
        -mapToEntity(ReviewCriteriaRequest request, ReviewCriteria criteria)
    }

    class ReviewReplyService {
        <<interface>>
        +addReply(Long reviewId, Long userId, ReviewReplyRequest request)
        +updateReply(Long replyId, ReviewReplyRequest request)
        +deleteReply(Long replyId)
        +getRepliesByReview(Long reviewId)
        +getRepliesByUser(Long userId)
    }

    class ReviewReplyServiceImpl {
        -ReviewReplyRepository reviewReplyRepository
        -ReviewRepository reviewRepository
        -UserRepository userRepository
        +addReply(Long reviewId, Long userId, ReviewReplyRequest request)
        +updateReply(Long replyId, ReviewReplyRequest request)
        +deleteReply(Long replyId)
        +getRepliesByReview(Long reviewId)
        +getRepliesByUser(Long userId)
        -mapToResponse(ReviewReply reply)
    }

    class ReviewReactionService {
        <<interface>>
        +addReaction(Long reviewId, Long userId, ReviewReactionRequest request)
        +updateReaction(Long reactionId, ReviewReactionRequest request)
        +removeReaction(Long reactionId)
        +getReactionsByReview(Long reviewId)
        +hasUserReacted(Long reviewId, Long userId)
        +getReactionCountsByReview(Long reviewId)
    }

    class ReviewReactionServiceImpl {
        -ReviewReactionRepository reviewReactionRepository
        -ReviewRepository reviewRepository
        -UserRepository userRepository
        +addReaction(Long reviewId, Long userId, ReviewReactionRequest request)
        +updateReaction(Long reactionId, ReviewReactionRequest request)
        +removeReaction(Long reactionId)
        +getReactionsByReview(Long reviewId)
        +hasUserReacted(Long reviewId, Long userId)
        +getReactionCountsByReview(Long reviewId)
        -mapToResponse(ReviewReaction reaction)
    }

    Review "1" --o "*" ReviewDetail : contains
    Review "1" --o "*" ReviewReply : has
    Review "1" --o "*" ReviewReaction : has
    ReviewDetail "*" --o "1" ReviewCriteria : uses
    
    ReviewController --> ReviewService : uses
    ReviewController --> ReviewCriteriaService : uses
    ReviewController --> ReviewReplyService : uses
    ReviewController --> ReviewReactionService : uses
    ReviewService <|.. ReviewServiceImpl : implements
    ReviewCriteriaService <|.. ReviewCriteriaServiceImpl : implements
    ReviewReplyService <|.. ReviewReplyServiceImpl : implements
    ReviewReactionService <|.. ReviewReactionServiceImpl : implements
    
    ReviewServiceImpl --> ReviewRepository : uses
    ReviewServiceImpl --> ReviewDetailRepository : uses
    ReviewServiceImpl --> ReviewCriteriaService : uses
    ReviewServiceImpl --> ReviewReactionService : uses
    ReviewServiceImpl --> UserRepository : uses
    ReviewServiceImpl --> ChefRepository : uses
    ReviewServiceImpl --> BookingRepository : uses
    ReviewServiceImpl --> ImageRepository : uses
    ReviewServiceImpl --> ImageService : uses
    
    ReviewCriteriaServiceImpl --> ReviewCriteriaRepository : uses
    ReviewReplyServiceImpl --> ReviewReplyRepository : uses
    ReviewReplyServiceImpl --> ReviewRepository : uses
    ReviewReplyServiceImpl --> UserRepository : uses
    ReviewReactionServiceImpl --> ReviewReactionRepository : uses
    ReviewReactionServiceImpl --> ReviewRepository : uses
    ReviewReactionServiceImpl --> UserRepository : uses
```

## Activity Diagrams

### Review Creation Activity Diagram
```mermaid
flowchart TD
    Start([Start]) --> A[Client sends review creation request]
    A --> B[Find user by userId]
    B --> C{User exists?}
    C -->|No| D[Return 404 Not Found]
    D --> End1([End])
    C -->|Yes| E[Find chef by chefId]
    E --> F{Chef exists?}
    F -->|No| G[Return 404 Not Found]
    G --> End2([End])
    F -->|Yes| H{BookingId provided?}
    H -->|Yes| I[Find booking by bookingId]
    I --> J{Booking exists?}
    J -->|No| K[Return 404 Not Found]
    K --> End3([End])
    J -->|Yes| L[Create Review entity]
    H -->|No| L
    L --> M[Set basic review data]
    M --> N[Calculate weighted rating]
    N --> O[Save review]
    O --> P[Upload main image if provided]
    P --> Q[Upload additional images if provided]
    Q --> R[Create review details for each criterion]
    R --> S[Return review response]
    S --> End4([End])
```

### Chef Response Activity Diagram
```mermaid
flowchart TD
    Start([Start]) --> A[Chef sends response request]
    A --> B[Find review by reviewId]
    B --> C{Review exists?}
    C -->|No| D[Return 404 Not Found]
    D --> End1([End])
    C -->|Yes| E[Find chef by chefId]
    E --> F{Chef exists?}
    F -->|No| G[Return 404 Not Found]
    G --> End2([End])
    F -->|Yes| H{Chef is review recipient?}
    H -->|No| I[Return 403 Forbidden]
    I --> End3([End])
    H -->|Yes| J[Add chef response]
    J --> K[Set chefResponseAt = now]
    K --> L[Save updated review]
    L --> M[Return updated review response]
    M --> End4([End])
```

### Review Reply Activity Diagram
```mermaid
flowchart TD
    Start([Start]) --> A[User sends reply request]
    A --> B[Find review by reviewId]
    B --> C{Review exists?}
    C -->|No| D[Return 404 Not Found]
    D --> End1([End])
    C -->|Yes| E[Find user by userId]
    E --> F{User exists?}
    F -->|No| G[Return 404 Not Found]
    G --> End2([End])
    F -->|Yes| H[Create review reply entity]
    H --> I[Set review, user, content]
    I --> J[Set createdAt = now, isDeleted = false]
    J --> K[Save reply to database]
    K --> L[Map to response DTO]
    L --> M[Return reply response]
    M --> End3([End])
```

### Review Reaction Activity Diagram
```mermaid
flowchart TD
    Start([Start]) --> A[User sends reaction request]
    A --> B[Find review by reviewId]
    B --> C{Review exists?}
    C -->|No| D[Return 404 Not Found]
    D --> End1([End])
    C -->|Yes| E[Find user by userId]
    E --> F{User exists?}
    F -->|No| G[Return 404 Not Found]
    G --> End2([End])
    F -->|Yes| H[Check if user already reacted]
    H --> I{Has existing reaction?}
    I -->|Yes| J[Update existing reaction]
    I -->|No| K[Create new reaction entity]
    K --> L[Set review, user, reactionType]
    L --> M[Set createdAt = now()]
    M --> N[Save reaction to database]
    J --> N
    N --> O[Map to response DTO]
    O --> P[Return reaction response]
    P --> End3([End])
```

### Review Summary Activity Diagram
```mermaid
flowchart TD
    Start([Start]) --> A[Client requests review summary]
    A --> B[Find chef by chefId]
    B --> C{Chef exists?}
    C -->|No| D[Return 404 Not Found]
    D --> End1([End])
    C -->|Yes| E[Get average rating for chef]
    E --> F[Get review count for chef]
    F --> G[Get rating distribution for chef]
    G --> H[Calculate criteria-specific ratings]
    H --> I[Get reaction counts]
    I --> J[Build summary response]
    J --> K[Return summary response]
    K --> End2([End])
```

## Sequence Diagrams

### Create Review Sequence Diagram
```mermaid
sequenceDiagram
    actor Client
    participant Controller as ReviewController
    participant Service as ReviewServiceImpl
    participant UserRepo as UserRepository
    participant ChefRepo as ChefRepository
    participant BookingRepo as BookingRepository
    participant CriteriaService as ReviewCriteriaService
    participant DetailRepo as ReviewDetailRepository
    participant ReviewRepo as ReviewRepository
    participant ImageService as ImageService
    
    Client->>Controller: POST /api/v1/reviews (with JWT Authorization header)
    Controller->>Controller: getCurrentUser() from SecurityContext
    Controller->>Service: createReview(request, currentUser.getId())
    Service->>UserRepo: findById(userId)
    UserRepo-->>Service: User
    Service->>ChefRepo: findById(chefId)
    ChefRepo-->>Service: Chef
    
    alt bookingId provided
        Service->>BookingRepo: findById(bookingId)
        BookingRepo-->>Service: Booking
    end
    
    Service->>Service: Create Review entity
    Service->>Service: Set user, chef, booking, description, etc.
    Service->>Service: Calculate weighted rating
    Service->>ReviewRepo: save(review)
    ReviewRepo-->>Service: Saved Review
    
    alt mainImage provided
        Service->>ImageService: uploadImage(image, reviewId, "REVIEW")
        ImageService-->>Service: imageUrl
        Service->>ReviewRepo: save(review with imageUrl)
    end
    
    alt additionalImages provided
        loop for each additional image
            Service->>ImageService: uploadImage(image, reviewId, "REVIEW")
        end
    end
    
    loop for each criteria rating
        Service->>CriteriaService: getCriteriaById(criteriaId)
        CriteriaService-->>Service: ReviewCriteriaResponse
        Service->>Service: Create ReviewDetail entity
        Service->>DetailRepo: save(reviewDetail)
    end
    
    Service->>Service: Map to ReviewResponse
    Service-->>Controller: ReviewResponse
    Controller-->>Client: HTTP 201 Created (ReviewResponse)
```

### Chef Response Sequence Diagram
```mermaid
sequenceDiagram
    actor Chef
    participant Controller as ReviewController
    participant Service as ReviewServiceImpl
    participant ReviewRepo as ReviewRepository
    participant ChefRepo as ChefRepository
    participant UserService as UserService
    participant ChefService as ChefService
    
    Chef->>Controller: POST /api/v1/reviews/{id}/response (with JWT Authorization)
    Controller->>Controller: getCurrentUser() from SecurityContext
    Controller->>Service: getReviewById(id)
    Service->>ReviewRepo: findById(id)
    ReviewRepo-->>Service: Review
    Service-->>Controller: ReviewResponse
    
    Controller->>ChefService: getChefById(review.getChefId())
    ChefService-->>Controller: ChefResponseDto
    
    alt Chef's user ID != currentUser ID
        Controller-->>Chef: HTTP 403 Forbidden
    end
    
    Controller->>Service: addChefResponse(id, response, currentUser.getId())
    Service->>ReviewRepo: findById(reviewId)
    ReviewRepo-->>Service: Review
    
    alt Review not found
        Service-->>Controller: ResourceNotFoundException
        Controller-->>Chef: HTTP 404 Not Found
    end
    
    Service->>Service: Set review.response = response
    Service->>Service: Set review.chefResponseAt = now()
    Service->>ReviewRepo: save(review)
    ReviewRepo-->>Service: Updated Review
    Service->>Service: Map to ReviewResponse
    Service-->>Controller: ReviewResponse
    Controller-->>Chef: HTTP 200 OK (ReviewResponse)
```

### Add Review Reply Sequence Diagram
```mermaid
sequenceDiagram
    actor Client
    participant Controller as ReviewController
    participant ReplyService as ReviewReplyServiceImpl
    participant ReviewRepo as ReviewRepository
    participant UserRepo as UserRepository
    participant ReplyRepo as ReviewReplyRepository
    
    Client->>Controller: POST /api/v1/reviews/{id}/reply (with JWT Authorization)
    Controller->>Controller: getCurrentUser() from SecurityContext
    Controller->>ReplyService: addReply(id, currentUser.getId(), request)
    ReplyService->>ReviewRepo: findById(reviewId)
    ReviewRepo-->>ReplyService: Review
    
    alt Review not found
        ReplyService-->>Controller: ResourceNotFoundException
        Controller-->>Client: HTTP 404 Not Found
    end
    
    ReplyService->>UserRepo: findById(userId)
    UserRepo-->>ReplyService: User
    
    alt User not found
        ReplyService-->>Controller: ResourceNotFoundException
        Controller-->>Client: HTTP 404 Not Found
    end
    
    ReplyService->>ReplyService: Create ReviewReply entity
    ReplyService->>ReplyService: Set review, user, content
    ReplyService->>ReplyService: Set createdAt = now(), isDeleted = false
    ReplyService->>ReplyRepo: save(reply)
    ReplyRepo-->>ReplyService: Saved ReviewReply
    ReplyService->>ReplyService: Map to ReviewReplyResponse
    ReplyService-->>Controller: ReviewReplyResponse
    Controller-->>Client: HTTP 201 Created (ReviewReplyResponse)
```

### Add Review Reaction Sequence Diagram
```mermaid
sequenceDiagram
    actor Client
    participant Controller as ReviewController
    participant ReactionService as ReviewReactionServiceImpl
    participant ReviewRepo as ReviewRepository
    participant UserRepo as UserRepository
    participant ReactionRepo as ReviewReactionRepository
    
    Client->>Controller: POST /api/v1/reviews/{id}/reaction (with JWT Authorization)
    Controller->>Controller: getCurrentUser() from SecurityContext
    Controller->>ReactionService: addReaction(id, currentUser.getId(), request)
    ReactionService->>ReviewRepo: findById(reviewId)
    ReviewRepo-->>ReactionService: Review
    
    alt Review not found
        ReactionService-->>Controller: ResourceNotFoundException
        Controller-->>Client: HTTP 404 Not Found
    end
    
    ReactionService->>UserRepo: findById(userId)
    UserRepo-->>ReactionService: User
    
    alt User not found
        ReactionService-->>Controller: ResourceNotFoundException
        Controller-->>Client: HTTP 404 Not Found
    end
    
    ReactionService->>ReactionRepo: findByReviewAndUser(review, user)
    ReactionRepo-->>ReactionService: Optional<ReviewReaction>
    
    alt Reaction exists
        ReactionService->>ReactionService: Update existing reaction
    else Reaction doesn't exist
        ReactionService->>ReactionService: Create new ReviewReaction
        ReactionService->>ReactionService: Set review, user, reactionType
        ReactionService->>ReactionService: Set createdAt = now()
    end
    
    ReactionService->>ReactionRepo: save(reaction)
    ReactionRepo-->>ReactionService: Saved ReviewReaction
    ReactionService->>ReactionService: Map to ReviewReactionResponse
    ReactionService-->>Controller: ReviewReactionResponse
    
    Controller->>ReactionService: getReactionCountsByReview(id)
    ReactionService-->>Controller: Map<String, Long> counts
    
    Controller->>Controller: Create response with reaction and counts
    Controller-->>Client: HTTP 201 Created (Reaction and counts)
```

### Get Review Summary Sequence Diagram
```mermaid
sequenceDiagram
    actor Client
    participant Controller as ReviewController
    participant Service as ReviewServiceImpl
    participant ChefRepo as ChefRepository
    participant ReviewRepo as ReviewRepository
    
    Client->>Controller: GET /api/v1/reviews/chef/{chefId}?page=0&size=10&sort=newest
    Controller->>Service: getReviewsByChef(chefId, pageable)
    Service->>ChefRepo: findById(chefId)
    ChefRepo-->>Service: Chef
    
    alt Chef not found
        Service-->>Controller: ResourceNotFoundException
        Controller-->>Client: HTTP 404 Not Found
    end
    
    Service->>ReviewRepo: findByChefAndIsDeletedFalse(chef, pageable)
    ReviewRepo-->>Service: Page<Review>
    Service->>Service: Map to Page<ReviewResponse>
    Service-->>Controller: Page<ReviewResponse>
    
    Controller->>Service: getReviewCountForChef(chefId)
    Service->>ReviewRepo: countByChef(chef)
    ReviewRepo-->>Service: Count
    
    Controller->>Service: getAverageRatingForChef(chefId)
    Service->>ReviewRepo: findAverageRatingByChef(chef)
    ReviewRepo-->>Service: Average Rating
    
    Controller->>Service: getRatingDistributionForChef(chefId)
    Service->>Service: Calculate ratings distribution
    Service-->>Controller: Map<String, Long> distribution
    
    Controller->>Controller: Build response Map with reviews, counts, ratings, etc.
    Controller-->>Client: HTTP 200 OK (Reviews Summary Response)
```

## Abbreviations
- **DTO**: Data Transfer Object
- **API**: Application Programming Interface
- **CRUD**: Create, Read, Update, Delete
- **JPA**: Java Persistence API 