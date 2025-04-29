# Schedule Management Feature Documentation

## Introduction
This document describes the schedule management feature of the VietChef application, including chef schedules, chef blocked dates, and availability slots finder.

## Class Diagram
The following class diagram illustrates the relationships between the main components of the schedule management feature:

```mermaid
classDiagram
    %% Controllers
    class ChefScheduleController {
        +createSchedule(ChefScheduleRequest request) : ChefScheduleResponse
        +createMultipleSchedules(ChefMultipleScheduleRequest request) : List~ChefScheduleResponse~
        +getScheduleById(Long scheduleId) : ChefScheduleResponse
        +getSchedulesForCurrentChef() : List~ChefScheduleResponse~
        +updateSchedule(ChefScheduleUpdateRequest request) : ChefScheduleResponse
        +deleteSchedule(Long scheduleId) : void
        +deleteSchedulesByDayOfWeek(Integer dayOfWeek) : void
    }
    
    class ChefBlockedDateController {
        +createBlockedDate(ChefBlockedDateRequest request) : ChefBlockedDateResponse
        +createBlockedDateRange(ChefBlockedDateRangeRequest request) : List~ChefBlockedDateResponse~
        +getBlockedDateById(Long blockId) : ChefBlockedDateResponse
        +getBlockedDatesForCurrentChef() : List~ChefBlockedDateResponse~
        +getBlockedDatesBetween(LocalDate startDate, LocalDate endDate) : List~ChefBlockedDateResponse~
        +getBlockedDatesByDate(LocalDate date) : List~ChefBlockedDateResponse~
        +updateBlockedDate(ChefBlockedDateUpdateRequest request) : ChefBlockedDateResponse
        +deleteBlockedDate(Long blockId) : void
    }
    
    class AvailabilityFinderController {
        +findAvailableTimeSlotsForChef(Long chefId, LocalDate startDate, LocalDate endDate) : List~AvailableTimeSlotResponse~
        +findAvailableTimeSlotsForCurrentChef(LocalDate startDate, LocalDate endDate) : List~AvailableTimeSlotResponse~
        +findAvailableTimeSlotsForChefByDate(Long chefId, LocalDate date) : List~AvailableTimeSlotResponse~
        +checkTimeSlotAvailability(Long chefId, LocalDate date, LocalTime startTime, LocalTime endTime) : Boolean
        +findAvailableTimeSlotsInSingleDate(Long chefId, LocalDate date, String customerLocation, Long menuId, List~Long~ dishIds, int guestCount, int maxDishesPerMeal) : List~AvailableTimeSlotResponse~
        +findAvailableTimeSlotsInMultipleDates(Long chefId, String customerLocation, int guestCount, int maxDishesPerMeal, List~AvailableTimeSlotRequest~ requests) : List~AvailableTimeSlotResponse~
    }
    
    %% Entities and Models
    class Chef {
        +Long id
        +User user
        +String bio
        +String description
        +String address
        +BigDecimal price
        +Integer maxServingSize
        +String status
        +Boolean isDeleted
        +String specialization
        +List~ChefSchedule~ schedules
    }

    class User {
        +Long id
        +String fullName
        +String email
    }
    
    class ChefSchedule {
        +Long id
        +Chef chef
        +Integer dayOfWeek
        +LocalTime startTime
        +LocalTime endTime
        +Boolean isDeleted
    }
    
    class ChefBlockedDate {
        +Long blockId
        +Chef chef
        +LocalDate blockedDate
        +LocalTime startTime
        +LocalTime endTime
        +String reason
        +Boolean isDeleted
    }
    
    class BookingDetail {
        +Long id
        +Booking booking
        +LocalDate sessionDate
        +LocalTime startTime
        +LocalTime endTime
        +LocalTime timeBeginTravel
        +String status
        +Boolean isDeleted
    }
    
    %% Services
    class AvailabilityFinderService {
        +findAvailableTimeSlotsForChef(Long chefId, LocalDate startDate, LocalDate endDate) : List~AvailableTimeSlotResponse~
        +findAvailableTimeSlotsForCurrentChef(LocalDate startDate, LocalDate endDate) : List~AvailableTimeSlotResponse~
        +findAvailableTimeSlotsForChefByDate(Long chefId, LocalDate date) : List~AvailableTimeSlotResponse~
        +isTimeSlotAvailable(Long chefId, LocalDate date, LocalTime startTime, LocalTime endTime) : boolean
        +findAvailableTimeSlotsWithInSingleDate(Long chefId, LocalDate date, String customerLocation, Long menuId, List~Long~ dishIds, int guestCount, int maxDishesPerMeal) : List~AvailableTimeSlotResponse~
        +findAvailableTimeSlotsWithInMultipleDates(Long chefId, String customerLocation, int guestCount, int maxDishesPerMeal, List~AvailableTimeSlotRequest~) : List~AvailableTimeSlotResponse~
    }
    
    class ChefScheduleService {
        +createScheduleForCurrentChef(ChefScheduleRequest request) : ChefScheduleResponse
        +createMultipleSchedulesForCurrentChef(ChefMultipleScheduleRequest request) : List~ChefScheduleResponse~
        +getScheduleById(Long scheduleId) : ChefScheduleResponse
        +getSchedulesForCurrentChef() : List~ChefScheduleResponse~
        +updateSchedule(ChefScheduleUpdateRequest request) : ChefScheduleResponse
        +deleteSchedule(Long scheduleId) : void
        +deleteSchedulesByDayOfWeek(Integer dayOfWeek) : void
    }
    
    class ChefBlockedDateService {
        +createBlockedDateForCurrentChef(ChefBlockedDateRequest request) : ChefBlockedDateResponse
        +createBlockedDateRangeForCurrentChef(ChefBlockedDateRangeRequest request) : List~ChefBlockedDateResponse~
        +getBlockedDateById(Long blockId) : ChefBlockedDateResponse
        +getBlockedDatesForCurrentChef() : List~ChefBlockedDateResponse~
        +getBlockedDatesForCurrentChefBetween(LocalDate startDate, LocalDate endDate) : List~ChefBlockedDateResponse~
        +getBlockedDatesForCurrentChefByDate(LocalDate date) : List~ChefBlockedDateResponse~
        +updateBlockedDate(ChefBlockedDateUpdateRequest request) : ChefBlockedDateResponse
        +deleteBlockedDate(Long blockId) : void
    }
    
    class BookingConflictService {
        +hasBookingConflict(Chef chef, LocalDate date, LocalTime startTime, LocalTime endTime) : boolean
        +hasBookingConflictOnDayOfWeek(Chef chef, Integer dayOfWeek, LocalTime startTime, LocalTime endTime, Integer daysToCheck) : boolean
        +hasActiveBookingsForDayOfWeek(Long chefId, Integer dayOfWeek) : boolean
    }
    
    %% DTOs
    class AvailableTimeSlotResponse {
        +Long chefId
        +String chefName
        +LocalDate date
        +LocalTime startTime
        +LocalTime endTime
        +Integer durationMinutes
    }

    class AvailableTimeSlotRequest {
        +LocalDate date
        +Long menuId
        +List~Long~ dishIds
    }
    
    %% Relationships
    Chef "1" -- "1" User
    Chef "1" -- "*" ChefSchedule
    Chef "1" -- "*" ChefBlockedDate
    
    %% Controller-Service relationships
    ChefScheduleController ..> ChefScheduleService: uses
    ChefBlockedDateController ..> ChefBlockedDateService: uses
    AvailabilityFinderController ..> AvailabilityFinderService: uses
    
    %% Service relationships
    AvailabilityFinderService ..> ChefSchedule: uses
    AvailabilityFinderService ..> ChefBlockedDate: uses
    AvailabilityFinderService ..> BookingDetail: uses
    AvailabilityFinderService ..> DistanceService: uses
    AvailabilityFinderService ..> CalculateService: uses
    ChefScheduleService ..> BookingConflictService: uses
    ChefBlockedDateService ..> BookingConflictService: uses
```

