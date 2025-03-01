package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailPriceRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingPriceRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.DistanceFeeResponse;
import com.spring2025.vietchefs.models.payload.responseModel.ReviewSingleBookingResponse;
import com.spring2025.vietchefs.models.payload.responseModel.TimeTravelResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.BookingService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailItemRepository bookingDetailItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private BookingDetailService bookingDetailService;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public BookingResponseDto createSingleBooking(BookingRequestDto dto) {
        User customer = userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Customer not found"));
        Chef chef = chefRepository.findById(dto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setChef(chef);
        booking.setBookingType("SINGLE");
        booking.setStatus("PENDING");
        booking.setRequestDetails(dto.getRequestDetails());
        booking.setTotalPrice(BigDecimal.ZERO);
        booking.setGuestCount(dto.getGuestCount());
        booking.setIsDeleted(false);
        booking = bookingRepository.save(booking);

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (BookingDetailRequestDto detailDto : dto.getBookingDetails()) {
            BookingDetail detail = bookingDetailService.createBookingDetail(booking, detailDto);
            totalPrice = totalPrice.add(detail.getTotalPrice());
        }

        booking.setTotalPrice(totalPrice);
        Booking booking1 = bookingRepository.save(booking);

        return modelMapper.map(booking1, BookingResponseDto.class);
    }

    @Override
    public ReviewSingleBookingResponse calculateFinalPriceForSingleBooking(BookingPriceRequestDto dto) {
        Chef chef = chefRepository.findById(dto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));

        BigDecimal totalBookingPrice = BigDecimal.ZERO;
        ReviewSingleBookingResponse reviewSingleBookingResponse = new ReviewSingleBookingResponse();

        for (BookingDetailPriceRequestDto detailDto : dto.getBookingDetails()) {
            BigDecimal totalCookTime = BigDecimal.ZERO;
            if (detailDto.getMenuId() != null || (detailDto.getExtraDishIds() != null && !detailDto.getExtraDishIds().isEmpty())) {
                List<Long> dishIds = new ArrayList<>();

                if (detailDto.getMenuId() != null) {
                    Menu menu = menuRepository.findById(detailDto.getMenuId())
                            .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));
                    dishIds.addAll(menu.getMenuItems().stream()
                            .map(item -> item.getDish().getId())
                            .collect(Collectors.toList()));
                }

                if (detailDto.getExtraDishIds() != null && !detailDto.getExtraDishIds().isEmpty()) {
                    dishIds.addAll(detailDto.getExtraDishIds());
                }

                if (!dishIds.isEmpty()) {
                    totalCookTime = calculateService.calculateTotalCookTime(dishIds);
                } else {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "At least one dish must be selected.");
                }
            }
            // 🔹 Tính phí dịch vụ đầu bếp (công nấu ăn)
            BigDecimal price1 = calculateService.calculateChefServiceFee(chef.getPrice(), totalCookTime);

            // 🔹 Tính phí món ăn (menu hoặc món lẻ)
            BigDecimal price2 = calculateService.calculateDishPrice(detailDto);

            // 🔹 Tính phí di chuyển
            DistanceFeeResponse price3Of = calculateService.calculateTravelFee(chef.getUser().getAddress(), detailDto.getLocation());
            BigDecimal price3 = price3Of.getTravelFee();
            TimeTravelResponse ttp = calculateService.calculateArrivalTime(detailDto.getStartTime(), totalCookTime, price3Of.getDurationHours());

            //  Nếu khách chọn phục vụ, tính thêm phí phục vụ
            BigDecimal servingFee = BigDecimal.ZERO;
            if (dto.getIsServing()) {
                servingFee = calculateService.calculateServingFee(detailDto.getStartTime(), detailDto.getEndTime(), chef.getPrice());
            }

            // 🔹 Tính tổng giá của BookingDetail
            BigDecimal price4 = calculateService.calculateFinalPrice(price1, price2, price3).add(servingFee);

            totalBookingPrice = totalBookingPrice.add(price4);
            reviewSingleBookingResponse.setTotalPrice(totalBookingPrice);
            reviewSingleBookingResponse.setTimeBeginTravel(ttp.getTimeBeginTravel());
            reviewSingleBookingResponse.setTimeBeginCook(ttp.getTimeBeginCook());

        }

        return reviewSingleBookingResponse;
    }
}
