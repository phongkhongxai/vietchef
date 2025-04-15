package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailItemRequestDto;
import com.spring2025.vietchefs.models.payload.dto.BookingDetailRequestDto;
import com.spring2025.vietchefs.models.payload.dto.ImageDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailUpdateRequest;
import com.spring2025.vietchefs.models.payload.requestModel.NotificationRequest;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.AvailabilityFinderService;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.ChefService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private UserRepository userRepository;
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
    private NotificationService notificationService;
    @Autowired
    private AvailabilityFinderService availabilityFinderService;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ImageService imageService;
    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;
    @Autowired
    private ChefService chefService;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public BookingDetail createBookingDetail(Booking booking, BookingDetailRequestDto dto) {
        if (isOverlappingWithExistingBookings(booking.getChef(), dto.getSessionDate(), dto.getTimeBeginTravel(), dto.getStartTime())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef đã có lịch trong khoảng thời gian này cho ngày "+dto.getSessionDate()+". Vui lòng chọn khung giờ khác.");
        }
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
        detail.setChefBringIngredients(dto.getChefBringIngredients());
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
    private boolean isOverlappingWithExistingBookings(Chef chef, LocalDate sessionDate, LocalTime timeBeginTravel, LocalTime startTime) {
        // Lấy toàn bộ bookingDetails của chef trong ngày đó
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking_ChefAndSessionDateAndIsDeletedFalse(chef, sessionDate);
        List<BookingDetail> activeBookings = bookingDetails.stream()
                .filter(detail -> {
                    Booking booking = detail.getBooking();
                    return !booking.getIsDeleted() &&
                            !List.of("CANCELED", "OVERDUE").contains(booking.getStatus()) &&
                            !detail.getIsDeleted() &&
                            !List.of("CANCELED", "OVERDUE").contains(detail.getStatus());
                })
                .sorted(Comparator.comparing(BookingDetail::getStartTime))
                .collect(Collectors.toList());

        // Tính khoảng thời gian cần kiểm tra (cho phép lố 10 phút)
        LocalTime checkStart = timeBeginTravel.minusSeconds(10);
        LocalTime checkEnd = startTime.plusMinutes(10);

        for (BookingDetail detail : activeBookings) {
            LocalTime existingStart = detail.getTimeBeginTravel();
            LocalTime existingEnd = detail.getStartTime();

            boolean isOverlap = !(checkEnd.isBefore(existingStart) || checkStart.isAfter(existingEnd));
            if (isOverlap) {
                return true;
            }
        }

        return false;
    }


    @Override
    public BookingDetailResponse getBookingDetailById(Long id) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "BookingDetail not found"));
        BookingDetailResponse dto = modelMapper.map(bookingDetail, BookingDetailResponse.class);
        List<Image> images = imageService.getImagesByEntity("BOOKING_DETAIL", bookingDetail.getId());
        List<ImageDto> imageDtos = images.stream()
                .map(image -> modelMapper.map(image, ImageDto.class))
                .collect(Collectors.toList());
        dto.setImages(imageDtos);
        return dto;
    }

    @Override
    public BookingDetailsResponse getBookingDetailsByChef(Long chefId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found with id: "+ chefId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<BookingDetail> bookingDetails = bookingDetailRepository.findByChefIdAndNotDeleted(chef.getId(),pageable);

        // get content for page object
        List<BookingDetail> listOfBds = bookingDetails.getContent();

        List<BookingDetailResponse> content = listOfBds.stream().map(bd -> {
            BookingDetailResponse dto = modelMapper.map(bd, BookingDetailResponse.class);
            List<Image> images = imageService.getImagesByEntity("BOOKING_DETAIL", bd.getId());
            List<ImageDto> imageDtos = images.stream()
                    .map(img -> modelMapper.map(img, ImageDto.class))
                    .collect(Collectors.toList());
            dto.setImages(imageDtos);
            return dto;
        }).collect(Collectors.toList());



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
    public BookingDetailsResponse getBookingDetailsByCustomer(Long customerId, int pageNo, int pageSize, String sortBy, String sortDir) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Customer not found with id: "+ customerId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<BookingDetail> bookingDetails = bookingDetailRepository.findByCustomerIdAndNotDeleted(customer.getId(),pageable);

        // get content for page object
        List<BookingDetail> listOfBds = bookingDetails.getContent();

        List<BookingDetailResponse> content = listOfBds.stream().map(bd -> {
            BookingDetailResponse dto = modelMapper.map(bd, BookingDetailResponse.class);
            List<Image> images = imageService.getImagesByEntity("BOOKING_DETAIL", bd.getId());
            List<ImageDto> imageDtos = images.stream()
                    .map(img -> modelMapper.map(img, ImageDto.class))
                    .collect(Collectors.toList());
            dto.setImages(imageDtos);
            return dto;
        }).collect(Collectors.toList());

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

        List<BookingDetailResponse> content = listOfBds.stream().map(bt -> modelMapper.map(bt, BookingDetailResponse.class)).collect(Collectors.toList());

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
        if(travelFeeResponse.getDistanceKm().compareTo(BigDecimal.valueOf(50))>0){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Distance between you and chef cannot bigger than 50km.");
        }
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
        reviewResponse.setChefBringIngredients(dto.getChefBringIngredients());
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
        bookingDetail.setChefBringIngredients(bookingDetailUpdateRequest.getChefBringIngredients());
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
    public BookingDetailDto updateStatusBookingDetailWatingCompleted(Long bookingDetailId, Long userId,List<MultipartFile> files) {
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));
        LocalTime completedTime = LocalTime.now();
        BookingDetail bookingDetail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "BookingDetail not found"));
        if(!Objects.equals(chef.getId(), bookingDetail.getBooking().getChef().getId())){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot change this status because you not in this booking.");
        }
        if (files == null || files.isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Phải upload ít nhất 1 ảnh.");
        }
        if (files.size() > 2) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chỉ được upload tối đa 2 ảnh.");
        }
        if (bookingDetail.getStatus().equalsIgnoreCase("IN_PROGRESS")
                && !completedTime.isBefore(bookingDetail.getTimeBeginCook())
                ) {
            for (MultipartFile file : files) {
                try {
                    imageService.uploadImage(file, bookingDetail.getId(), "BOOKING_DETAIL");
                } catch (IOException e) {
                    throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi upload ảnh: " + e.getMessage());
                }
            }
            bookingDetail.setStatus("WAITING_FOR_CONFIRMATION");
            bookingDetail = bookingDetailRepository.save(bookingDetail);
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(bookingDetail.getBooking().getCustomer().getId())
                    .title("Booking Completed")
                    .body("The chef has completed your service and is waiting for your confirmation.")
                    .bookingDetailId(bookingDetail.getId())
                    .screen("BookingDetail")
                    .build();
            notificationService.sendPushNotification(notification);
            chefService.updateReputation(chef, 1);

        }else {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot update status. Current time is not within the allowed time window.");
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
            booking = bookingRepository.save(booking);
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(booking.getCustomer().getId())
                    .title("Booking Completed")
                    .body("Completed Booking Single with Chef "+ booking.getChef().getUser().getFullName()+".")
                    .bookingId(booking.getId())
                    .screen("Booking")
                    .build();
            notificationService.sendPushNotification(notification);
        }else {
            List<BookingDetail> refreshedDetails = bookingDetailRepository.findByBookingId(booking.getId());
            boolean allDetailsCompleted = refreshedDetails.stream()
                    .filter(detail -> !"CANCELED".equalsIgnoreCase(detail.getStatus()))
                    .allMatch(detail -> "COMPLETED".equalsIgnoreCase(detail.getStatus()));

            if (allDetailsCompleted) {
                booking.setStatus("COMPLETED");
                if(booking.getDepositPaid().compareTo(BigDecimal.ZERO) >0){
                    Wallet walletCus = walletRepository.findByUserId(userId)
                            .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Customer's wallet not found"));
                    walletCus.setBalance(walletCus.getBalance().add(booking.getDepositPaid()));
                    walletRepository.save(walletCus);

                    CustomerTransaction refundTransaction = CustomerTransaction.builder()
                            .wallet(walletCus)
                            .booking(booking)
                            .transactionType("REFUND")
                            .amount(booking.getDepositPaid())
                            .description("Refund for completed booking with Chef "+booking.getChef().getUser().getFullName()+".")
                            .status("COMPLETED")
                            .isDeleted(false)
                            .build();
                    customerTransactionRepository.save(refundTransaction);
                    booking.setDepositPaid(BigDecimal.ZERO);
                    NotificationRequest notification = NotificationRequest.builder()
                            .userId(booking.getCustomer().getId())
                            .title("Booking Completed")
                            .body("Refund deposit paid for completed Booking with Chef "+ booking.getChef().getUser().getFullName()+".")
                            .screen("Wallet")
                            .build();
                    notificationService.sendPushNotification(notification);
                }
                booking =bookingRepository.save(booking);

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
        transaction.setTransactionType("CREDIT");
        transaction.setAmount(amountToTransfer);
        transaction.setStatus("COMPLETED");
        transaction.setDescription("Payment for completed bookingDetail #" + bookingDetail.getId());
        chefTransactionRepository.save(transaction);
        NotificationRequest notification = NotificationRequest.builder()
                .userId(booking.getChef().getUser().getId())
                .title("Booking Completed")
                .body("The customer has confirmed the completion of your service.")
                .bookingId(booking.getId())
                .screen("ChefEarningsScreen")
                .build();
        notificationService.sendPushNotification(notification);


        return modelMapper.map(bookingDetail, BookingDetailDto.class);
    }

    @Scheduled(cron = "0 1 0 * * ?") // Mỗi ngày vào lúc nửa đêm 12h01
    public void updateBookingDetailsStatus() {
        LocalDate currentDate = LocalDate.now();
        List<BookingDetail> bookingDetails = bookingDetailRepository.findBySessionDateAndStatusAndIsDeletedFalse(currentDate, "SCHEDULED_COMPLETE");

        for (BookingDetail bookingDetail : bookingDetails) {
            bookingDetail.setStatus("IN_PROGRESS");
            bookingDetailRepository.save(bookingDetail);

            Booking booking = bookingDetail.getBooking();
            Chef chef = booking.getChef();
            Long chefUserId = chef.getUser().getId();

            NotificationRequest chefNotification = NotificationRequest.builder()
                    .userId(chefUserId)
                    .title("Cooking Session Started")
                    .body("Your scheduled session for " + currentDate + " has started. Time to get cooking!")
                    .bookingDetailId(booking.getId())
                    .screen("nothing")
                    .build();
            notificationService.sendPushNotification(chefNotification);
            // Send notification to the Customer
            Long customerUserId = booking.getCustomer().getId();
            NotificationRequest customerNotification = NotificationRequest.builder()
                    .userId(customerUserId)
                    .title("Your Booking Has Started")
                    .body("Your cooking session for " + currentDate + " has started. Get ready to enjoy your meal!")
                    .bookingDetailId(booking.getId())
                    .screen("nothing")
                    .build();
            notificationService.sendPushNotification(customerNotification);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // chạy mỗi giờ
    @Transactional
    public void autoCompleteBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<BookingDetail> pendingDetails = bookingDetailRepository
                .findAllByStatusAndIsDeletedFalse("WAITING_FOR_CONFIRMATION");

        for (BookingDetail detail : pendingDetails) {
            LocalDateTime sessionDateTime = LocalDateTime.of(detail.getSessionDate(), detail.getStartTime());

            if (sessionDateTime.plusHours(12).isBefore(now)) {
                try {
                    completeBookingDetail(detail);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private void completeBookingDetail(BookingDetail detail) {
        Booking booking = detail.getBooking();

        detail.setStatus("COMPLETED");
        detail = bookingDetailRepository.save(detail);

        // Nếu là SINGLE -> set booking hoàn thành
        if ("SINGLE".equalsIgnoreCase(booking.getBookingType())) {
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);
            NotificationRequest notification = NotificationRequest.builder()
                    .userId(booking.getCustomer().getId())
                    .title("Booking Completed")
                    .body("Completed Booking Single with Chef "+ booking.getChef().getUser().getFullName()+".")
                    .bookingId(booking.getId())
                    .screen("Booking")
                    .build();
            notificationService.sendPushNotification(notification);
        } else {
            // LOAD lại danh sách từ DB
            List<BookingDetail> refreshedDetails = bookingDetailRepository.findByBookingId(booking.getId());
            boolean allCompleted = refreshedDetails.stream()
                    .filter(d -> !"CANCELED".equalsIgnoreCase(d.getStatus()))
                    .allMatch(d -> "COMPLETED".equalsIgnoreCase(d.getStatus()));

            if (allCompleted) {
                booking.setStatus("COMPLETED");

                // Hoàn trả cọc nếu có
                if (booking.getDepositPaid().compareTo(BigDecimal.ZERO) > 0) {
                    Wallet cusWallet = walletRepository.findByUserId(booking.getCustomer().getId())
                            .orElseThrow(() -> new RuntimeException("Customer wallet not found"));

                    cusWallet.setBalance(cusWallet.getBalance().add(booking.getDepositPaid()));
                    walletRepository.save(cusWallet);

                    // Tạo transaction hoàn cọc
                    CustomerTransaction refundTransaction = CustomerTransaction.builder()
                            .wallet(cusWallet)
                            .booking(booking)
                            .transactionType("REFUND")
                            .amount(booking.getDepositPaid())
                            .description("Auto refund for completed booking with Chef " + booking.getChef().getUser().getFullName())
                            .status("COMPLETED")
                            .isDeleted(false)
                            .build();
                    NotificationRequest notification = NotificationRequest.builder()
                            .userId(booking.getCustomer().getId())
                            .title("Booking Completed")
                            .body("Refund deposit paid for completed Booking with Chef "+ booking.getChef().getUser().getFullName()+".")
                            .screen("Wallet")
                            .build();
                    notificationService.sendPushNotification(notification);
                    customerTransactionRepository.save(refundTransaction);
                    booking.setDepositPaid(BigDecimal.ZERO);
                }
                bookingRepository.save(booking);
            }
        }

        // Chuyển tiền cho chef
        Chef chef = booking.getChef();
        BigDecimal amount = detail.getTotalChefFeePrice();

        Wallet chefWallet = walletRepository.findByUserId(chef.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Chef wallet not found"));

        chefWallet.setBalance(chefWallet.getBalance().add(amount));
        walletRepository.save(chefWallet);

        ChefTransaction transaction = new ChefTransaction();
        transaction.setWallet(chefWallet);
        transaction.setBookingDetail(detail);
        transaction.setTransactionType("CREDIT");
        transaction.setAmount(amount);
        transaction.setStatus("COMPLETED");
        transaction.setDescription("Auto payment for completed bookingDetail #" + detail.getId());

        chefTransactionRepository.save(transaction);

        notificationService.sendPushNotification(NotificationRequest.builder()
                .userId(chef.getUser().getId())
                .title("Booking Completed")
                .body("System has auto-confirmed the completion and sent payment.")
                .screen("ChefEarningsScreen")
                .bookingId(booking.getId())
                .build()
        );
    }
}
