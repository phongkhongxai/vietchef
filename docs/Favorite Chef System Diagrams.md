# Favorite Chef System Feature Documentation

## Introduction
This document describes the favorite chef system feature of the VietChef application, which allows customers to save their favorite chefs for easy access and tracking.

## Class Diagram
The following class diagram illustrates the relationships between the main components of the favorite chef system feature:

```mermaid
classDiagram
    %% Controllers
    class FavoriteChefController {
        +FavoriteChefDto addFavoriteChef(Long userId, Long chefId)
        +String removeFavoriteChef(Long userId, Long chefId)
        +FavoriteChefsResponse getFavoriteChefs(Long userId, int pageNo, int pageSize, String sortBy, String sortDir)
        +Boolean isChefFavorite(Long userId, Long chefId)
    }
    
    %% Services
    class FavoriteChefService {
        +FavoriteChefDto addFavoriteChef(Long userId, Long chefId)
        +void removeFavoriteChef(Long userId, Long chefId)
        +FavoriteChefsResponse getFavoriteChefs(Long userId, int pageNo, int pageSize, String sortBy, String sortDir)
        +boolean isChefFavorite(Long userId, Long chefId)
    }
    
    class FavoriteChefServiceImpl {
        -FavoriteChefRepository favoriteChefRepository
        -UserRepository userRepository
        -ChefRepository chefRepository
        -ModelMapper modelMapper
        +FavoriteChefDto addFavoriteChef(Long userId, Long chefId)
        +void removeFavoriteChef(Long userId, Long chefId)
        +FavoriteChefsResponse getFavoriteChefs(Long userId, int pageNo, int pageSize, String sortBy, String sortDir)
        +boolean isChefFavorite(Long userId, Long chefId)
        -FavoriteChefDto mapToFavoriteChefDto(FavoriteChef favoriteChef)
    }
    
    %% Repositories
    class FavoriteChefRepository {
        +Optional~FavoriteChef~ findByUserAndChefAndIsDeletedFalse(User user, Chef chef)
        +Optional~FavoriteChef~ findByUserAndChef(User user, Chef chef)
        +Page~FavoriteChef~ findByUserAndIsDeletedFalse(User user, Pageable pageable)
        +Page~FavoriteChef~ findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable)
        +boolean existsByUserAndChefAndIsDeletedFalse(User user, Chef chef)
    }
    
    %% Entities and Models
    class FavoriteChef {
        +Long id
        +User user
        +Chef chef
        +LocalDateTime createdAt
        +Boolean isDeleted
    }
    
    class User {
        +Long id
        +String fullName
        +String email
        +String avatarUrl
        +List~FavoriteChef~ favoriteChefs
    }
    
    class Chef {
        +Long id
        +User user
        +String bio
        +String description
        +String address
        +BigDecimal price
        +Integer maxServingSize
        +String specialization
        +String status
        +List~FavoriteChef~ favoritedBy
    }
    
    %% DTOs
    class FavoriteChefDto {
        +Long id
        +Long userId
        +Long chefId
        +String chefName
        +String chefAvatar
        +String chefSpecialization
        +String chefAddress
        +LocalDateTime createdAt
    }
    
    class FavoriteChefsResponse {
        +List~FavoriteChefDto~ content
        +int pageNo
        +int pageSize
        +long totalElements
        +int totalPages
        +boolean last
    }
    
    %% Relationships
    FavoriteChefController ..> FavoriteChefService : uses
    FavoriteChefService <|.. FavoriteChefServiceImpl : implements
    FavoriteChefServiceImpl ..> FavoriteChefRepository : uses
    FavoriteChefServiceImpl ..> UserRepository : uses
    FavoriteChefServiceImpl ..> ChefRepository : uses
    FavoriteChefRepository ..> FavoriteChef : manages
    User "1" -- "*" FavoriteChef : has
    Chef "1" -- "*" FavoriteChef : favoritedBy
    FavoriteChef "*" -- "1" User : belongs to
    FavoriteChef "*" -- "1" Chef : references
```

## Activity Diagrams

### Add Favorite Chef Activity Diagram
This diagram illustrates the flow of operations for adding a chef to favorites:

