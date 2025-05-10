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
        <<interface>>
        +findAvailableTimeSlotsWithInSingleDate(Long chefId, LocalDate date, String customerLocation, Long menuId, List~Long~ dishIds, int guestCount, int maxDishesPerMeal) : List~AvailableTimeSlotResponse~
        +findAvailableTimeSlotsWithInMultipleDates(Long chefId, String customerLocation, int guestCount, int maxDishesPerMeal, List~AvailableTimeSlotRequest~ requests) : List~AvailableTimeSlotResponse~
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
    
    class TimeZoneService {
        +getTimezoneFromAddress(String address) : String
        +convertBetweenTimezones(LocalDateTime localDateTime, String sourceTimezone, String targetTimezone) : LocalDateTime
    }
    
    %% DTOs
    class AvailableTimeSlotResponse {
        +Long chefId
        +String chefName
        +LocalDate date
        +LocalTime startTime
        +LocalTime endTime
        +Integer durationMinutes
        +String note
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
    AvailabilityFinderService ..> TimeZoneService: uses
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
    N --> N1[Verify chef owns the schedule]
    N1 -->|Not owner| N2[Return permission error]
    N2 --> Z
    N1 -->|Is owner| O[Validate time constraints]
    O --> P[Validate no conflicts]
    P --> Q[Update and save schedule]
    Q --> Z
    
    %% Delete Schedule Path
    B -->|Delete Schedule| R[Chef requests to delete schedule]
    R --> S[ChefScheduleController receives delete request]
    S --> T[Call ChefScheduleService]
    T --> T1[Verify chef owns the schedule]
    T1 -->|Not owner| T2[Return permission error]
    T2 --> Z
    T1 -->|Is owner| U[Check for active bookings]
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
    F -->|Yes| H[Check for conflicts with existing blocked dates]
    H --> I{Blocked date conflict?}
    I -->|Yes| J[Display blocked date conflict error]
    J --> D
    I -->|No| K[Check for conflicts with schedules]
    K --> L{Schedule conflict?}
    L -->|Yes| M[Display schedule conflict error]
    M --> D
    L -->|No| N[Check for conflicts with bookings]
    N --> O{Booking conflict?}
    O -->|Yes| P[Display booking conflict error]
    P --> D
    O -->|No| Q[Create and save ChefBlockedDate]
    Q --> R[Display success message]
    
    %% Date Range Block Path
    C -->|Date Range| S[Enter blocked date range: start date, end date, times, reason]
    S --> T[Validate date range and time constraints]
    T --> U{Valid range and times?}
    U -->|No| V[Display validation error]
    V --> S
    U -->|Yes| W[Process each date in range]
    
    W --> X[Determine start/end times for current date]
    X --> Y[Check conflicts with existing blocked dates]
    Y --> Z{Blocked date conflict?}
    Z -->|Yes| AA[Display blocked date conflict error]
    AA --> S
    Z -->|No| AB[Check conflicts with schedules]
    AB --> AC{Schedule conflict?}
    AC -->|Yes| AD[Display schedule conflict error]
    AD --> S
    AC -->|No| AE[Check conflicts with bookings]
    AE --> AF{Booking conflict?}
    AF -->|Yes| AG[Display booking conflict error]
    AG --> S
    AF -->|No| AH[Create and save ChefBlockedDate for current date]
    
    AH --> AI{More dates in range?}
    AI -->|Yes| W
    AI -->|No| AJ[Display success message]
    
    R --> AK[End]
    AJ --> AK
    
    %% Delete Blocked Date Path
    C -->|Delete| AL[Select blocked date to delete]
    AL --> AM[Check if chef owns the blocked date]
    AM --> AN{Is owner?}
    AN -->|No| AO[Display permission error]
    AO --> AK
    AN -->|Yes| AP[Soft delete blocked date]
    AP --> AQ[Display success message]
    AQ --> AK
    
    %% Update Blocked Date Path
    C -->|Update| AR[Select blocked date to update]
    AR --> AS[Check if chef owns the blocked date]
    AS --> AT{Is owner?}
    AT -->|No| AU[Display permission error]
    AU --> AK
    AT -->|Yes| AV[Modify blocked date details]
    AV --> AW[Validate time constraints]
    AW --> AX{Time valid?}
    AX -->|No| AY[Display time constraint error]
    AY --> AV
    AX -->|Yes| AZ[Check for conflicts with schedules, bookings, and other blocked dates]
    AZ --> BA{Any conflicts?}
    BA -->|Yes| BB[Display conflict error]
    BB --> AV
    BA -->|No| BC[Update and save ChefBlockedDate]
    BC --> BD[Display success message]
    BD --> AK
    
    %% View Blocked Dates Path
    C -->|View| BE[Select view option]
    BE --> BF{View option?}
    BF -->|All| BG[Retrieve all non-deleted blocked dates]
    BF -->|By Date Range| BH[Enter date range]
    BH --> BI[Retrieve blocked dates within range]
    BF -->|By Specific Date| BJ[Enter specific date]
    BJ --> BK[Retrieve blocked dates for specific date]
    
    BG --> BL[Display blocked dates]
    BI --> BL
    BK --> BL
    BL --> AK
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
    participant TS as TimeZoneService
    participant DB as Database

    Client->>AC: GET /api/v1/availability/chef/{chefId}/single-date
    Note over Client,AC: Query params: date, customerLocation, menuId/dishIds, guestCount, maxDishesPerMeal
    
    AC->>AFS: findAvailableTimeSlotsWithInSingleDate(chefId, date, customerLocation, menuId, dishIds, guestCount, maxDishesPerMeal)
    
    AFS->>CR: findById(chefId)
    CR->>DB: SELECT * FROM chef WHERE id = chefId
    DB-->>CR: chef data
    CR-->>AFS: Chef entity
    
    AFS->>TS: getTimezoneFromAddress(chefAddress)
    TS-->>AFS: Chef timezone ID
    
    AFS->>TS: getTimezoneFromAddress(customerLocation)
    TS-->>AFS: Customer timezone ID
    
    AFS->>DS: calculateDistance(chefLocation, customerLocation)
    DS-->>AFS: Return travel time & distance
    
    alt If menuId is provided
        AFS->>DB: SELECT * FROM menu_dish WHERE menu_id = menuId
        DB-->>AFS: menu dishes data
    else If dishIds is provided
        AFS->>DB: SELECT * FROM dish WHERE id IN (dishIds)
        DB-->>AFS: dishes data
    end
    
    AFS->>CS: calculateCookingTime(dishes, guestCount)
    CS-->>AFS: Return cooking time
    
    AFS->>CSR: findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek)
    CSR->>DB: SELECT * FROM chef_schedule WHERE chef_id = ? AND day_of_week = ? AND is_deleted = false
    DB-->>CSR: schedules data
    CSR-->>AFS: List of schedules
    
    AFS->>CBDR: findByChefAndBlockedDateAndIsDeletedFalse(chef, date)
    CBDR->>DB: SELECT * FROM chef_blocked_date WHERE chef_id = ? AND blocked_date = ? AND is_deleted = false
    DB-->>CBDR: blocked dates data
    CBDR-->>AFS: List of blocked dates
    
    AFS->>BDR: findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, date)
    BDR->>DB: SELECT * FROM booking_detail bd JOIN booking b ON bd.booking_id = b.id WHERE b.chef_id = ? AND bd.session_date = ? AND bd.is_deleted = false
    DB-->>BDR: booking details data
    BDR-->>AFS: List of bookings
    
    loop For each schedule
        AFS->>AFS: Filter blocked dates for current date
        
        AFS->>AFS: Create timeline of events (bookings and blocked periods)
        
        AFS->>AFS: Process timeline to find available time slots
        Note over AFS: Create slots where no overlap with blocked dates or bookings
        
        AFS->>AFS: Apply minimum slot duration filtering
        
        AFS->>AFS: Add valid slots to availableSlots list
    end
    
    AFS->>AFS: Filter slots requiring 24h advance notice
    AFS->>AFS: Filter slots that fit within a schedule
    AFS->>AFS: Sort slots by date and time
    
    AFS->>TS: Convert slots from chef timezone to customer timezone
    TS-->>AFS: Return converted time slots
    
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
    participant TS as TimeZoneService
    participant DB as Database

    Client->>AC: POST /api/v1/availability/chef/{chefId}/multiple-dates
    Note over Client,AC: Query params: customerLocation, guestCount, maxDishesPerMeal
    Note over Client,AC: Request body: List<AvailableTimeSlotRequest>
    
    AC->>AFS: findAvailableTimeSlotsWithInMultipleDates(chefId, customerLocation, guestCount, maxDishesPerMeal, availableTimeSlotRequests)
    
    AFS->>CR: findById(chefId)
    CR->>DB: SELECT * FROM chef WHERE id = chefId
    DB-->>CR: chef data
    CR-->>AFS: Chef entity
    
    AFS->>TS: getTimezoneFromAddress(chefAddress)
    TS-->>AFS: Chef timezone ID
    
    AFS->>TS: getTimezoneFromAddress(customerLocation)
    TS-->>AFS: Customer timezone ID
    
    AFS->>DS: calculateDistance(chefLocation, customerLocation)
    DS-->>AFS: Return travel time & distance
    
    loop For each AvailableTimeSlotRequest
        Note over AFS: Each request contains date, menuId/dishIds
        
        alt If request has menuId
            AFS->>DB: SELECT * FROM menu_dish WHERE menu_id = menuId
            DB-->>AFS: menu dishes data
        else If request has dishIds
            AFS->>DB: SELECT * FROM dish WHERE id IN (dishIds)
            DB-->>AFS: dishes data
        end
        
        AFS->>CS: calculateCookingTime(dishes, guestCount)
        CS-->>AFS: Return cooking time
        
        AFS->>CSR: findByChefAndDayOfWeekAndIsDeletedFalse(chef, dayOfWeek)
        CSR->>DB: SELECT * FROM chef_schedule WHERE chef_id = ? AND day_of_week = ? AND is_deleted = false
        DB-->>CSR: schedules data
        CSR-->>AFS: List of schedules
        
        AFS->>CBDR: findByChefAndBlockedDateAndIsDeletedFalse(chef, request.date)
        CBDR->>DB: SELECT * FROM chef_blocked_date WHERE chef_id = ? AND blocked_date = ? AND is_deleted = false
        DB-->>CBDR: blocked dates data
        CBDR-->>AFS: List of blocked dates
        
        AFS->>BDR: findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, request.date)
        BDR->>DB: SELECT * FROM booking_detail bd JOIN booking b ON bd.booking_id = b.id WHERE b.chef_id = ? AND bd.session_date = ? AND bd.is_deleted = false
        DB-->>BDR: booking details data
        BDR-->>AFS: List of bookings
        
        loop For each schedule
            AFS->>AFS: Filter blocked dates for current date
            
            AFS->>AFS: Create timeline of events (bookings and blocked periods)
            
            AFS->>AFS: Process timeline to find available time slots
            Note over AFS: Create slots where no overlap with blocked dates or bookings
            
            AFS->>AFS: Apply minimum slot duration filtering
            
            AFS->>AFS: Add valid slots to availableSlots list
        end
    end
    
    AFS->>AFS: Filter slots requiring 24h advance notice
    AFS->>AFS: Filter slots that fit within a schedule
    AFS->>AFS: Sort slots by date and time
    
    AFS->>TS: Convert slots from chef timezone to customer timezone
    TS-->>AFS: Return converted time slots
    
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
- DB: Database
- TS: TimeZoneService