## Activity Diagrams

### Schedule Management Activity Diagram
This diagram illustrates the flow of operations for managing chef schedules, including creation, updates, deletion, and viewing:

```mermaid
flowchart TD
    A[Start] --> B{What operation?}
    
    %% Create Schedule Path
    B -->|Create Schedule| C[Chef submits schedule request]
    C --> D[ChefScheduleController receives request]
    D --> E[Call ChefScheduleService]
    E --> F[Validate time constraints]
    F --> G[Validate no schedule conflicts]
    G --> H[Validate no blocked date conflicts]
    H --> I[Validate no booking conflicts]
    I --> J[Create and save ChefSchedule]
    J --> Z[Return response]
    
    %% Update Schedule Path
    B -->|Update Schedule| K[Chef submits schedule update]
    K --> L[ChefScheduleController receives update]
    L --> M[Call ChefScheduleService]
    M --> N[Find existing schedule]
    N --> O[Validate time constraints]
    O --> P[Validate no conflicts]
    P --> Q[Update and save schedule]
    Q --> Z
    
    %% Delete Schedule Path
    B -->|Delete Schedule| R[Chef requests to delete schedule]
    R --> S[ChefScheduleController receives delete request]
    S --> T[Call ChefScheduleService]
    T --> U[Check for active bookings]
    U --> V{Has active bookings?}
    V -->|Yes| W[Return error: cannot delete with active bookings]
    V -->|No| X[Soft delete schedule]
    X --> Z
    
    %% View Schedules Path
    B -->|View Schedules| Y[Chef requests to view schedules]
    Y --> AA[ChefScheduleController receives request]
    AA --> AB[Call ChefScheduleService]
    AB --> AC[Get current chef]
    AC --> AD[Retrieve all non-deleted schedules]
    AD --> AE[Map to response DTOs]
    AE --> Z
    
    %% Find Available Slots
    B -->|Find Available Slots| AF[Submit date range request]
    AF --> AG[AvailabilityFinderController receives request]
    AG --> AH[Call AvailabilityFinderService]
    AH --> AI[Find available slots]
    AI --> AJ[Return available time slots]
    AJ --> Z
```

