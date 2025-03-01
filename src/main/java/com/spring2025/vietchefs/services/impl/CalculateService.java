package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Menu;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailPriceRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuItemRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceFeeResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.models.payload.responseModel.TimeTravelResponse;
import com.spring2025.vietchefs.repositories.DishRepository;
import com.spring2025.vietchefs.repositories.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

@Service
public class CalculateService {
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private DistanceService distanceService;
    public BigDecimal calculateChefServiceFee(BigDecimal pricePerHour, BigDecimal totalCookTime) {
        return pricePerHour.multiply(totalCookTime);
    }
    public BigDecimal calculateTotalCookTime(List<Long> menuItemIds) {
        List<BigDecimal> cookTimes = menuItemIds.stream()
                .map(item -> dishRepository.findByIdNotDeleted(item)
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found"))
                        .getCookTime()) //
                .toList();

        if (cookTimes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxCookTime = cookTimes.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal minCookTime = cookTimes.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        return maxCookTime.add(minCookTime).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDishPrice(BookingDetailPriceRequestDto detailDto) {
        BigDecimal totalDishPrice = BigDecimal.ZERO;

        // Nếu khách hàng đặt theo menu
        if (detailDto.getMenuId() != null) {
            Menu menu = menuRepository.findById(detailDto.getMenuId())
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));

            // Tính tổng giá các món trong menu
            BigDecimal menuTotalPrice = menu.getMenuItems().stream()
                    .map(item -> item.getDish().getBasePrice().multiply(BigDecimal.valueOf(detailDto.getGuestCount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add); // Cộng tổng giá

            // Áp dụng giảm giá nếu có
            if (menu.getHasDiscount() && menu.getDiscountPercentage() != null) {
                BigDecimal discount = BigDecimal.valueOf(menu.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
                menuTotalPrice = menuTotalPrice.multiply(BigDecimal.ONE.subtract(discount));
            }

            totalDishPrice = totalDishPrice.add(menuTotalPrice);
        }

        for (Long dishId : detailDto.getExtraDishIds()) {
            Dish dish = dishRepository.findById(dishId)
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found"));

            BigDecimal dishPrice = dish.getBasePrice()
                    .multiply(BigDecimal.valueOf(detailDto.getGuestCount()));

            totalDishPrice = totalDishPrice.add(dishPrice);
        }

        return totalDishPrice;
    }

    public DistanceFeeResponse calculateTravelFee(String chefAddress, String customerLocation) {
        DistanceResponse distanceAndTime = distanceService.calculateDistanceAndTime(chefAddress, customerLocation);
        DistanceFeeResponse distanceFee = new DistanceFeeResponse();
        distanceFee.setDistanceKm(distanceAndTime.getDistanceKm());
        distanceFee.setDurationHours(distanceAndTime.getDurationHours());
        distanceFee.setTravelFee(distanceAndTime.getDistanceKm().multiply(BigDecimal.valueOf(2.5))); //2.5 đô 1km
        return distanceFee;
    }

    public BigDecimal calculateFinalPrice(BigDecimal price1, BigDecimal price2, BigDecimal price3) {
        BigDecimal price1Final = price1.multiply(BigDecimal.valueOf(1.12));// Cộng thêm 12% phí nền tảng
        return price1Final.add(price2).add(price3);
    }

    public TimeTravelResponse calculateArrivalTime(LocalTime starTime, BigDecimal totalTimeCook, BigDecimal travelTime) {

        long cookSeconds = totalTimeCook.multiply(BigDecimal.valueOf(3600)).longValue();
        long travelSeconds = travelTime.multiply(BigDecimal.valueOf(3600)).longValue();


        TimeTravelResponse timeTravelResponse = new TimeTravelResponse();
        timeTravelResponse.setTimeBeginCook(starTime.minusSeconds(1800+cookSeconds));
        timeTravelResponse.setTimeBeginTravel(starTime.minusSeconds(cookSeconds+travelSeconds+1800));

        return timeTravelResponse;
    }

    public BigDecimal calculateServingFee(LocalTime startTime, LocalTime endTime, BigDecimal pricePerHour) {
        if (startTime == null || endTime == null || pricePerHour == null) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Start time, end time, and price per hour are required.");
        }

        long servingMinutes = Duration.between(startTime, endTime).toMinutes();

        if (servingMinutes <= 0) {
            return BigDecimal.ZERO; // Nếu thời gian <= 0 thì không tính phí
        }

        BigDecimal servingHours = BigDecimal.valueOf(servingMinutes).divide(BigDecimal.valueOf(60), RoundingMode.CEILING);

        // Tính giá phục vụ (70% của giá theo giờ)
        BigDecimal servingFee = servingHours.multiply(pricePerHour.multiply(BigDecimal.valueOf(0.7)));

        return servingFee;
    }



}
