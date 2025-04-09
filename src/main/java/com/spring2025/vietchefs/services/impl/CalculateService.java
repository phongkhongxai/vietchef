package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Menu;
import com.spring2025.vietchefs.models.entity.MenuItem;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceFeeResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceResponse;
import com.spring2025.vietchefs.models.payload.responseModel.TimeTravelResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.DishRepository;
import com.spring2025.vietchefs.repositories.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalculateService {
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private ChefRepository chefRepository;

    @Autowired
    private DistanceService distanceService;
    public BigDecimal calculateChefServiceFee(BigDecimal pricePerHour, BigDecimal totalCookTime) {
        return pricePerHour.multiply(totalCookTime);
    }
    private BigDecimal getCookTimeMultiplier(int numberOfGuests) {
        if (numberOfGuests <= 4) return BigDecimal.valueOf(1.0);    // 4 khách trở xuống thì không thay đổi thời gian
        else if (numberOfGuests <= 6) return BigDecimal.valueOf(1.11);  // Tăng 11% thời gian cho 6 khách
        else if (numberOfGuests <= 8) return BigDecimal.valueOf(1.2);   // Tăng 20% thời gian cho 8 khách
        else if (numberOfGuests <= 10) return BigDecimal.valueOf(1.35); // Tăng 35% thời gian cho 10 khách
        else return BigDecimal.valueOf(1.5);  // 12 khách trở lên thì tăng 50% thời gian
    }

    public  BigDecimal calculateTotalCookTime(List<Long> dishIds, int numberOfGuests) {
        List<Dish> dishes = dishRepository.findAllById(dishIds);

        // Nhóm theo estimatedCookGroup để xử lý từng nhóm khác nhau nếu có
        Map<Integer, List<Dish>> groupedByGroupSize = dishes.stream()
                .collect(Collectors.groupingBy(Dish::getEstimatedCookGroup));

        BigDecimal totalTime = BigDecimal.ZERO;

        for (Map.Entry<Integer, List<Dish>> entry : groupedByGroupSize.entrySet()) {
            int groupSize = entry.getKey();
            List<Dish> groupDishes = entry.getValue();

            // Tổng số món trong nhóm
            int totalDishes = groupDishes.size();

            // Số nhóm đầy đủ để chia
            int fullGroups = totalDishes / groupSize;
            //int remainder = totalDishes % groupSize;

            // Lấy danh sách dish time
            List<BigDecimal> cookTimes = groupDishes.stream()
                    .map(Dish::getCookTime)
                    .toList();

            // Tính cho các nhóm đầy đủ
            for (int i = 0; i < fullGroups * groupSize; i += groupSize) {
                BigDecimal groupTime = BigDecimal.ZERO;
                for (int j = 0; j < groupSize; j++) {
                    groupTime = groupTime.add(cookTimes.get(i + j));
                }
                totalTime = totalTime.add(groupTime.divide(BigDecimal.valueOf(groupSize), 2, RoundingMode.HALF_UP));
            }

            // Tính phần dư (món lẻ nấu riêng)
            for (int i = fullGroups * groupSize; i < totalDishes; i++) {
                totalTime = totalTime.add(cookTimes.get(i));
            }
        }

        // Nhân với multiplier theo số lượng khách
        BigDecimal multiplier = getCookTimeMultiplier(numberOfGuests);
        totalTime = totalTime.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        // Chuyển phút → giờ
        return totalTime.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
    public BigDecimal calculateTotalCookTimeFromMenu(Long menuId, List<Long> dishIds, int guestCount) {
        BigDecimal totalCookTime = BigDecimal.ZERO;

        // Nếu menuId có giá trị, lấy tổng thời gian nấu từ Menu
        if (menuId != null) {
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));

            // Lấy thời gian nấu tổng của menu
            totalCookTime = menu.getTotalCookTime();  // Giả sử Menu có trường totalCookTime
        }

        // Kiểm tra và tính thêm thời gian nấu cho các món không có trong menu
        if (dishIds != null && !dishIds.isEmpty()) {
            Set<Long> uniqueDishIds = new HashSet<>(dishIds);

            // Nếu đã có menuId, loại bỏ những món đã có trong menu
            if (menuId != null) {
                Menu menu = menuRepository.findById(menuId)
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));

                List<Long> menuDishIds = menu.getMenuItems().stream()
                        .map(item -> item.getDish().getId())
                        .toList();

                // Loại bỏ những món đã có trong menu
                menuDishIds.forEach(uniqueDishIds::remove);
            }

            // Tính tổng thời gian nấu cho các món còn lại chưa có trong menu
            if (!uniqueDishIds.isEmpty()) {
                totalCookTime = totalCookTime.add(calculateTotalCookTime(new ArrayList<>(uniqueDishIds), guestCount));
            }
        }

        return totalCookTime;
    }

    public BigDecimal calculateMaxCookTime(Long chefId, int maxNumberOfDishes, int numberOfGuests) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));

        List<Dish> allDishes = dishRepository.findByChefAndIsDeletedFalse(chef);

        // --- Lấy menu có thời gian nấu lâu nhất ---
        List<Menu> chefMenus = menuRepository.findByChef(chef);
        Optional<Menu> maxCookTimeMenu = chefMenus.stream()
                .max(Comparator.comparing(Menu::getTotalCookTime));

        List<Dish> menuDishes = new ArrayList<>();
        if (maxCookTimeMenu.isPresent()) {
            menuDishes = maxCookTimeMenu.get().getMenuItems().stream()
                    .map(MenuItem::getDish)
                    .toList();
        }

        // --- Món KHÔNG có trong menu ---
        Set<Long> menuDishIds = menuDishes.stream()
                .map(Dish::getId)
                .collect(Collectors.toSet());

        List<Dish> notInMenu = allDishes.stream()
                .filter(d -> !menuDishIds.contains(d.getId()))
                .toList();

        // Lấy 1 món group=1, lâu nhất
        Optional<Dish> g1Dish = notInMenu.stream()
                .filter(d -> d.getEstimatedCookGroup() == 1)
                .max(Comparator.comparing(Dish::getCookTime));

        // Lấy 2 món group=2, lâu nhất
        List<Dish> g2Dishes = notInMenu.stream()
                .filter(d -> d.getEstimatedCookGroup() == 2)
                .sorted((a, b) -> b.getCookTime().compareTo(a.getCookTime()))
                .limit(2)
                .toList();

        // --- Tính thời gian nấu ---
        BigDecimal totalTime = BigDecimal.ZERO;
        if (maxCookTimeMenu.isPresent()) {
            BigDecimal menuCookTimeInMinutes = maxCookTimeMenu.get().getTotalCookTime()
                    .multiply(BigDecimal.valueOf(60));
            totalTime = totalTime.add(menuCookTimeInMinutes);
        }
        if (g1Dish.isPresent()) {
            totalTime = totalTime.add(g1Dish.get().getCookTime());
        }

        if (!g2Dishes.isEmpty()) {
            BigDecimal g2Total = g2Dishes.stream()
                    .map(Dish::getCookTime)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalTime = totalTime.add(g2Total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
        }

        // --- Nhân hệ số theo số khách ---
        BigDecimal multiplier = getCookTimeMultiplier(numberOfGuests);
        totalTime = totalTime.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        if (totalTime.compareTo(BigDecimal.valueOf(210)) > 0) {
            totalTime = BigDecimal.valueOf(210);
        }
        if (totalTime.compareTo(BigDecimal.valueOf(60)) < 0) {
            totalTime = BigDecimal.valueOf(60);
        }

        // --- Trả về thời gian giờ ---
        return totalTime.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDishPrice(Long menuId, int guestCount, List<Long> extraDishIds) {
        BigDecimal totalDishPrice = BigDecimal.ZERO;

        // Nếu khách hàng đặt theo menu
        if (menuId != null) {
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));

            // Tính tổng giá các món trong menu
            BigDecimal menuTotalPrice = menu.getMenuItems().stream()
                    .map(item -> item.getDish().getBasePrice().multiply(BigDecimal.valueOf(guestCount)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add); // Cộng tổng giá

            // Áp dụng giảm giá nếu có
            if (menu.getHasDiscount() && menu.getDiscountPercentage() != null) {
                BigDecimal discount = BigDecimal.valueOf(menu.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
                menuTotalPrice = menuTotalPrice.multiply(BigDecimal.ONE.subtract(discount));
            }

            totalDishPrice = totalDishPrice.add(menuTotalPrice);
        }
    if(extraDishIds!=null && !extraDishIds.isEmpty()){
    for (Long dishId : extraDishIds) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found"));

        BigDecimal dishPrice = dish.getBasePrice()
                .multiply(BigDecimal.valueOf(guestCount));

        totalDishPrice = totalDishPrice.add(dishPrice);
    }
}


        return totalDishPrice;
    }

    public DistanceFeeResponse calculateTravelFee(String chefAddress, String customerLocation) {
        DistanceResponse distanceAndTime = distanceService.calculateDistanceAndTime(chefAddress, customerLocation);
        if (distanceAndTime.getDistanceKm().compareTo(BigDecimal.ZERO) == 0 &&
                distanceAndTime.getDurationHours().compareTo(BigDecimal.ZERO) == 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Address not found.");
        }
        BigDecimal travelFee;
        if (distanceAndTime.getDistanceKm().compareTo(BigDecimal.valueOf(3)) < 0) {
            travelFee = BigDecimal.ZERO; // dưới 3km free cost
        } else {
            travelFee = distanceAndTime.getDistanceKm().multiply(BigDecimal.valueOf(0.7)); // 0.7 đô/km
        }

        DistanceFeeResponse distanceFee = new DistanceFeeResponse();
        distanceFee.setDistanceKm(distanceAndTime.getDistanceKm());
        distanceFee.setDurationHours(distanceAndTime.getDurationHours());
        distanceFee.setTravelFee(travelFee);
        return distanceFee;
    }

    public BigDecimal calculateFinalPrice(BigDecimal price1, BigDecimal price2, BigDecimal price3) {
        BigDecimal price1Final = price1.multiply(BigDecimal.valueOf(1.25));// Cộng thêm 20% phí nền tảng
        return price1Final.add(price2).add(price3);
    }

    public TimeTravelResponse calculateArrivalTime(LocalTime starTime, BigDecimal totalTimeCook, BigDecimal travelTime) {

        long cookSeconds = totalTimeCook.multiply(BigDecimal.valueOf(3600)).longValue();
        long travelSeconds = travelTime.multiply(BigDecimal.valueOf(3600)).longValue();


        TimeTravelResponse timeTravelResponse = new TimeTravelResponse();
        timeTravelResponse.setTimeBeginCook(starTime.minusSeconds(600+cookSeconds));
        timeTravelResponse.setTimeBeginTravel(starTime.minusSeconds(cookSeconds+travelSeconds+600));

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

        return servingHours.multiply(pricePerHour.multiply(BigDecimal.valueOf(0.6)));
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Bán kính trái đất tính bằng km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // Khoảng cách theo km
        return distance;
    }



}