### Chef Blocked Date Activity Diagram
This diagram shows the comprehensive process of managing chef blocked dates, including creating single/range blocks, updating, deleting, and viewing blocked dates:

```mermaid
flowchart TD
    A[Start] --> B[Chef requests to create blocked date]
    B --> C{Select operation type}
    
    %% Single Date Block Path
    C -->|Single Date| D[Enter blocked date details: date, time, reason]
    D --> E[Validate time constraints]
    E --> F{Time valid?}
    F -->|No| G[Display time constraint error]
    G --> D
    F -->|Yes| H[Check for conflicts with existing schedules]
    H --> I{Schedule conflict?}
    I -->|Yes| J[Display schedule conflict error]
    J --> D
    I -->|No| K[Check for conflicts with bookings]
    K --> L{Booking conflict?}
    L -->|Yes| M[Display booking conflict error]
    M --> D
    L -->|No| N[Create and save ChefBlockedDate]
    N --> O[Display success message]
    
    %% Date Range Block Path
    C -->|Date Range| P[Enter blocked date range: start date, end date, times, reason]
    P --> Q[Validate date range and time constraints]
    Q --> R{Valid range and times?}
    R -->|No| S[Display validation error]
    S --> P
    R -->|Yes| T[Process each date in range]
    
    T --> U[Determine start/end times for current date]
    U --> V[Check conflicts with existing blocked dates]
    V --> W{Blocked date conflict?}
    W -->|Yes| X[Display blocked date conflict error]
    X --> P
    W -->|No| Y[Check conflicts with schedules]
    Y --> Z{Schedule conflict?}
    Z -->|Yes| AA[Display schedule conflict error]
    AA --> P
    Z -->|No| AB[Check conflicts with bookings]
    AB --> AC{Booking conflict?}
    AC -->|Yes| AD[Display booking conflict error]
    AD --> P
    AC -->|No| AE[Create and save ChefBlockedDate for current date]
    
    AE --> AF{More dates in range?}
    AF -->|Yes| U
    AF -->|No| AG[Display success message]
    
    O --> AH[End]
    AG --> AH
    
    %% Delete Blocked Date Path
    C -->|Delete| AI[Select blocked date to delete]
    AI --> AJ[Soft delete blocked date]
    AJ --> AK[Display success message]
    AK --> AH
    
    %% Update Blocked Date Path
    C -->|Update| AL[Select blocked date to update]
    AL --> AM[Modify blocked date details]
    AM --> AN[Validate time constraints]
    AN --> AO{Time valid?}
    AO -->|No| AP[Display time constraint error]
    AP --> AM
    AO -->|Yes| AQ[Check for conflicts with schedules, bookings, and other blocked dates]
    AQ --> AR{Any conflicts?}
    AR -->|Yes| AS[Display conflict error]
    AS --> AM
    AR -->|No| AT[Update and save ChefBlockedDate]
    AT --> AU[Display success message]
    AU --> AH
    
    %% View Blocked Dates Path
    C -->|View| AV[Select view option]
    AV --> AW{View option?}
    AW -->|All| AX[Retrieve all non-deleted blocked dates]
    AW -->|By Date Range| AY[Enter date range]
    AY --> AZ[Retrieve blocked dates within range]
    AW -->|By Specific Date| BA[Enter specific date]
    BA --> BB[Retrieve blocked dates for specific date]
    
    AX --> BC[Display blocked dates]
    AZ --> BC
    BB --> BC
    BC --> AH
```

