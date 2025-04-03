package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateRequest;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.PaymentCycleService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class BookingDetailServiceImpl implements BookingDetailService {
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingDetailItemRepository bookingDetailItemRepository;
    @Autowired
    private PaymentCycleService paymentCycleService;

    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public BookingDetail createBookingDetail(Booking booking, BookingDetailRequestDto dto) {
        BookingDetail detail =  new BookingDetail();
        detail.setBooking(booking);
        detail.setSessionDate(dto.getSessionDate());
        detail.setStartTime(dto.getStartTime());
        detail.setEndTime(dto.getEndTime());
        detail.setLocation(dto.getLocation());
        detail.setIsServing(dto.getIsServing());
        detail.setArrivalFee(dto.getArrivalFee());
        detail.setChefCookingFee(dto.getChefCookingFee());
        detail.setChefServingFee(dto.getChefServingFee());
        detail.setPriceOfDishes(dto.getPriceOfDishes());
        detail.setIsDeleted(false);
        detail.setIsUpdated(dto.getIsUpdated());
        detail.setTimeBeginCook(dto.getTimeBeginCook());
        detail.setTimeBeginTravel(dto.getTimeBeginTravel());
        detail.setTotalChefFeePrice(dto.getTotalChefFeePrice());
        detail.setPlatformFee(dto.getPlatformFee());
        detail.setDiscountAmout(dto.getDiscountAmout() != null ? dto.getDiscountAmout() : BigDecimal.ZERO);
        detail.setMenuId(dto.getMenuId() != null ? dto.getMenuId() : null);
        List<BookingDetailItem> dishes = Optional.ofNullable(dto.getDishes())
                .orElse(Collections.emptyList()) // Nếu null thì thay bằng danh sách rỗng
                .stream()
                .map(itemDto -> {
                    Dish dish = dishRepository.findById(itemDto.getDishId())
                            .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found"));
                    BookingDetailItem detailDish = new BookingDetailItem();
                    detailDish.setBookingDetail(detail);
                    detailDish.setDish(dish);
                    detailDish.setNotes(itemDto.getNotes());
                    return detailDish;
                })
                .collect(Collectors.toList());
        detail.setDishes(dishes);
        detail.setTotalPrice(dto.getTotalPrice());
        return bookingDetailRepository.save(detail);
    }

    @Override
    public BookingDetailDto getBookingDetailById(Long id) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "BookingDetail not found"));
        return modelMapper.map(bookingDetail, BookingDetailDto.class);
    }

    @Override
    public BookingDetailsResponse getBookingDetailByBooking(Long bookingId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Booking not found with id: "+ bookingId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingAndIsDeletedFalse(booking,pageable);

        // get content for page object
        List<BookingDetail> listOfBds = bookingDetails.getContent();

        List<BookingDetailDto> content = listOfBds.stream().map(bt -> modelMapper.map(bt, BookingDetailDto.class)).collect(Collectors.toList());

        BookingDetailsResponse templatesResponse = new BookingDetailsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(bookingDetails.getNumber());
        templatesResponse.setPageSize(bookingDetails.getSize());
        templatesResponse.setTotalElements(bookingDetails.getTotalElements());
        templatesResponse.setTotalPages(bookingDetails.getTotalPages());
        templatesResponse.setLast(bookingDetails.isLast());
        return templatesResponse;
    }

    @Override
    public ReviewBookingDetailResponse calculateUpdatedBookingDetail(Long bookingDetailId, BookingDetailUpdateDto dto) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "BookingDetail not found"));
        if(bookingDetail.getIsUpdated()){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"BookingDetail already updated.");
        }
        Chef chef = bookingDetail.getBooking().getChef();
        BigDecimal totalCookTime = BigDecimal.ZERO;
        ReviewBookingDetailResponse reviewResponse = new ReviewBookingDetailResponse();

        // 🔹 Xử lý danh sách món ăn (menu hoặc món lẻ)
        if (dto.getMenuId() != null || (dto.getExtraDishIds() != null && !dto.getExtraDishIds().isEmpty())) {
            Set<Long> uniqueDishIds = new HashSet<>();

            if (dto.getMenuId() != null) {
                Menu menu = menuRepository.findById(dto.getMenuId())
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));

                if (!menu.getChef().getId().equals(chef.getId())) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Menu does not belong to the selected Chef");
                }

                List<Long> menuDishIds = menu.getMenuItems().stream()
                        .map(item -> item.getDish().getId())
                        .toList();

                uniqueDishIds.addAll(menuDishIds);
            }

            if (dto.getExtraDishIds() != null && !dto.getExtraDishIds().isEmpty()) {
                for (Long extraDishId : dto.getExtraDishIds()) {
                    Dish dish = dishRepository.findById(extraDishId)
                            .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with ID: " + extraDishId));

                    if (!dish.getChef().getId().equals(chef.getId())) {
                        throw new VchefApiException(HttpStatus.BAD_REQUEST, "Dish with ID " + extraDishId + " does not belong to the selected Chef");
                    }

                    uniqueDishIds.add(extraDishId);
                }
            }

            List<Long> dishIds = new ArrayList<>(uniqueDishIds);
            if (!dishIds.isEmpty()) {
                totalCookTime = calculateService.calculateTotalCookTime(dishIds);
            } else {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "At least one dish must be selected.");
            }
        }

        BigDecimal cookingFee = calculateService.calculateChefServiceFee(chef.getPrice(), totalCookTime);
        BigDecimal dishPrice = calculateService.calculateDishPrice(dto.getMenuId(), bookingDetail.getBooking().getGuestCount(), dto.getExtraDishIds());
        DistanceFeeResponse travelFeeResponse = calculateService.calculateTravelFee(chef.getAddress(), bookingDetail.getLocation());
        BigDecimal travelFee = travelFeeResponse.getTravelFee();
        TimeTravelResponse timeTravelResponse = calculateService.calculateArrivalTime(bookingDetail.getStartTime(), totalCookTime, travelFeeResponse.getDurationHours());
        BigDecimal servingFee = BigDecimal.ZERO;
        if (dto.getIsServing()) {
            servingFee = calculateService.calculateServingFee(bookingDetail.getStartTime(), bookingDetail.getEndTime(), chef.getPrice());
        }

        BigDecimal discountAmountDetail = BigDecimal.ZERO;
        BigDecimal totalChefFeePrice = cookingFee.add(dishPrice).add(travelFee).add(servingFee);
        BigDecimal totalPrice = calculateService.calculateFinalPrice(cookingFee, dishPrice, travelFee).add(servingFee);
        if(bookingDetail.getBooking().getBookingPackage().getDiscount()!=null){
            discountAmountDetail = totalPrice.multiply(bookingDetail.getBooking().getBookingPackage().getDiscount());
            totalPrice = totalPrice.subtract(discountAmountDetail);
        }

        reviewResponse.setChefCookingFee(cookingFee);
        reviewResponse.setPriceOfDishes(dishPrice);
        reviewResponse.setArrivalFee(travelFee);
        reviewResponse.setChefServingFee(servingFee);
        reviewResponse.setPlatformFee(cookingFee.multiply(BigDecimal.valueOf(0.12))); // Phí nền tảng 12%
        reviewResponse.setTotalChefFeePrice(totalChefFeePrice);
        reviewResponse.setDiscountAmout(discountAmountDetail);
        reviewResponse.setTotalPrice(totalPrice);
        reviewResponse.setMenuId(dto.getMenuId());
        reviewResponse.setTimeBeginTravel(timeTravelResponse.getTimeBeginTravel());
        reviewResponse.setTimeBeginCook(timeTravelResponse.getTimeBeginCook());
        reviewResponse.setPlatformFee(cookingFee.multiply(BigDecimal.valueOf(0.12)));
        reviewResponse.setTotalChefFeePrice(cookingFee.multiply(BigDecimal.valueOf(0.88)).add(dishPrice).add(travelFee).add(servingFee));
        reviewResponse.setIsServing(dto.getIsServing());
        reviewResponse.setDishes(dto.getDishes());

        return reviewResponse;
    }

    @Override
    @Transactional
    public BookingDetailDto updateBookingDetail(Long bookingDetailId, BookingDetailUpdateRequest bookingDetailUpdateRequest) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "BookingDetail not found"));
        Booking booking = bookingDetail.getBooking();
        if(bookingDetail.getIsUpdated()){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"BookingDetail already updated.");
        }
        // Xóa danh sách món ăn cũ
        if (!bookingDetail.getDishes().isEmpty()) {
            bookingDetail.getDishes().clear();
        }

        List<BookingDetailItem> newDishes = new ArrayList<>();
        for (BookingDetailItemRequestDto dis : bookingDetailUpdateRequest.getDishes()) {
            Dish dish = dishRepository.findById(dis.getDishId())
                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with ID: " + dis.getDishId()));

            BookingDetailItem detailDish = new BookingDetailItem();
            detailDish.setBookingDetail(bookingDetail);
            detailDish.setDish(dish);
            detailDish.setNotes(dis.getNotes());
            newDishes.add(detailDish);
        }

        bookingDetail.getDishes().addAll(newDishes);
        bookingDetail.setMenuId(bookingDetailUpdateRequest.getMenuId());
        bookingDetail.setArrivalFee(bookingDetailUpdateRequest.getArrivalFee());
        bookingDetail.setChefCookingFee(bookingDetailUpdateRequest.getChefCookingFee());
        bookingDetail.setChefServingFee(bookingDetailUpdateRequest.getChefServingFee());
        bookingDetail.setPriceOfDishes(bookingDetailUpdateRequest.getPriceOfDishes());
        bookingDetail.setTimeBeginTravel(bookingDetailUpdateRequest.getTimeBeginTravel());
        bookingDetail.setTimeBeginCook(bookingDetailUpdateRequest.getTimeBeginCook());
        bookingDetail.setTotalPrice(bookingDetailUpdateRequest.getTotalPrice());
        bookingDetail.setIsServing(bookingDetailUpdateRequest.getIsServing());
        bookingDetail.setPlatformFee(bookingDetailUpdateRequest.getPlatformFee());
        bookingDetail.setTotalChefFeePrice(bookingDetailUpdateRequest.getTotalChefFeePrice());
        bookingDetail.setIsUpdated(true);
        bookingDetail.setDiscountAmout(bookingDetailUpdateRequest.getDiscountAmout() != null ? bookingDetailUpdateRequest.getDiscountAmout() : BigDecimal.ZERO);
        BookingDetail ba = bookingDetailRepository.save(bookingDetail);

        BigDecimal newTotalPrice = bookingDetailRepository.calculateTotalPriceByBooking(booking.getId());
        booking.setTotalPrice(newTotalPrice);
        bookingRepository.save(booking);

        // Cập nhật lại PaymentCycle
        paymentCycleService.updatePaymentCycles(booking);


        return modelMapper.map(ba, BookingDetailDto.class);
    }
}
