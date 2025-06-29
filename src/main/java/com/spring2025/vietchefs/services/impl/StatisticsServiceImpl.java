package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.responseModel.AdminOverviewDto;
import com.spring2025.vietchefs.models.payload.responseModel.BookingStatisticsDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefOverviewDto;
import com.spring2025.vietchefs.models.payload.responseModel.UserStatisticsDto;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChefRepository chefRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;
    
    @Autowired
    private ChefTransactionRepository chefTransactionRepository;
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    
    @Autowired
    private ReviewService reviewService;

    @Override
    public AdminOverviewDto getAdminOverview() {
        // Calculate platform revenue metrics
        BigDecimal totalRevenue = bookingDetailRepository.findTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        // Calculate monthly revenue (last 30 days)
        BigDecimal monthlyRevenue = bookingDetailRepository.findMonthlyRevenue();
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;
        
        // ✅ EXACT CALCULATION: Use actual platform fee from booking details
        BigDecimal systemCommission = bookingDetailRepository.findTotalActualPlatformFee();
        if (systemCommission == null) systemCommission = BigDecimal.ZERO;
        
        // ✅ EXACT CALCULATION: Use actual chef payouts from booking details  
        BigDecimal totalPayouts = bookingDetailRepository.findTotalChefPayouts();
        if (totalPayouts == null) totalPayouts = BigDecimal.ZERO;
        BigDecimal paymentIn = customerTransactionRepository.findTotalAmountByTypes(List.of("INITIAL_PAYMENT", "PAYMENT"));
        if (paymentIn == null) paymentIn = BigDecimal.ZERO;
        BigDecimal refund = customerTransactionRepository.findTotalAmountByType("REFUND");
        if (refund == null) refund = BigDecimal.ZERO;
        BigDecimal totalDiscount = bookingDetailRepository.findTotalDiscount();
        if (totalDiscount == null) totalDiscount = BigDecimal.ZERO;
        // Calculate monthly commission for growth percentage (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal monthlyCommission = bookingDetailRepository.findActualPlatformFeeFromDate(thirtyDaysAgo);
        if (monthlyCommission == null) monthlyCommission = BigDecimal.ZERO;
        BigDecimal totalDepositPaid = bookingRepository.getTotalDepositPaidForCompletedBookings();
        if (totalDepositPaid == null) totalDepositPaid = BigDecimal.ZERO;
        BigDecimal totalDeposit = paymentRepository.getTotalDepositAllTime();
        if (totalDeposit == null) totalDeposit = BigDecimal.ZERO;
        BigDecimal totalPayout = paymentRepository.getTotalPayoutAllTime();
        if (totalPayout == null) totalPayout = BigDecimal.ZERO;
        
        // Calculate growth percentage based on commission (compare with previous 30 days)
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        BigDecimal previousMonthCommission = bookingDetailRepository.findActualPlatformFeeFromDate(sixtyDaysAgo);
        if (previousMonthCommission != null) {
            previousMonthCommission = previousMonthCommission.subtract(monthlyCommission);
        } else {
            previousMonthCommission = BigDecimal.ZERO;
        }
        
        Double platformGrowth = 0.0;
        if (previousMonthCommission.compareTo(BigDecimal.ZERO) > 0) {
            platformGrowth = monthlyCommission.subtract(previousMonthCommission)
                    .divide(previousMonthCommission, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }
        
        // Get platform statistics using correct booking statuses
        Long totalUsers = userRepository.countActiveUsers();
        Long totalChefs = userRepository.countByRole("ROLE_CHEF");
        Long totalCustomers = userRepository.countByRole("ROLE_CUSTOMER");
        
        // Use correct booking statuses based on the codebase
        Long activeBookings = bookingRepository.countByStatus("CONFIRMED") + 
                             bookingRepository.countByStatus("CONFIRMED_PAID") +
                             bookingRepository.countByStatus("CONFIRMED_PARTIALLY_PAID");
        
        Long completedBookings = bookingRepository.countByStatus("COMPLETED");
        Long pendingApprovals = chefRepository.countByStatus("PENDING");
        
        // Calculate today's metrics
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long newSignupsToday = userRepository.countNewUsersFromDate(todayStart);
        Long bookingsToday = bookingRepository.countBookingsFromDate(todayStart);
        BigDecimal revenueToday = Optional.ofNullable(
                customerTransactionRepository.findRevenueByDate(LocalDate.now())
        ).orElse(BigDecimal.ZERO);
        
        // Calculate customer satisfaction (average rating across platform)
        // Get real customer satisfaction from completed bookings with ratings
        BigDecimal totalRatingSum = bookingRepository.findTotalRatingSum();
        Long totalRatingsCount = bookingRepository.countTotalRatings();
        BigDecimal customerSatisfaction = (totalRatingsCount > 0) ? 
            totalRatingSum.divide(BigDecimal.valueOf(totalRatingsCount), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.valueOf(4.0);
        
        // Determine system health based on growth and activity
        String systemHealth = "Excellent";
        if (platformGrowth < -10) {
            systemHealth = "Critical";
        } else if (platformGrowth < -5) {
            systemHealth = "Warning";
        } else if (platformGrowth < 5) {
            systemHealth = "Good";
        }
        
        // Calculate chef retention rate based on active vs total chefs
        Long totalChefsForRetention = userRepository.countByRole("ROLE_CHEF");
        Long activeChefsForRetention = chefRepository.countByStatus("ACTIVE");
        Double chefRetentionRate = (totalChefsForRetention > 0) ? 
            (activeChefsForRetention.doubleValue() / totalChefsForRetention.doubleValue()) * 100 : 0.0;
        
        return AdminOverviewDto.builder()
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .systemCommission(systemCommission)
                .totalPayouts(totalPayouts)
                .paymentIn(paymentIn)
                .refund(refund)
                .totalDiscount(totalDiscount)
                .totalDeposit(totalDeposit)
                .totalPayout(totalPayout)
                .totalDepositPaid(totalDepositPaid)
                .totalUsers(totalUsers)
                .totalChefs(totalChefs)
                .totalCustomers(totalCustomers)
                .activeBookings(activeBookings)
                .completedBookings(completedBookings)
                .pendingApprovals(pendingApprovals)
                .platformGrowth(platformGrowth)
                .chefRetentionRate(chefRetentionRate)
                .customerSatisfaction(customerSatisfaction.doubleValue())
                .systemHealth(systemHealth)
                .newSignupsToday(newSignupsToday)
                .bookingsToday(bookingsToday)
                .revenueToday(revenueToday)
                .build();
    }

    @Override
    public UserStatisticsDto getUserStatistics() {
        return UserStatisticsDto.builder()
                .totalUsers(userRepository.countActiveUsers())
                .totalCustomers(userRepository.countByRole("ROLE_CUSTOMER"))
                .totalChefs(userRepository.countByRole("ROLE_CHEF"))
                .activeChefs(chefRepository.countByStatus("ACTIVE"))
                .pendingChefs(chefRepository.countByStatus("PENDING"))
                .bannedUsers(userRepository.countByIsBannedTrue())
                .premiumUsers(userRepository.countByIsPremiumTrue())
                .newUsersThisMonth(0L) // Will implement later with date filtering
                .userGrowthChart(new ArrayList<>()) // Will implement later
                .build();
    }

    @Override
    public BookingStatisticsDto getBookingStatistics() {
        BigDecimal averageBookingValue = bookingRepository.findAverageBookingValue();
        if (averageBookingValue == null) {
            averageBookingValue = BigDecimal.ZERO;
        }
        
        return BookingStatisticsDto.builder()
                .totalBookings(bookingRepository.countActiveBookings())
                .completedBookings(bookingRepository.countByStatus("COMPLETED"))
                .canceledBookings(bookingRepository.countByStatus("CANCELED"))
                .pendingBookings(bookingRepository.countByStatus("PENDING"))
                .confirmedBookings(bookingRepository.countByStatus("CONFIRMED") + 
                                 bookingRepository.countByStatus("CONFIRMED_PAID") +
                                 bookingRepository.countByStatus("CONFIRMED_PARTIALLY_PAID"))
                .averageBookingValue(averageBookingValue)
                .bookingTrends(new ArrayList<>()) // Will implement later
                .topChefs(new ArrayList<>()) // Will implement later
                .build();
    }

    @Override
    public ChefOverviewDto getChefOverview(Long chefUserId) {
        Chef chef = chefRepository.findByUserId(chefUserId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found"));

        // Calculate earnings metrics
        BigDecimal totalEarnings = chefTransactionRepository.findTotalEarningsByChef(chefUserId);
        if (totalEarnings == null) totalEarnings = BigDecimal.ZERO;

        // Calculate monthly earnings (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal monthlyEarnings = chefTransactionRepository.findEarningsByChefFromDate(chefUserId, thirtyDaysAgo);
        if (monthlyEarnings == null) monthlyEarnings = BigDecimal.ZERO;

        // Calculate weekly earnings (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        BigDecimal weeklyEarnings = chefTransactionRepository.findEarningsByChefFromDate(chefUserId, sevenDaysAgo);
        if (weeklyEarnings == null) weeklyEarnings = BigDecimal.ZERO;

        // Calculate today's earnings
        BigDecimal todayEarnings = chefTransactionRepository.findTodayEarningsByChef(chefUserId);
        if (todayEarnings == null) todayEarnings = BigDecimal.ZERO;

        // Calculate growth percentages
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        BigDecimal previousMonthEarnings = chefTransactionRepository.findEarningsByChefFromDate(chefUserId, sixtyDaysAgo);
        if (previousMonthEarnings != null) {
            previousMonthEarnings = previousMonthEarnings.subtract(monthlyEarnings);
        } else {
            previousMonthEarnings = BigDecimal.ZERO;
        }

        Double monthlyGrowth = 0.0;
        if (previousMonthEarnings.compareTo(BigDecimal.ZERO) > 0) {
            monthlyGrowth = monthlyEarnings.subtract(previousMonthEarnings)
                    .divide(previousMonthEarnings, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Calculate weekly growth
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        BigDecimal previousWeekEarnings = chefTransactionRepository.findEarningsByChefFromDate(chefUserId, fourteenDaysAgo);
        if (previousWeekEarnings != null) {
            previousWeekEarnings = previousWeekEarnings.subtract(weeklyEarnings);
        } else {
            previousWeekEarnings = BigDecimal.ZERO;
        }

        Double weeklyGrowth = 0.0;
        if (previousWeekEarnings.compareTo(BigDecimal.ZERO) > 0) {
            weeklyGrowth = weeklyEarnings.subtract(previousWeekEarnings)
                    .divide(previousWeekEarnings, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Get booking statistics using correct statuses
        Long totalBookings = bookingRepository.countByChefIdExcludingPending(chef.getId());
        Long completedBookings = bookingRepository.countByChefIdAndStatus(chef.getId(), "COMPLETED");
        
        // Upcoming bookings include all confirmed statuses
        Long upcomingBookings = bookingRepository.countByChefIdAndStatus(chef.getId(), "CONFIRMED") +
                               bookingRepository.countByChefIdAndStatus(chef.getId(), "CONFIRMED_PAID") +
                               bookingRepository.countByChefIdAndStatus(chef.getId(), "CONFIRMED_PARTIALLY_PAID");
        
        Long canceledBookings = bookingRepository.countByChefIdAndStatus(chef.getId(), "CANCELED") +
                               bookingRepository.countByChefIdAndStatus(chef.getId(), "OVERDUE") +
                                bookingRepository.countByChefIdAndStatus(chef.getId(),"REJECTED");
        
        Long pendingBookings = bookingRepository.countByChefIdAndStatus(chef.getId(), "PAID") +
                bookingRepository.countByChefIdAndStatus(chef.getId(), "DEPOSITED") +
                bookingRepository.countByChefIdAndStatus(chef.getId(), "PAID_FIRST_CYCLE");

        // Calculate completion rate
        Double completionRate = 0.0;
        if (totalBookings > 0) {
            completionRate = (completedBookings.doubleValue() / totalBookings.doubleValue()) * 100;
        }

        // Get customer and order metrics
        Long totalCustomers = bookingRepository.countUniqueCustomersByChef(chef.getId());
        BigDecimal averageOrderValue = bookingRepository.findAverageOrderValueByChef(chef.getId());
        if (averageOrderValue == null) averageOrderValue = BigDecimal.ZERO;

        // Get review statistics
        BigDecimal averageRating = reviewService.getAverageRatingForChef(chef.getId());
        Long totalReviews = reviewService.getReviewCountForChef(chef.getId());

        // Determine performance status based on multiple factors
        String performanceStatus = "Average";
        if (averageRating != null && completionRate >= 90 && monthlyGrowth >= 10) {
            if (averageRating.compareTo(BigDecimal.valueOf(4.5)) >= 0) {
                performanceStatus = "Excellent";
            } else if (averageRating.compareTo(BigDecimal.valueOf(4.0)) >= 0) {
                performanceStatus = "Good";
            }
        } else if (averageRating != null && averageRating.compareTo(BigDecimal.valueOf(3.0)) < 0) {
            performanceStatus = "Poor";
        }

        // Calculate active hours based on completed bookings
        // Realistic estimate: average 3-4 hours per booking based on booking complexity
        Integer activeHours = Math.toIntExact(completedBookings * 3); // Standard 3 hours per booking estimate

        return ChefOverviewDto.builder()
                .totalEarnings(totalEarnings)
                .monthlyEarnings(monthlyEarnings)
                .weeklyEarnings(weeklyEarnings)
                .todayEarnings(todayEarnings)
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .upcomingBookings(upcomingBookings)
                .canceledBookings(canceledBookings)
                .pendingBookings(pendingBookings)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .reputationPoints(chef.getReputationPoints())
                .performanceStatus(performanceStatus)
                .monthlyGrowth(monthlyGrowth)
                .weeklyGrowth(weeklyGrowth)
                .totalCustomers(totalCustomers)
                .averageOrderValue(averageOrderValue)
                .completionRate(completionRate)
                .activeHours(activeHours)
                .build();
    }
} 