## Sequence Diagrams

### Single Date Availability Search (GET /api/v1/availability/chef/{chefId}/single-date)
This sequence diagram details the process of finding available time slots for a chef on a specific date, accounting for travel time, cooking time, and existing commitments:

```mermaid
sequenceDiagram
    actor Client
    participant AC as AvailabilityFinderController
    participant AFS as AvailabilityFinderService
    participant CR as ChefRepository
    participant CSR as ChefScheduleRepository
    participant CBDR as ChefBlockedDateRepository
    participant BDR as BookingDetailRepository
    participant DS as DistanceService
    participant CS as CalculateService

    Client->>AC: GET /api/v1/availability/chef/{chefId}/single-date
    Note over Client,AC: Query params: date, customerLocation, menuId/dishIds, guestCount, maxDishesPerMeal
    
    AC->>AFS: findAvailableTimeSlotsWithInSingleDate(chefId, date, customerLocation, menuId, dishIds, guestCount, maxDishesPerMeal)
    
    AFS->>CR: findById(chefId)
    CR-->>AFS: Return Chef
    
    AFS->>DS: calculateDistance(chefLocation, customerLocation)
    DS-->>AFS: Return travel time & distance
    
    alt If menuId is provided
        AFS->>AFS: Lookup menu dishes
    else If dishIds is provided
        AFS->>AFS: Lookup individual dishes
    end
    
    AFS->>CS: calculateCookingTime(dishes, guestCount)
    CS-->>AFS: Return cooking time
    
    AFS->>CSR: findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek)
    CSR-->>AFS: Return schedules for day
    
    AFS->>CBDR: findByChefAndBlockedDateAndIsDeletedFalse(chef, date)
    CBDR-->>AFS: Return blocked dates
    
    AFS->>BDR: findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date)
    BDR-->>AFS: Return bookings
    
    loop For each schedule
        AFS->>AFS: Check if schedule overlaps with any blocked date
        Note over AFS: If overlaps, skip this schedule
        
        AFS->>AFS: Initialize currentTime = schedule.startTime
        
        loop For each booking sorted by startTime
            AFS->>AFS: Check if gap between currentTime and booking.startTime
            Note over AFS: If gap is sufficient (considering travel + cooking + recovery time)
            
            Alt If sufficient gap exists
                AFS->>AFS: Create available slot
                AFS->>AFS: Add to availableSlots list
            End
            
            AFS->>AFS: Update currentTime = booking.endTime
        end
        
        AFS->>AFS: Check if gap between currentTime and schedule.endTime
        Note over AFS: If gap is sufficient (considering constraints)
        
        Alt If sufficient gap exists
            AFS->>AFS: Create final available slot
            AFS->>AFS: Add to availableSlots list
        End
    end
    
    AFS->>AFS: Filter slots by minimum duration requirements
    AFS->>AFS: Sort slots by date and time
    
    AFS-->>AC: Return List<AvailableTimeSlotResponse>
    AC-->>Client: Return available time slots JSON
```

### Multiple Dates Availability Search (POST /api/v1/availability/chef/{chefId}/multiple-dates)
This sequence diagram shows the process of finding available time slots across multiple dates with potentially different requirements for each date:

```mermaid
sequenceDiagram
    actor Client
    participant AC as AvailabilityFinderController
    participant AFS as AvailabilityFinderService
    participant CR as ChefRepository
    participant CSR as ChefScheduleRepository
    participant CBDR as ChefBlockedDateRepository
    participant BDR as BookingDetailRepository
    participant DS as DistanceService
    participant CS as CalculateService

    Client->>AC: POST /api/v1/availability/chef/{chefId}/multiple-dates
    Note over Client,AC: Query params: customerLocation, guestCount, maxDishesPerMeal
    Note over Client,AC: Request body: List<AvailableTimeSlotRequest>
    
    AC->>AFS: findAvailableTimeSlotsWithInMultipleDates(chefId, customerLocation, guestCount, maxDishesPerMeal, availableTimeSlotRequests)
    
    AFS->>CR: findById(chefId)
    CR-->>AFS: Return Chef
    
    AFS->>DS: calculateDistance(chefLocation, customerLocation)
    DS-->>AFS: Return travel time & distance
    
    loop For each AvailableTimeSlotRequest
        Note over AFS: Each request contains date, menuId/dishIds
        
        alt If request has menuId
            AFS->>AFS: Lookup menu dishes
        else If request has dishIds
            AFS->>AFS: Lookup individual dishes
        end
        
        AFS->>CS: calculateCookingTime(dishes, guestCount)
        CS-->>AFS: Return cooking time
        
        AFS->>CSR: findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek)
        CSR-->>AFS: Return schedules for day
        
        AFS->>CBDR: findByChefAndBlockedDateAndIsDeletedFalse(chef, request.date)
        CBDR-->>AFS: Return blocked dates
        
        AFS->>BDR: findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, request.date)
        BDR-->>AFS: Return bookings
        
        loop For each schedule
            AFS->>AFS: Check if schedule overlaps with any blocked date
            Note over AFS: If overlaps, skip this schedule
            
            AFS->>AFS: Initialize currentTime = schedule.startTime
            
            loop For each booking sorted by startTime
                AFS->>AFS: Check if gap between currentTime and booking.startTime
                Note over AFS: If gap is sufficient (considering travel + cooking + recovery time)
                
                Alt If sufficient gap exists
                    AFS->>AFS: Create available slot
                    AFS->>AFS: Add to availableSlots list
                End
                
                AFS->>AFS: Update currentTime = booking.endTime
            end
            
            AFS->>AFS: Check if gap between currentTime and schedule.endTime
            Note over AFS: If gap is sufficient (considering constraints)
            
            Alt If sufficient gap exists
                AFS->>AFS: Create final available slot
                AFS->>AFS: Add to availableSlots list
            End
        end
    end
    
    AFS->>AFS: Filter slots by minimum duration requirements
    AFS->>AFS: Sort slots by date and time
    
    AFS-->>AC: Return List<AvailableTimeSlotResponse>
    AC-->>Client: Return available time slots JSON
```

## API Endpoints

### Chef Schedule Endpoints
- **POST** `/api/v1/chef-schedules` - Create a new schedule for the current chef
- **POST** `/api/v1/chef-schedules/multiple` - Create multiple schedules for the current chef
- **GET** `/api/v1/chef-schedules/{scheduleId}` - Get schedule by ID
- **GET** `/api/v1/chef-schedules/me` - Get all schedules for the current chef
- **PUT** `/api/v1/chef-schedules` - Update a chef schedule
- **DELETE** `/api/v1/chef-schedules/{scheduleId}` - Delete a chef schedule
- **DELETE** `/api/v1/chef-schedules/day/{dayOfWeek}` - Delete all schedules for a specific day of week

### Chef Blocked Date Endpoints
- **POST** `/api/v1/chef-blocked-dates` - Create a new blocked date
- **POST** `/api/v1/chef-blocked-dates/range` - Create a range of blocked dates
- **GET** `/api/v1/chef-blocked-dates/{blockId}` - Get blocked date by ID
- **GET** `/api/v1/chef-blocked-dates/me` - Get all blocked dates for the current chef
- **GET** `/api/v1/chef-blocked-dates/range` - Get blocked dates in a date range
- **GET** `/api/v1/chef-blocked-dates/date/{date}` - Get blocked dates for a specific date
- **PUT** `/api/v1/chef-blocked-dates` - Update a blocked date
- **DELETE** `/api/v1/chef-blocked-dates/{blockId}` - Delete a blocked date

### Availability Finder Endpoints
- **GET** `/api/v1/availability/chef/{chefId}` - Find available time slots for a chef in a date range
- **GET** `/api/v1/availability/chef/me` - Find available time slots for the current chef
- **GET** `/api/v1/availability/chef/{chefId}/date/{date}` - Find available time slots for a chef on a specific date
- **GET** `/api/v1/availability/chef/{chefId}/check` - Check if a specific time slot is available
- **GET** `/api/v1/availability/chef/{chefId}/single-date` - Find available time slots considering travel and cooking time
- **POST** `/api/v1/availability/chef/{chefId}/multiple-dates` - Find available time slots across multiple dates

## Abbreviations
- AC: AvailabilityFinderController
- AFS: AvailabilityFinderService
- CR: ChefRepository
- CSR: ChefScheduleRepository
- CBDR: ChefBlockedDateRepository
- BDR: BookingDetailRepository
- DS: DistanceService
- CS: CalculateService