```mermaid
flowchart TD
    A[Start] --> B[Customer views chef profile]
    B --> C[Customer clicks 'Add to Favorites' button]
    C --> D[Request sent to FavoriteChefController]
    D --> E[Controller calls FavoriteChefService]
    E --> F[Service validates user exists]
    F --> G[Service validates chef exists]
    G --> H{Chef status is ACTIVE?}
    H -->|No| I[Throw exception: Chef is not active]
    H -->|Yes| J[Check if chef is in user's favorites]
    J --> K{Does record exist?}
    
    %% Doesn't exist path
    K -->|No| L[Create new FavoriteChef entity]
    L --> M[Set user, chef, isDeleted=false]
    M --> N[Save to database]
    
    %% Exists path
    K -->|Yes| O{Is record already active?}
    O -->|Yes| P[Throw exception: Already in favorites]
    O -->|No| Q[Set isDeleted=false to reactivate]
    Q --> R[Save to database]
    
    %% Final steps
    N --> S[Map to FavoriteChefDto]
    R --> S
    S --> T[Return FavoriteChefDto]
    
    I --> U[End]
    P --> U
    T --> U
```

### Remove Favorite Chef Activity Diagram
This diagram shows the process of removing a chef from favorites:

```mermaid
flowchart TD
    A[Start] --> B[Customer views favorites list]
    B --> C[Customer clicks 'Remove' button for a chef]
    C --> D[Request sent to FavoriteChefController]
    D --> E[Controller calls FavoriteChefService]
    E --> F[Service validates user exists]
    F --> G[Service validates chef exists]
    G --> H{Favorite exists and is active?}
    H -->|No| I[Throw exception: Not found in favorites]
    H -->|Yes| J[Set isDeleted = true]
    J --> K[Save updated entity]
    K --> L[Return success message]
    I --> M[End]
    L --> M
```

### Get Favorite Chefs Activity Diagram
This diagram details the process of retrieving a user's favorite chefs:

```mermaid
flowchart TD
    A[Start] --> B[Customer navigates to favorites page]
    B --> C[Request sent to FavoriteChefController]
    C --> D[Controller calls FavoriteChefService with pagination parameters]
    D --> E[Service validates user exists]
    E --> F[Create Pageable object with sort options]
    F --> G[Query repository for user's favorites]
    G --> H[Map FavoriteChef entities to DTOs]
    H --> I[Create FavoriteChefsResponse with pagination info]
    I --> J[Return response to controller]
    J --> K[Controller returns response to client]
    K --> L[Client displays paginated list of favorite chefs]
    L --> M[End]
```

### Check Favorite Status Activity Diagram
This diagram illustrates the process of checking if a chef is in a user's favorites:

```mermaid
flowchart TD
    A[Start] --> B[UI needs to know if chef is favorited]
    B --> C[Request sent to FavoriteChefController]
    C --> D[Controller calls isChefFavorite service method]
    D --> E[Service validates user exists]
    E --> F[Service validates chef exists]
    F --> G[Query repository to check if chef is in favorites]
    G --> H[Return boolean result]
    H --> I[Controller returns result]
    I --> J[UI updates favorite button state]
    J --> K[End]
```

## Sequence Diagrams

### Add Favorite Chef Sequence Diagram
This sequence diagram details the process of adding a chef to a user's favorites:

```mermaid
sequenceDiagram
    actor Customer
    participant FC as FavoriteChefController
    participant FS as FavoriteChefServiceImpl
    participant FCR as FavoriteChefRepository
    participant UR as UserRepository
    participant CR as ChefRepository
    
    Customer->>FC: POST /api/v1/favorite-chefs/{userId}/chefs/{chefId}
    FC->>FS: addFavoriteChef(userId, chefId)
    
    FS->>UR: findExistUserById(userId)
    UR-->>FS: Return User
    
    FS->>CR: findById(chefId)
    CR-->>FS: Return Chef
    
    Note over FS: Check if chef status is ACTIVE
    alt Chef status is not ACTIVE
        FS-->>FC: Throw VchefApiException: "Chef is not active"
    else Chef status is ACTIVE
        FS->>FCR: findByUserAndChef(user, chef)
        FCR-->>FS: Return Optional<FavoriteChef>
        
        alt FavoriteChef exists
            Note over FS: Check if favorite is already active
            alt isDeleted = false (already in favorites)
                FS-->>FC: Throw VchefApiException: "Chef already in favorites"
            else isDeleted = true (was deleted before)
                FS->>FS: Set isDeleted = false
                FS->>FCR: save(favoriteChef)
                FCR-->>FS: Return reactivated FavoriteChef
            end
        else FavoriteChef doesn't exist
            FS->>FS: Create new FavoriteChef
            FS->>FS: Set user, chef, isDeleted=false
            FS->>FCR: save(favoriteChef)
            FCR-->>FS: Return saved FavoriteChef
        end
        
        FS->>FS: Map to FavoriteChefDto
        FS-->>FC: Return FavoriteChefDto
    end
    
    FC-->>Customer: Return FavoriteChefDto JSON response
```

