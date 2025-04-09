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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private ChefTransactionRepository chefTransactionRepository;
    @Autowired
    private PaymentCycleService paymentCycleService;

    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public BookingDetail createBookingDetail(Booking booking, BookingDetailRequestDto dto) {
        BookingDetail detail =  new BookingDetail();
        detail.setBooking(booking);
        detail.setSessionDate(dto.getSessionDate());
        detail.setStartTime(dto.getStartTime());
        detail.setLocation(dto.getLocation());
        detail.setArrivalFee(dto.getArrivalFee());
        detail.setChefCookingFee(dto.getChefCookingFee());
        detail.setPriceOfDishes(dto.getPriceOfDishes());
        detail.setIsDeleted(false);
        detail.setTotalCookTime(dto.getTotalCookTime().divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        detail.setIsUpdated(dto.getIsUpdated());
        detail.setTimeBeginCook(dto.getTimeBeginCook());
        detail.setTimeBeginTravel(dto.getTimeBeginTravel());
        detail.setTotalChefFeePrice(dto.getTotalChefFeePrice());
        detail.setPlatformFee(dto.getPlatformFee());
        detail.setTotalCookTime(dto.getTotalCookTime());
        detail.setDiscountAmout(dto.getDiscountAmout() != null ? dto.getDiscountAmout() : BigDecimal.ZERO);
        detail.setMenuId(dto.getMenuId() != null ? dto.getMenuId() : null);
        List<BookingDetailItem> dishes = Optional.ofNullable(dto.getDishes())
                .orElse(Collections.emptyList())
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
            if (dto.getMenuId() != null) {
                totalCookTime = calculateService.calculateTotalCookTimeFromMenu(dto.getMenuId(), dishIds, bookingDetail.getBooking().getGuestCount());
            } else {
                totalCookTime = calculateService.calculateTotalCookTime(dishIds, bookingDetail.getBooking().getGuestCount());
            }
        }

        BigDecimal cookingFee = calculateService.calculateChefServiceFee(chef.getPrice(), totalCookTime);

        BigDecimal dishPrice = calculateService.calculateDishPrice(dto.getMenuId(), bookingDetail.getBooking().getGuestCount(), dto.getExtraDishIds());
        BigDecimal platformFee = cookingFee.multiply(BigDecimal.valueOf(0.25))  // 25% của cookingFee
                .add(dishPrice.multiply(BigDecimal.valueOf(0.20))); // 20% của dishPrice

        DistanceFeeResponse travelFeeResponse = calculateService.calculateTravelFee(chef.getAddress(), bookingDetail.getLocation());
        BigDecimal travelFee = travelFeeResponse.getTravelFee();
        TimeTravelResponse timeTravelResponse = calculateService.calculateArrivalTime(bookingDetail.getStartTime(), totalCookTime, travelFeeResponse.getDurationHours());


        BigDecimal discountAmountDetail = BigDecimal.ZERO;
        BigDecimal totalChefFeePrice = cookingFee.add(dishPrice.multiply(BigDecimal.valueOf(0.8))).add(travelFee) ;
        BigDecimal totalPrice = calculateService.calculateFinalPrice(cookingFee, dishPrice, travelFee);
        if(bookingDetail.getBooking().getBookingPackage().getDiscount()!=null){
            discountAmountDetail = platformFee.multiply(bookingDetail.getBooking().getBookingPackage().getDiscount());
            totalPrice = totalPrice.subtract(discountAmountDetail);
        }

        reviewResponse.setChefCookingFee(cookingFee);
        reviewResponse.setTotalCookTime(totalCookTime);
        reviewResponse.setPriceOfDishes(dishPrice);
        reviewResponse.setArrivalFee(travelFee);
        reviewResponse.setPlatformFee(platformFee);
        reviewResponse.setTotalChefFeePrice(totalChefFeePrice);
        reviewResponse.setDiscountAmout(discountAmountDetail);
        reviewResponse.setTotalPrice(totalPrice);
        reviewResponse.setMenuId(dto.getMenuId());
        reviewResponse.setTimeBeginTravel(timeTravelResponse.getTimeBeginTravel());
        reviewResponse.setTimeBeginCook(timeTravelResponse.getTimeBeginCook());

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
        bookingDetail.setPriceOfDishes(bookingDetailUpdateRequest.getPriceOfDishes());
        bookingDetail.setTimeBeginTravel(bookingDetailUpdateRequest.getTimeBeginTravel());
        bookingDetail.setTimeBeginCook(bookingDetailUpdateRequest.getTimeBeginCook());
        bookingDetail.setTotalPrice(bookingDetailUpdateRequest.getTotalPrice());
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

    @Override
    public BookingDetailDto updateStatusBookingDetailWatingCompleted(Long bookingDetailId, Long userId) {
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));
        LocalTime completedTime = LocalTime.now();
        BookingDetail bookingDetail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "BookingDetail not found"));
        if(!Objects.equals(chef.getId(), bookingDetail.getBooking().getChef().getId())){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot change this status because you not in this booking.");
        }
        if (!completedTime.isBefore(bookingDetail.getTimeBeginCook()) && !completedTime.isAfter(bookingDetail.getStartTime()) &&bookingDetail.getStatus().equalsIgnoreCase("IN_PROGRESS")) {
            bookingDetail.setStatus("WAITING_FOR_CONFIRMATION");
            bookingDetail = bookingDetailRepository.save(bookingDetail);
        }
        return modelMapper.map(bookingDetail, BookingDetailDto.class);
    }

    @Override
    @Transactional
    public BookingDetailDto confirmBookingCompletionByCustomer(Long bookingDetailId,Long userId) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "BookingDetail not found"));

        Booking booking = bookingDetail.getBooking();
        if(!Objects.equals(booking.getCustomer().getId(), userId)){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot complete this booking because you not in booking.");
        }

        if (!"WAITING_FOR_CONFIRMATION".equalsIgnoreCase(bookingDetail.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot complete this booking. Current status: " + bookingDetail.getStatus());
        }

        bookingDetail.setStatus("COMPLETED");
        bookingDetail = bookingDetailRepository.save(bookingDetail);
        if(booking.getBookingType().equalsIgnoreCase("SINGLE")){
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);
        }else {
            boolean allDetailsCompleted = booking.getBookingDetails()
                    .stream()
                    .allMatch(detail -> "COMPLETED".equalsIgnoreCase(detail.getStatus()));

            if (allDetailsCompleted) {
                booking.setStatus("COMPLETED");
                bookingRepository.save(booking);
            }
        }


        // 2. Update wallet balance
        Chef chef = booking.getChef();
        BigDecimal amountToTransfer = bookingDetail.getTotalChefFeePrice();

        Wallet wallet = walletRepository.findByUserId(chef.getUser().getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef's wallet not found"));

        wallet.setBalance(wallet.getBalance().add(amountToTransfer));
        walletRepository.save(wallet);

        // 3. Create transaction
        ChefTransaction transaction = new ChefTransaction();
        transaction.setWallet(wallet);
        transaction.setBookingDetail(bookingDetail);
        transaction.setTransactionType("CREDIT"); // or use enum
        transaction.setAmount(amountToTransfer);
        transaction.setStatus("COMPLETED");
        transaction.setDescription("Payment for completed bookingDetail #" + bookingDetail.getId());

        chefTransactionRepository.save(transaction);

        return modelMapper.map(bookingDetail, BookingDetailDto.class);
    }

    @Scheduled(cron = "0 1 0 * * ?") // Mỗi ngày vào lúc nửa đêm 12h01
    public void updateBookingDetailsStatus() {
        LocalDate currentDate = LocalDate.now();
        List<BookingDetail> bookingDetails = bookingDetailRepository.findBySessionDateAndStatus(currentDate, "LOCKED");

        for (BookingDetail bookingDetail : bookingDetails) {
                bookingDetail.setStatus("IN_PROGRESS");
                bookingDetailRepository.save(bookingDetail);
        }
    }
}