### Remove Favorite Chef Sequence Diagram
This sequence diagram shows the process of removing a chef from favorites:

```mermaid
sequenceDiagram
    actor Customer
    participant FC as FavoriteChefController
    participant FS as FavoriteChefServiceImpl
    participant FCR as FavoriteChefRepository
    participant UR as UserRepository
    participant CR as ChefRepository
    
    Customer->>FC: DELETE /api/v1/favorite-chefs/{userId}/chefs/{chefId}
    FC->>FS: removeFavoriteChef(userId, chefId)
    
    FS->>UR: findExistUserById(userId)
    UR-->>FS: Return User
    
    FS->>CR: findById(chefId)
    CR-->>FS: Return Chef
    
    FS->>FCR: findByUserAndChefAndIsDeletedFalse(user, chef)
    FCR-->>FS: Return Optional<FavoriteChef>
    
    alt FavoriteChef exists and is active
        FS->>FS: Set isDeleted = true
        FS->>FCR: save(favoriteChef)
        FCR-->>FS: Return updated FavoriteChef
        FS-->>FC: No return value (void)
    else FavoriteChef doesn't exist or is already removed
        FS-->>FC: Throw VchefApiException: "Chef not found in favorites"
    end
    
    FC-->>Customer: Return success message
```

### Get Favorite Chefs Sequence Diagram
This sequence diagram illustrates the process of retrieving a user's favorite chefs with pagination:

```mermaid
sequenceDiagram
    actor Customer
    participant FC as FavoriteChefController
    participant FS as FavoriteChefServiceImpl
    participant FCR as FavoriteChefRepository
    participant UR as UserRepository
    
    Customer->>FC: GET /api/v1/favorite-chefs/{userId}?pageNo=0&pageSize=10&sortBy=createdAt&sortDir=desc
    FC->>FS: getFavoriteChefs(userId, pageNo, pageSize, sortBy, sortDir)
    
    FS->>UR: findExistUserById(userId)
    UR-->>FS: Return User
    
    FS->>FS: Create Sort object
    FS->>FS: Create Pageable object
    
    FS->>FCR: findByUserIdAndIsDeletedFalse(userId, pageable)
    FCR-->>FS: Return Page<FavoriteChef>
    
    FS->>FS: Convert each FavoriteChef to FavoriteChefDto
    FS->>FS: Create FavoriteChefsResponse with pagination info
    
    FS-->>FC: Return FavoriteChefsResponse
    FC-->>Customer: Return FavoriteChefsResponse JSON
```

### Check Favorite Status Sequence Diagram
This sequence diagram shows the process of checking if a chef is in a user's favorites:

```mermaid
sequenceDiagram
    actor Client
    participant FC as FavoriteChefController
    participant FS as FavoriteChefServiceImpl
    participant FCR as FavoriteChefRepository
    participant UR as UserRepository
    participant CR as ChefRepository
    
    Client->>FC: GET /api/v1/favorite-chefs/{userId}/chefs/{chefId}
    FC->>FS: isChefFavorite(userId, chefId)
    
    FS->>UR: findExistUserById(userId)
    UR-->>FS: Return User
    
    FS->>CR: findById(chefId)
    CR-->>FS: Return Chef
    
    FS->>FCR: existsByUserAndChefAndIsDeletedFalse(user, chef)
    FCR-->>FS: Return boolean
    
    FS-->>FC: Return boolean
    FC-->>Client: Return boolean JSON response
```

## Abbreviations
- FC: FavoriteChefController
- FS: FavoriteChefServiceImpl
- FCR: FavoriteChefRepository
- UR: UserRepository
- CR: ChefRepository 