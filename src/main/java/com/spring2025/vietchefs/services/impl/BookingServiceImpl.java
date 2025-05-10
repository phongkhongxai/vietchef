package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.entity.Package;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.*;
import com.spring2025.vietchefs.models.payload.requestModel.*;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.PaymentCycleService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {
    @Autowired
    private BookingDetailRepository bookingDetailRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaypalService paypalService;
    @Autowired
    private BookingDetailItemRepository bookingDetailItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private PaymentCycleRepository paymentCycleRepository;
    @Autowired
    private BookingDetailService bookingDetailService;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private PaymentCycleService paymentCycleService;
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ChefService chefService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public BookingsResponse getBookingsByCustomerId(Long customerId, int pageNo, int pageSize, String sortBy, String sortDir) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"User not found with id: "+ customerId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Booking> bookings = bookingRepository.findByCustomerIdAndIsDeletedFalse(customer.getId(),pageable);

        // get content for page object
        List<Booking> listOfBookings = bookings.getContent();

        List<BookingResponseDto> content = listOfBookings.stream().map(bt -> modelMapper.map(bt, BookingResponseDto.class)).collect(Collectors.toList());
        BookingsResponse templatesResponse = new BookingsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(bookings.getNumber());
        templatesResponse.setPageSize(bookings.getSize());
        templatesResponse.setTotalElements(bookings.getTotalElements());
        templatesResponse.setTotalPages(bookings.getTotalPages());
        templatesResponse.setLast(bookings.isLast());
        return templatesResponse;
    }

    @Override
    public BookingsResponse getBookingsByChefId(Long userId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found with userid: "+ userId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        List<String> excludedStatuses = List.of("PENDING");
        Page<Booking> bookings = bookingRepository.findByChefIdAndStatusNotInAndIsDeletedFalse(chef.getId(), excludedStatuses, pageable);

        // get content for page object
        List<Booking> listOfBookings = bookings.getContent();

        List<BookingResponseDto> content = listOfBookings.stream().map(bt -> modelMapper.map(bt, BookingResponseDto.class)).collect(Collectors.toList());
        BookingsResponse templatesResponse = new BookingsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(bookings.getNumber());
        templatesResponse.setPageSize(bookings.getSize());
        templatesResponse.setTotalElements(bookings.getTotalElements());
        templatesResponse.setTotalPages(bookings.getTotalPages());
        templatesResponse.setLast(bookings.isLast());
        return templatesResponse;
    }

    @Override
    public BookingsResponse getBookingsByCustomerIdAndStatus(Long customerId, List<String> statusList, int pageNo, int pageSize, String sortBy, String sortDir) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"User not found with id: "+ customerId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Booking> bookings = bookingRepository.findByCustomerIdAndStatusInIgnoreCaseAndIsDeletedFalse(customer.getId(), statusList,pageable);

        // get content for page object
        List<Booking> listOfBookings = bookings.getContent();

        List<BookingResponseDto> content = listOfBookings.stream().map(bt -> modelMapper.map(bt, BookingResponseDto.class)).collect(Collectors.toList());
        BookingsResponse templatesResponse = new BookingsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(bookings.getNumber());
        templatesResponse.setPageSize(bookings.getSize());
        templatesResponse.setTotalElements(bookings.getTotalElements());
        templatesResponse.setTotalPages(bookings.getTotalPages());
        templatesResponse.setLast(bookings.isLast());
        return templatesResponse;
    }

    @Override
    public BookingsResponse getBookingsByChefIdAndStatus(Long userId, List<String> statusList, int pageNo, int pageSize, String sortBy, String sortDir) {
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found with userid: "+ userId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<Booking> bookings = bookingRepository.findByChefIdAndStatusInIgnoreCaseAndIsDeletedFalse(chef.getId(), statusList, pageable);

        // get content for page object
        List<Booking> listOfBookings = bookings.getContent();

        List<BookingResponseDto> content = listOfBookings.stream().map(bt -> modelMapper.map(bt, BookingResponseDto.class)).collect(Collectors.toList());
        BookingsResponse templatesResponse = new BookingsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(bookings.getNumber());
        templatesResponse.setPageSize(bookings.getSize());
        templatesResponse.setTotalElements(bookings.getTotalElements());
        templatesResponse.setTotalPages(bookings.getTotalPages());
        templatesResponse.setLast(bookings.isLast());
        return templatesResponse;
    }

    @Override
    public BookingResponseDto getBookingById(Long id) {
        Optional<Booking> booking = bookingRepository.findById(id);
        if (booking.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found with id: "+ id);
        }
        return modelMapper.map(booking, BookingResponseDto.class);
    }

    @Override
    @Transactional
    public BookingResponseDto createSingleBooking(BookingRequestDto dto) {
        User customer = userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Customer not found"));
        Chef chef = chefRepository.findById(dto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));
        if (chef.getReputationPoints() < 60 && chef.getStatus().equalsIgnoreCase("LOCKED")) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "Chef không đủ uy tín để nhận booking dài hạn.");
        }
        if (dto.getGuestCount()>chef.getMaxServingSize()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef just can serving max is "+chef.getMaxServingSize()+".");
        }
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
            detailDto.setIsUpdated(true);
            BookingDetail detail = bookingDetailService.createBookingDetail(booking, detailDto);
            totalPrice = totalPrice.add(detail.getTotalPrice());
        }

        booking.setTotalPrice(totalPrice);
        Booking booking1 = bookingRepository.save(booking);
        NotificationRequest notification = NotificationRequest.builder()
                .userId(customer.getId())
                .title("Booking Created Successfully")
                .body("Your booking has been created. Please proceed to payment to confirm your reservation.")
                .bookingId(booking.getId())
                .screen("Booking")
                .build();
        notificationService.sendPushNotification(notification);
        return modelMapper.map(booking1, BookingResponseDto.class);

    }

    @Override
    @Transactional
    public BookingResponseDto createLongtermBooking(BookingRequestDto dto) {
        User customer = userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Customer not found"));
        Chef chef = chefRepository.findById(dto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));
        Package selectedPackage = packageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Package not found"));
        if (chef.getReputationPoints() < 80) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "Chef không đủ uy tín để nhận booking dài hạn.");
        }
        if (dto.getGuestCount()>chef.getMaxServingSize()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef just can serving max is "+chef.getMaxServingSize()+".");
        }
        // Kiểm tra xem đầu bếp có hỗ trợ package này không
        if (!chef.getPackages().contains(selectedPackage)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Selected package is not available for this chef.");
        }
        if (dto.getBookingDetails().size() != selectedPackage.getDurationDays()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "The number of booking details must match the package duration of " + selectedPackage.getDurationDays() + " days.");
        }
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Tạo Booking chính
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setChef(chef);
        booking.setBookingType("LONG_TERM");
        booking.setStatus("PENDING");
        booking.setRequestDetails(dto.getRequestDetails());
        booking.setGuestCount(dto.getGuestCount());
        booking.setBookingPackage(selectedPackage);
        booking.setIsDeleted(false);
        booking.setTotalPrice(totalPrice);
        booking = bookingRepository.save(booking);

        List<BookingDetail> bookingDetailList = new ArrayList<>();
        List<String> overlapMessages = new ArrayList<>();
        for (BookingDetailRequestDto detailDto : dto.getBookingDetails()) {
            // Kiểm tra trùng lịch cho từng ngày
            if (isOverlappingWithExistingBookings(chef, detailDto.getSessionDate(), detailDto.getTimeBeginTravel(), detailDto.getStartTime())) {
                String overlapMessage = "Chef đã có lịch trong khoảng thời gian này cho ngày " + detailDto.getSessionDate() + ". Vui lòng chọn khung giờ khác.";
                overlapMessages.add(overlapMessage);
            } else {
                BookingDetail detail = bookingDetailService.createBookingDetail(booking, detailDto);
                bookingDetailList.add(detail);
                totalPrice = totalPrice.add(detail.getTotalPrice());
            }
        }

        if (!overlapMessages.isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, String.join("\n", overlapMessages));
        }

        booking.setTotalPrice(totalPrice);
        booking.setBookingDetails(bookingDetailList);
        bookingRepository.save(booking);

        // Chia các kỳ thanh toán
        paymentCycleService.createPaymentCycles(booking);
        NotificationRequest notification = NotificationRequest.builder()
                .userId(customer.getId())
                .title("Please Confirm Your Booking with a Deposit")
                .body("Your long-term booking has been created. Please pay the deposit to confirm your reservation.")
                .bookingId(booking.getId())
                .screen("Booking")
                .build();
        notificationService.sendPushNotification(notification);


        // Chuyển Booking sang DTO để trả về
        return modelMapper.map(booking, BookingResponseDto.class);
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
    public ReviewSingleBookingResponse calculateFinalPriceForSingleBooking(BookingPriceRequestDto dto) {
        Chef chef = chefRepository.findById(dto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));
        if (chef.getReputationPoints() < 60 || chef.getStatus().equalsIgnoreCase("LOCKED") || !chef.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "Chef không đủ uy tín để nhận booking.");
        }
        if (dto.getGuestCount()>chef.getMaxServingSize()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef just can serving max is "+chef.getMaxServingSize()+".");
        }
        BigDecimal totalBookingPrice = BigDecimal.ZERO;
        ReviewSingleBookingResponse reviewSingleBookingResponse = new ReviewSingleBookingResponse();

        BookingDetailPriceRequestDto detailDto = dto.getBookingDetail();
        BigDecimal totalCookTime = BigDecimal.ZERO;
        if (!detailDto.getSessionDate().isAfter(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"SessionDate should be in the future.");
        }
            if (detailDto.getMenuId() != null || (detailDto.getExtraDishIds() != null && !detailDto.getExtraDishIds().isEmpty())) {
                Set<Long> uniqueDishIds = new HashSet<>();

                if (detailDto.getMenuId() != null) {
                    Menu menu = menuRepository.findById(detailDto.getMenuId())
                            .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));

                    if (!menu.getChef().getId().equals(chef.getId())) {
                        throw new VchefApiException(HttpStatus.BAD_REQUEST, "Menu does not belong to the selected Chef");
                    }

                    List<Long> menuDishIds = menu.getMenuItems().stream()
                            .map(item -> item.getDish().getId())
                            .toList();

                    uniqueDishIds.addAll(menuDishIds);
                }

                if (detailDto.getExtraDishIds() != null && !detailDto.getExtraDishIds().isEmpty()) {
                    for (Long extraDishId : detailDto.getExtraDishIds()) {
                        Dish dish = dishRepository.findById(extraDishId)
                                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with ID: " + extraDishId));

                        if (!dish.getChef().getId().equals(chef.getId())) {
                            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Dish with ID " + extraDishId + " does not belong to the selected Chef");
                        }

                        uniqueDishIds.add(extraDishId);
                    }
                }

                List<Long> dishIds = new ArrayList<>(uniqueDishIds);
                if (detailDto.getMenuId() != null) {
                    // Nếu có menuId, gọi hàm tính tổng thời gian từ menu và món ngoài menu
                    totalCookTime = calculateService.calculateTotalCookTimeFromMenu(detailDto.getMenuId(), dishIds, dto.getGuestCount());
                } else {
                    // Nếu không có menuId, chỉ tính tổng thời gian cho các món trong dishIds
                    totalCookTime = calculateService.calculateTotalCookTime(dishIds, dto.getGuestCount());
                }
                reviewSingleBookingResponse.setCookTimeMinutes(totalCookTime.multiply(BigDecimal.valueOf(60)));

            }

            // 🔹 Tính phí dịch vụ đầu bếp (công nấu ăn)
            BigDecimal price1 = calculateService.calculateChefServiceFee(chef.getPrice(), totalCookTime);
            reviewSingleBookingResponse.setChefCookingFee(price1);

            // 🔹 Tính phí món ăn (menu hoặc món lẻ)
            BigDecimal price2 = calculateService.calculateDishPrice(detailDto.getMenuId(), dto.getGuestCount(), detailDto.getExtraDishIds());
            reviewSingleBookingResponse.setPriceOfDishes(price2);
            BigDecimal platformFee = price1.multiply(BigDecimal.valueOf(0.25))  // 25% của cookingFee
                .add(price2.multiply(BigDecimal.valueOf(0.20))); // 20% của dishPrice

            // 🔹 Tính phí di chuyển
            DistanceFeeResponse price3Of = calculateService.calculateTravelFee(chef.getAddress(), detailDto.getLocation());
            if(price3Of.getDistanceKm().compareTo(BigDecimal.valueOf(50))>0){
                throw new VchefApiException(HttpStatus.BAD_REQUEST,"Distance between you and chef cannot bigger than 50km.");
            }
            BigDecimal price3 = price3Of.getTravelFee();
            TimeTravelResponse ttp = calculateService.calculateArrivalTime(detailDto.getStartTime(), totalCookTime, price3Of.getDurationHours());
            reviewSingleBookingResponse.setArrivalFee(price3);
            reviewSingleBookingResponse.setPlatformFee(platformFee);
            BigDecimal totalChefFeePrice = price1.add(price2.multiply(BigDecimal.valueOf(0.8))).add(price3) ;


            // 🔹 Tính tổng giá của BookingDetail
            BigDecimal price4 = calculateService.calculateFinalPrice(price1, price2, price3);
            totalBookingPrice = totalBookingPrice.add(price4);
            reviewSingleBookingResponse.setDistanceKm(price3Of.getDistanceKm());
            reviewSingleBookingResponse.setChefBringIngredients(detailDto.getChefBringIngredients());
            reviewSingleBookingResponse.setTotalChefFeePrice(totalChefFeePrice);
            reviewSingleBookingResponse.setTotalPrice(totalBookingPrice);
            reviewSingleBookingResponse.setTimeBeginTravel(ttp.getTimeBeginTravel());
            reviewSingleBookingResponse.setTimeBeginCook(ttp.getTimeBeginCook());
            reviewSingleBookingResponse.setMenuId(detailDto.getMenuId());
        return reviewSingleBookingResponse;
    }

    @Override
    public ReviewLongTermBookingResponse calculateFinalPriceForLongTermBooking(BookingLTPriceRequestDto dto) {
        Chef chef = chefRepository.findById(dto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));
        if (chef.getReputationPoints() < 80 || !chef.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new VchefApiException(HttpStatus.FORBIDDEN, "Chef không đủ uy tín để nhận booking.");
        }
        if (dto.getGuestCount()>chef.getMaxServingSize()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef just can serving max is "+chef.getMaxServingSize()+".");
        }
        Package bookingPackage = packageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Package not found"));
        if (dto.getBookingDetails().size() != bookingPackage.getDurationDays()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "The number of booking details must match the package duration of " + bookingPackage.getDurationDays() + " days.");
        }
        if(dto.getGuestCount() > bookingPackage.getMaxGuestCountPerMeal()){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "The number of guests can not bigger than " + bookingPackage.getMaxGuestCountPerMeal() + ".");
        }

        // 🔹 Tính phí di chuyển
        DistanceFeeResponse travelFeeResponse = calculateService.calculateTravelFee(chef.getAddress(), dto.getLocation());
        if(travelFeeResponse.getDistanceKm().compareTo(BigDecimal.valueOf(50))>0){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Distance between you and chef cannot bigger than 50km.");
        }
        BigDecimal travelFee = travelFeeResponse.getTravelFee();
        BigDecimal totalBookingPrice = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        List<BookingDetailPriceResponse> detailPriceResponses = new ArrayList<>();
        List<LocalDate> top5SessionDates = dto.getBookingDetails().stream()
                .map(BookingDetailPriceLTRequest::getSessionDate)
                .filter(se -> se.isAfter(LocalDate.now())) //   lọc những ngày trong tương lai
                .distinct()
                .sorted()
                .limit(5)
                .toList();

        List<LocalDate> invalidDates = new ArrayList<>();
        for (BookingDetailPriceLTRequest detailDto : dto.getBookingDetails()) {
            BigDecimal totalCookTime = BigDecimal.ZERO;
            if (!detailDto.getSessionDate().isAfter(LocalDate.now())) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST,"SessionDate should be in the future.");
            }
            boolean isTodayOrTomorrow = detailDto.getSessionDate().isEqual(LocalDate.now())
                    || detailDto.getSessionDate().isEqual(LocalDate.now().plusDays(1));

            if (isTodayOrTomorrow && top5SessionDates.contains(detailDto.getSessionDate())) {
                if (Boolean.FALSE.equals(detailDto.getIsDishSelected())
                        && (detailDto.getDishes() == null || detailDto.getDishes().isEmpty())) {
                    invalidDates.add(detailDto.getSessionDate());
                }
            }
            // 🔹 Kiểm tra xem BookingDetail đã chọn món chưa
            if (Boolean.FALSE.equals(detailDto.getIsDishSelected()) && detailDto.getDishes()==null) {
                //  Nếu chưa chọn món, lấy tổng thời gian nấu của 3 món lâu nhất của đầu bếp
                totalCookTime = calculateService.calculateMaxCookTime(chef.getId(),bookingPackage.getMaxDishesPerMeal(),dto.getGuestCount());

            } else {
                // 🔹 Nếu đã chọn món, tính thời gian nấu dựa trên món ăn đã chọn
                if (detailDto.getMenuId() != null || (detailDto.getExtraDishIds() != null && !detailDto.getExtraDishIds().isEmpty())) {
                    Set<Long> uniqueDishIds = new HashSet<>();

                    if (detailDto.getMenuId() != null) {
                        Menu menu = menuRepository.findById(detailDto.getMenuId())
                                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found"));

                        if (!menu.getChef().getId().equals(chef.getId())) {
                            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Menu does not belong to the selected Chef");
                        }

                        List<Long> menuDishIds = menu.getMenuItems().stream()
                                .map(item -> item.getDish().getId())
                                .toList();

                        uniqueDishIds.addAll(menuDishIds);
                    }

                    if (detailDto.getExtraDishIds() != null && !detailDto.getExtraDishIds().isEmpty()) {
                        for (Long extraDishId : detailDto.getExtraDishIds()) {
                            Dish dish = dishRepository.findById(extraDishId)
                                    .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found with ID: " + extraDishId));

                            if (!dish.getChef().getId().equals(chef.getId())) {
                                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Dish with ID " + extraDishId + " does not belong to the selected Chef");
                            }

                            uniqueDishIds.add(extraDishId);
                        }
                    }

                    List<Long> dishIds = new ArrayList<>(uniqueDishIds);
                    if (detailDto.getMenuId() != null) {
                        totalCookTime = calculateService.calculateTotalCookTimeFromMenu(detailDto.getMenuId(), dishIds, dto.getGuestCount());
                    } else {
                        // Nếu không có menuId, chỉ tính tổng thời gian cho các món trong dishIds
                        totalCookTime = calculateService.calculateTotalCookTime(dishIds, dto.getGuestCount());
                    }
                }
            }


            // 🔹 Tính phí dịch vụ đầu bếp (công nấu ăn)
            BigDecimal chefCookingFee = calculateService.calculateChefServiceFee(chef.getPrice(), totalCookTime);
            // 🔹 Tính phí món ăn
            BigDecimal dishPrice = calculateService.calculateDishPrice(detailDto.getMenuId(), dto.getGuestCount(), detailDto.getExtraDishIds());
            BigDecimal totalChefFeePrice = chefCookingFee.add(dishPrice.multiply(BigDecimal.valueOf(0.8))).add(travelFee);
            BigDecimal platformFee = chefCookingFee.multiply(BigDecimal.valueOf(0.25))  // 25% của cookingFee
                    .add(dishPrice.multiply(BigDecimal.valueOf(0.20))); // 20% của dishPrice
            // 🔹 Tính tổng giá từng buổi
            BigDecimal sessionTotalPrice = calculateService.calculateFinalPrice(chefCookingFee, dishPrice, travelFee);
            // 🔹 Tính thời gian di chuyển và nấu ăn
            TimeTravelResponse ttp = calculateService.calculateArrivalTime(detailDto.getStartTime(), totalCookTime, travelFeeResponse.getDurationHours());
            BigDecimal discountAmountDetail = BigDecimal.ZERO;
            // Áp dụng giảm giá từ Package
            if (bookingPackage.getDiscount() != null) {
                discountAmountDetail = platformFee.multiply(bookingPackage.getDiscount());
                discountAmount = discountAmount.add(discountAmountDetail);
                sessionTotalPrice = sessionTotalPrice.subtract(discountAmountDetail);
            }
            totalBookingPrice = totalBookingPrice.add(sessionTotalPrice);

            // 🔹 Tạo response cho từng BookingDetail
            BookingDetailPriceResponse detailResponse = new BookingDetailPriceResponse();
            detailResponse.setTotalCookTime(totalCookTime.multiply(BigDecimal.valueOf(60)));
            detailResponse.setMenuId(detailDto.getMenuId());
            detailResponse.setChefBringIngredients(detailDto.getChefBringIngredients());
            detailResponse.setSessionDate(detailDto.getSessionDate());
            detailResponse.setDiscountAmout(discountAmountDetail);
            detailResponse.setTotalPrice(sessionTotalPrice);
            detailResponse.setChefCookingFee(chefCookingFee);
            detailResponse.setPriceOfDishes(dishPrice);
            detailResponse.setArrivalFee(travelFee);
            detailResponse.setTimeBeginTravel(ttp.getTimeBeginTravel());
            detailResponse.setTimeBeginCook(ttp.getTimeBeginCook());
            detailResponse.setStartTime(detailDto.getStartTime());
            detailResponse.setLocation(dto.getLocation());
            detailResponse.setDishes(detailDto.getDishes());
            detailResponse.setPlatformFee(platformFee);
            detailResponse.setTotalChefFeePrice(totalChefFeePrice);
            detailResponse.setIsUpdated(detailDto.getIsDishSelected());
            detailPriceResponses.add(detailResponse);
        }
        if (!invalidDates.isEmpty()) {
            String message = "You must select a dish for the following session dates: " +
                    invalidDates.stream()
                            .map(LocalDate::toString)
                            .collect(Collectors.joining(", "));
            throw new VchefApiException(HttpStatus.BAD_REQUEST, message);
        }

        // Tạo response tổng hợp
        ReviewLongTermBookingResponse reviewResponse = new ReviewLongTermBookingResponse();
        reviewResponse.setDistanceKm(travelFeeResponse.getDistanceKm());
        reviewResponse.setTotalPrice(totalBookingPrice);
        reviewResponse.setDiscountAmount(discountAmount);
        reviewResponse.setBookingDetails(detailPriceResponses);

        return reviewResponse;
    }

    @Override
    @Transactional
    public BookingResponseDto updateBookingStatusConfirm(Long bookingId, Long userId, boolean isConfirmed) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId));
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with userId"));
        if (!chef.getId().equals(booking.getChef().getId())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef not in booking.");
        }

        // 3. Kiểm tra trạng thái và loại Booking trước khi xác nhận/từ chối
        boolean isSinglePaid = "PAID".equals(booking.getStatus()) && "SINGLE".equals(booking.getBookingType());
        boolean isLongTermDeposited = "DEPOSITED".equals(booking.getStatus()) && "LONG_TERM".equals(booking.getBookingType());
        boolean isPaidFirst = "PAID_FIRST_CYCLE".equals(booking.getStatus()) && "LONG_TERM".equalsIgnoreCase(booking.getBookingType());
        if (isConfirmed) {
            // Trường hợp xác nhận booking
            if (isSinglePaid || isLongTermDeposited || isPaidFirst) {
                if (isSinglePaid) {
                    List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
                    for (BookingDetail detail : bookingDetails) {
                        detail.setStatus("SCHEDULED_COMPLETE");
                        bookingDetailRepository.save(detail);
                    }
                    booking.setStatus("CONFIRMED");

                }
                if (isLongTermDeposited) {
//                    List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
//                    for (BookingDetail detail : bookingDetails) {
//                        if (detail.getIsUpdated()) {
//                            detail.setStatus("SCHEDULED_COMPLETE");
//                            bookingDetailRepository.save(detail);
//                        }
//                    }
                    booking.setStatus("CONFIRMED");

                }
                if (isPaidFirst){
//                    List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
//                    for (BookingDetail detail : bookingDetails) {
//                        if (detail.getIsUpdated()) {
//                            detail.setStatus("SCHEDULED_COMPLETE");
//                            bookingDetailRepository.save(detail);
//                        }
//                    }
                    booking.setStatus("CONFIRMED_PARTIALLY_PAID");
                }
                bookingRepository.save(booking);
                NotificationRequest confirmNotification = NotificationRequest.builder()
                        .userId(booking.getCustomer().getId())
                        .title("Booking Confirmed")
                        .body("Your booking #" + booking.getBookingType() + " has been confirmed by " +
                                booking.getChef().getUser().getFullName() + ".")
                        .bookingId(booking.getId())
                        .screen("Booking")
                        .build();

                notificationService.sendPushNotification(confirmNotification);

                return modelMapper.map(booking, BookingResponseDto.class);
            }
            else {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking does not meet the conditions for confirmation.");
            }
        } else {
            // Trường hợp từ chối booking và hoàn tiền lại
            if (isSinglePaid || isLongTermDeposited || isPaidFirst) {
                List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
                for (BookingDetail detail : bookingDetails) {
                    detail.setStatus("CANCELED");
                    bookingDetailRepository.save(detail);
                }
                // 4. Lấy ví của khách hàng để hoàn tiền
                Wallet wallet = walletRepository.findByUserId(booking.getCustomer().getId())
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for customer."));
                BigDecimal refundAmount = isSinglePaid ? booking.getTotalPrice() : booking.getDepositPaid();
                if(isPaidFirst){
                    PaymentCycle paymentCycle = paymentCycleRepository.findByBookingAndCycleOrder(booking,1);
                    refundAmount = paymentCycle.getAmountDue();
                }
                // 5. Hoàn tiền vào ví khách hàng
                wallet.setBalance(wallet.getBalance().add(refundAmount));
                walletRepository.save(wallet);

                // 6. Ghi lại giao dịch hoàn tiền
                CustomerTransaction refundTransaction = CustomerTransaction.builder()
                        .wallet(wallet)
                        .booking(booking)
                        .transactionType("REFUND")
                        .amount(refundAmount)
                        .status("COMPLETED")
                        .isDeleted(false)
                        .description("Refund for rejected Booking #" + booking.getId())
                        .build();
                customerTransactionRepository.save(refundTransaction);
                chefService.updateReputation(chef,-1);

                // 7. Cập nhật trạng thái Booking thành REJECTED
                booking.setStatus("REJECTED");
                bookingRepository.save(booking);
                NotificationRequest rejectChefNotification = NotificationRequest.builder()
                        .userId(booking.getChef().getUser().getId())
                        .title("Booking Rejected - Reputation Points Penalized")
                        .body("Your booking #" + booking.getBookingType() + " was rejected. As a result, your reputation points have been penalized for not accepting the booking.")
                        .bookingId(booking.getId())
                        .screen("ChefDashboard")
                        .notiType("PENALDO_NOTIFY")
                        .build();
                notificationService.sendPushNotification(rejectChefNotification);

                NotificationRequest rejectNotification = NotificationRequest.builder()
                        .userId(booking.getCustomer().getId())
                        .title("Booking Rejected")
                        .body("Your booking #" + booking.getBookingType() + " was rejected by " +
                                booking.getChef().getUser().getFullName() + ". A refund of " +
                                refundAmount + " has been issued to your wallet.")
                        .bookingId(booking.getId())
                        .screen("Booking")
                        .build();

                notificationService.sendPushNotification(rejectNotification);

                return modelMapper.map(booking, BookingResponseDto.class);
            } else {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking does not meet the conditions for rejection.");
            }

        }
    }

    @Override
    public BookingResponseDto paymentBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found with id: " + bookingId));

        if (!booking.getStatus().equalsIgnoreCase("PENDING")) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking is not in PENDING status.");
        }
        if (!booking.getBookingType().equalsIgnoreCase("SINGLE")) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking is not in Single.");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for customer."));

        BigDecimal bookingTotalPrice = booking.getTotalPrice();
        if (wallet.getBalance().compareTo(bookingTotalPrice) < 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in the wallet.");
        }
        wallet.setBalance(wallet.getBalance().subtract(bookingTotalPrice));
        walletRepository.save(wallet);
        booking.setStatus("PAID");
        bookingRepository.save(booking);

        // Tạo Transaction ghi lại giao dịch thanh toán
        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setWallet(wallet);
        transaction.setBooking(booking);
        transaction.setTransactionType("PAYMENT");
        transaction.setAmount(bookingTotalPrice);
        transaction.setStatus("COMPLETED");
        transaction.setDescription("Payment for Booking #" + booking.getId()+" with "+booking.getChef().getUser().getFullName()+" Chef.");
        customerTransactionRepository.save(transaction);

        NotificationRequest notification = NotificationRequest.builder()
                .userId(userId)
                .title("Payment Successful")
                .body("Your booking has been successfully paid. Chef " + booking.getChef().getUser().getFullName() + " will be ready as scheduled.")
                .bookingId(booking.getId())
                .screen("BookingDetail")
                .build();

        notificationService.sendPushNotification(notification);
        NotificationRequest chefNotification = NotificationRequest.builder()
                .userId(booking.getChef().getUser().getId())
                .title("New Booking Requires Confirmation")
                .body("A customer has completed a payment for Booking #" + booking.getBookingType() +
                        ". Please confirm before the scheduled session.")
                .bookingId(booking.getId())
                .screen("ChefBookingManagementScreen")
                .build();
        notificationService.sendPushNotification(chefNotification);

        return modelMapper.map(booking, BookingResponseDto.class);
    }

    @Override
    public List<PaymentCycleResponse> getPaymentCyclesWithDetails(Long bookingId) {
        // Tìm danh sách các PaymentCycle theo BookingId
        List<PaymentCycle> paymentCycles = paymentCycleRepository.findByBookingId(bookingId);

            Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found with id."));
        if (paymentCycles.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "No PaymentCycles found for bookingId: " + bookingId);
        }

        // Lấy tất cả BookingDetail của Booking
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);

        // Nhóm BookingDetail theo PaymentCycle
        List<PaymentCycleResponse> responseList = new ArrayList<>();
        for (PaymentCycle cycle : paymentCycles) {
            // Lọc BookingDetail nào nằm trong khoảng ngày của PaymentCycle
            List<BookingDetailDto> detailsInCycle = bookingDetails.stream()
                    .filter(detail -> !detail.getSessionDate().isBefore(cycle.getStartDate()) &&
                            !detail.getSessionDate().isAfter(cycle.getEndDate()))
                    .map(detail -> modelMapper.map(detail, BookingDetailDto.class))
                    .toList();

            // Tạo DTO response cho PaymentCycle
            PaymentCycleResponse cycleDto = PaymentCycleResponse.builder()
                    .id(cycle.getId())
                    .startDate(cycle.getStartDate())
                    .endDate(cycle.getEndDate())
                    .amountDue(cycle.getAmountDue())
                    .status(cycle.getStatus())
                    .cycleOrder(cycle.getCycleOrder())
                    .bookingDetails(detailsInCycle)
                    .build();

            responseList.add(cycleDto);
        }

        return responseList;
    }

    @Override
    @Transactional
    public PaymentCycleResponse payForPaymentCycle(Long paymentCycleId, Long userId) {
        // 1. Lấy PaymentCycle từ paymentCycleId
        PaymentCycle paymentCycle = paymentCycleRepository.findById(paymentCycleId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "PaymentCycle not found with id: " + paymentCycleId));

        // 2. Kiểm tra PaymentCycle đã thanh toán chưa
        if ("PAID".equals(paymentCycle.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This PaymentCycle has already been paid.");
        }

        // 3. Lấy Booking từ PaymentCycle
        Booking booking = paymentCycle.getBooking();
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) && !"CONFIRMED_PARTIALLY_PAID".equalsIgnoreCase(booking.getStatus()) && !"PENDING_FIRST_CYCLE".equalsIgnoreCase(booking.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This PaymentCycle cannot be paid at this time.");
        }
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
        boolean allUpdated = bookingDetails.stream()
                .filter(detail -> !detail.getSessionDate().isBefore(paymentCycle.getStartDate()) &&
                        !detail.getSessionDate().isAfter(paymentCycle.getEndDate()))
                .allMatch(detail -> Boolean.TRUE.equals(detail.getIsUpdated()));

        if (!allUpdated) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "All BookingDetails must be updated before payment.");
        }

        // 4. Lấy ví của người dùng
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for user."));

        // 5. Kiểm tra số dư có đủ không
        BigDecimal amountDue = paymentCycle.getAmountDue();
        BigDecimal depositPaid = booking.getDepositPaid(); // Số tiền đặt cọc
        BigDecimal remainingAmount = amountDue;

        if (depositPaid.compareTo(BigDecimal.ZERO) > 0) {
                remainingAmount = amountDue.add(depositPaid);
        }

        if("PENDING_FIRST_CYCLE".equalsIgnoreCase(booking.getStatus())){
            if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (wallet.getBalance().compareTo(remainingAmount) < 0) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in wallet.");
                }
                // Trừ tiền trong ví
                wallet.setBalance(wallet.getBalance().subtract(remainingAmount));
                walletRepository.save(wallet);
            }
        }else{
            if(paymentCycle.getAmountDue().compareTo(BigDecimal.ZERO) > 0){
                if (wallet.getBalance().compareTo(paymentCycle.getAmountDue()) < 0) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in wallet.");
                }
                wallet.setBalance(wallet.getBalance().subtract(paymentCycle.getAmountDue()));
                walletRepository.save(wallet);
            }
        }
        // 7. Cập nhật trạng thái của PaymentCycle → "PAID"
        paymentCycle.setStatus("PAID");
        paymentCycleRepository.save(paymentCycle);

        // 8. Cập nhật trạng thái của BookingDetail thuộc PaymentCycle này
        for (BookingDetail detail : bookingDetails) {
            if (!detail.getSessionDate().isBefore(paymentCycle.getStartDate()) &&
                    !detail.getSessionDate().isAfter(paymentCycle.getEndDate())) {
                detail.setStatus("SCHEDULED_COMPLETE");
            }
        }
        bookingDetailRepository.saveAll(bookingDetails);

        // 9. Cập nhật trạng thái Booking dựa vào tình trạng PaymentCycle
        List<PaymentCycle> allCycles = paymentCycleRepository.findByBookingId(booking.getId());
        if("PENDING_FIRST_CYCLE".equalsIgnoreCase(booking.getStatus())){
            booking.setStatus("PAID_FIRST_CYCLE");
        }else{
            long paidCount = allCycles.stream().filter(pc -> "PAID".equals(pc.getStatus())).count();
            if (paidCount == 0) {
                booking.setStatus("CONFIRMED");
            } else if (paidCount < allCycles.size()) {
                booking.setStatus("CONFIRMED_PARTIALLY_PAID");
            } else {
                booking.setStatus("CONFIRMED_PAID");
            }
        }
        booking = bookingRepository.save(booking);
        // 10. Ghi lại giao dịch vào bảng Transaction
        CustomerTransaction transaction = CustomerTransaction.builder()
                .wallet(wallet)
                .booking(booking)
                .transactionType("PAYMENT")
                .amount(paymentCycle.getAmountDue())
                .status("COMPLETED")
                .isDeleted(false)
                .description("Payment for PaymentCycle #" + paymentCycle.getId())
                .build();
        customerTransactionRepository.save(transaction);
        if ("PAID_FIRST_CYCLE".equalsIgnoreCase(booking.getStatus())){
            CustomerTransaction transaction1 = CustomerTransaction.builder()
                    .wallet(wallet)
                    .booking(booking)
                    .transactionType("INITIAL_PAYMENT")
                    .amount(booking.getDepositPaid())
                    .status("COMPLETED")
                    .isDeleted(false)
                    .description("Deposit for Long-Term Booking #" + booking.getId() +
                            " with " + booking.getChef().getUser().getFullName() + " Chef.")
                    .build();
            customerTransactionRepository.save(transaction1);
        }
        NotificationRequest chefNotification;
        if(booking.getStatus().equalsIgnoreCase("PAID_FIRST_CYCLE")){
            chefNotification = NotificationRequest.builder()
                    .userId(booking.getChef().getUser().getId())
                    .title("New Booking Requires Confirmation")
                    .body("A customer has completed a payment for Booking #" + booking.getBookingType() +
                            ". Please confirm before the scheduled session.")
                    .bookingId(booking.getId())
                    .screen("ChefBookingManagementScreen")
                    .build();
        }else{
            chefNotification = NotificationRequest.builder()
                    .userId(booking.getChef().getUser().getId())
                    .title("Customer Paid for Upcoming Sessions")
                    .body("Customer has paid for Payment Cycle #" + paymentCycle.getCycleOrder() +
                            ". Please check your schedule.")
                    .bookingId(booking.getId())
                    .screen("ChefBookingManagementScreen")
                    .build();

        }
        notificationService.sendPushNotification(chefNotification);
        // 11. Trả về PaymentCycleResponse sau khi thanh toán
        return PaymentCycleResponse.builder()
                .id(paymentCycle.getId())
                .startDate(paymentCycle.getStartDate())
                .endDate(paymentCycle.getEndDate())
                .amountDue(paymentCycle.getAmountDue())
                .status(paymentCycle.getStatus())
                .cycleOrder(paymentCycle.getCycleOrder())
                .bookingDetails(bookingDetails.stream()
                        .map(detail -> modelMapper.map(detail, BookingDetailDto.class))
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<BookingResponseDto> depositBooking(Long bookingId, Long userId) {
        // 1. Lấy Booking theo ID
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId));

        if (!"PENDING".equals(booking.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking is not in PENDING status.");
        }
        if (!"LONG_TERM".equals(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking is not Long-Term.");
        }

        List<BookingDetail> details = bookingDetailRepository.findByBookingOrderBySessionDateAsc(booking);
        if (details.isEmpty()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking has no BookingDetails.");
        }

        LocalDate now = LocalDate.now();
        LocalDate firstSession = details.get(0).getSessionDate();
        if (!firstSession.isAfter(now)) {
            return ApiResponse.<BookingResponseDto>builder()
                    .success(false)
                    .message("Cannot deposit. The first session has already started or is today.")
                    .build();
        }
        BigDecimal depositAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.05));
        if (!firstSession.isAfter(now.plusDays(2))) {
            booking.setStatus("PENDING_FIRST_CYCLE");
            booking.setDepositPaid(depositAmount);
            bookingRepository.save(booking);
            return ApiResponse.<BookingResponseDto>builder()
                    .success(false)
                    .message("Session is too close. Status updated to PENDING_FIRST_CYCLE. Please proceed with first payment.")
                    .data(modelMapper.map(booking, BookingResponseDto.class))
                    .build();
        }
        // Tiến hành đặt cọc như bình thường
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for customer."));

        if (wallet.getBalance().compareTo(depositAmount) < 0) {
            return ApiResponse.<BookingResponseDto>builder()
                    .success(false)
                    .message("Insufficient balance in the wallet.")
                    .build();
        }

        wallet.setBalance(wallet.getBalance().subtract(depositAmount));
        walletRepository.save(wallet);

        booking.setStatus("DEPOSITED");
        booking.setDepositPaid(depositAmount);
        bookingRepository.save(booking);

        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setWallet(wallet);
        transaction.setBooking(booking);
        transaction.setTransactionType("INITIAL_PAYMENT");
        transaction.setAmount(depositAmount);
        transaction.setStatus("COMPLETED");
        transaction.setDescription("Deposit for Long-Term Booking #" + booking.getId() +
                " with " + booking.getChef().getUser().getFullName() + " Chef.");
        customerTransactionRepository.save(transaction);
        NotificationRequest notification = NotificationRequest.builder()
                .userId(userId)
                .title("Deposit Successful")
                .body("Your deposit for the long-term booking with Chef " + booking.getChef().getUser().getFullName() + " has been received. Get ready for pay your first cycle!")
                .bookingId(booking.getId())
                .screen("BookingDetail")
                .build();
        notificationService.sendPushNotification(notification);
        NotificationRequest chefNotification = NotificationRequest.builder()
                .userId(booking.getChef().getUser().getId())
                .title("New Booking Requires Confirmation")
                .body("A customer has completed a payment for Booking #" + booking.getBookingType() +
                        ". Please confirm before the scheduled session.")
                .bookingId(booking.getId())
                .screen("ChefBookingManagementScreen")
                .build();
        notificationService.sendPushNotification(chefNotification);


        return ApiResponse.<BookingResponseDto>builder()
                .success(true)
                .message("Deposit successfully.")
                .data(modelMapper.map(booking, BookingResponseDto.class))
                .build();

    }

    @Override
    @Transactional
    public BookingResponseDto cancelSingleBooking(Long bookingId,Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!customer.getId().equals(booking.getCustomer().getId())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "User not in booking.");
        }
        if (!"SINGLE".equalsIgnoreCase(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This is not a single booking");
        }
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingId(bookingId);
        if (bookingDetails.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Booking detail not found");
        }
        BookingDetail bookingDetail = bookingDetails.get(0);
        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus()) &&
                bookingDetail.getSessionDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel booking less than 1 days before session date");
        }
        if ("PENDING".equalsIgnoreCase(booking.getStatus())) {
            booking.setStatus("CANCELED");
            booking = bookingRepository.save(booking);
            return modelMapper.map(booking, BookingResponseDto.class);
        }
        // Nếu booking đã được thanh toán (PAID), kiểm tra giao dịch và hoàn tiền
        if ("PAID".equalsIgnoreCase(booking.getStatus()) || "CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            List<CustomerTransaction> transactions = customerTransactionRepository
                    .findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(bookingId, "PAYMENT", "COMPLETED");

            if (!transactions.isEmpty()) {
                boolean hasRefund = customerTransactionRepository.existsByBookingIdAndTransactionType(bookingId, "REFUND");
                if (hasRefund) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking has already been refunded");
                }

                Wallet customerWallet = transactions.get(0).getWallet();
                BigDecimal refundAmount = transactions.get(0).getAmount();
                // Tạo giao dịch hoàn tiền (REFUND)
                CustomerTransaction refundTransaction = CustomerTransaction.builder()
                        .wallet(customerWallet)
                        .booking(booking)
                        .transactionType("REFUND")
                        .amount(refundAmount)
                        .description("Refund for canceled booking.")
                        .status("COMPLETED")
                        .isDeleted(false)
                        .build();
                customerTransactionRepository.save(refundTransaction);

                customerWallet.setBalance(customerWallet.getBalance().add(refundAmount));
                walletRepository.save(customerWallet);
                bookingDetail.setStatus("REFUNDED");
                bookingDetailRepository.save(bookingDetail);
            }
        } else{
            bookingDetail.setStatus("CANCELED");
            bookingDetailRepository.save(bookingDetail);
        }
        booking.setStatus("CANCELED");
        booking = bookingRepository.save(booking);
        if("CONFIRMED".equalsIgnoreCase(booking.getStatus())){
            NotificationRequest chefNotification = NotificationRequest.builder()
                    .userId(booking.getChef().getUser().getId())
                    .title("Booking Canceled")
                    .body("The customer has canceled their booking for " + bookingDetail.getSessionDate() + ".")
                    .bookingId(booking.getId())
                    .screen("ChefBookingManagementScreen")
                    .build();
            notificationService.sendPushNotification(chefNotification);
        }

        // Trả về DTO phản hồi
        return modelMapper.map(booking, BookingResponseDto.class);

    }

    @Override
    @Transactional
    public BookingResponseDto cancelSingleBookingFromChef(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with userId"));
        if (!chef.getId().equals(booking.getChef().getId())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef not in booking.");
        }
        if (!"SINGLE".equalsIgnoreCase(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This is not a single booking");
        }
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingId(bookingId);
        if (bookingDetails.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Booking detail not found");
        }
        BookingDetail bookingDetail = bookingDetails.get(0);
        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus()) &&
                bookingDetail.getSessionDate().isEqual(LocalDate.now())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel booking with a session happening today.");
        }
        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            List<CustomerTransaction> transactions = customerTransactionRepository
                    .findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(bookingId, "PAYMENT", "COMPLETED");
            if (!transactions.isEmpty()) {
                boolean hasRefund = customerTransactionRepository.existsByBookingIdAndTransactionType(bookingId, "REFUND");
                if (hasRefund) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking has already been refunded");
                }
                Wallet customerWallet = transactions.get(0).getWallet();
                BigDecimal refundAmount = transactions.get(0).getAmount();
                // Tạo giao dịch hoàn tiền (REFUND)
                CustomerTransaction refundTransaction = CustomerTransaction.builder()
                        .wallet(customerWallet)
                        .booking(booking)
                        .transactionType("REFUND")
                        .amount(refundAmount)
                        .description("Refund for canceled booking by Chef.")
                        .status("COMPLETED")
                        .isDeleted(false)
                        .build();
                customerTransactionRepository.save(refundTransaction);

                customerWallet.setBalance(customerWallet.getBalance().add(refundAmount));
                walletRepository.save(customerWallet);
                bookingDetail.setStatus("REFUNDED");
                bookingDetailRepository.save(bookingDetail);

                // Gửi thông báo cho customer về việc hoàn tiền
                NotificationRequest refundNotification = NotificationRequest.builder()
                        .userId(booking.getCustomer().getId())
                        .title("Booking Canceled and Refund Issued")
                        .body("Your booking #" + booking.getBookingType() +" with Chef "+ booking.getChef().getUser().getFullName()+" has been canceled by the chef. A refund of " +
                                refundAmount + " has been issued to your wallet.")
                        .bookingId(booking.getId())
                        .screen("CustomerBookingManagementScreen")
                        .build();
                notificationService.sendPushNotification(refundNotification);
            }
        }else{
            bookingDetail.setStatus("CANCELED");
            bookingDetailRepository.save(bookingDetail);
        }

        booking.setStatus("CANCELED");
        booking = bookingRepository.save(booking);
        chefService.updateReputation(chef,-3);

        NotificationRequest customerNotification = NotificationRequest.builder()
                .userId(chef.getId())
                .title("Booking Cancel - Reputation Points Penalized")
                .body("Your booking #" + booking.getBookingType() + " was cancelled. As a result, your reputation points have been penalized for cancelling the booking.")
                .bookingId(booking.getId())
                .notiType("PENALDO_NOTIFY")
                .screen("ChefDashboard")
                .build();
        notificationService.sendPushNotification(customerNotification);

        return modelMapper.map(booking, BookingResponseDto.class);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelLongTermBookingFromChef(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with userId"));
        if (!chef.getId().equals(booking.getChef().getId())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef not in booking.");
        }
        if (!"LONG_TERM".equalsIgnoreCase(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This is not a long-term booking");
        }
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) &&
                !"CONFIRMED_PARTIALLY_PAID".equalsIgnoreCase(booking.getStatus()) &&
                !"CONFIRMED_PAID".equalsIgnoreCase(booking.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking cannot be cancelled.");
        }
        List<BookingDetail> allDetails = bookingDetailRepository.findByBookingId(bookingId);
        if (allDetails.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "No booking details found");
        }
        boolean hasTodaySession = allDetails.stream()
                .anyMatch(detail -> detail.getSessionDate().isEqual(LocalDate.now()));
        if (hasTodaySession) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel booking with a session happening today.");
        }

        List<BookingDetail> futureDetails = allDetails.stream()
                .filter(detail -> detail.getSessionDate().isAfter(LocalDate.now()))
                .toList();

        BigDecimal totalRefund = BigDecimal.ZERO;
        for (BookingDetail detail : futureDetails) {
            PaymentCycle paymentCycle = getPaymentCycleForBookingDetail(detail);
            if (paymentCycle!=null) {
                if ("PAID".equalsIgnoreCase(paymentCycle.getStatus())) {
                    if(!detail.getStatus().equalsIgnoreCase("COMPLETED")){
                        detail.setStatus("REFUNDED");
                        totalRefund = totalRefund.add(detail.getTotalPrice());
                    }
                } else {
                    paymentCycle.setStatus("CANCELED");
                    paymentCycleRepository.save(paymentCycle);
                    detail.setStatus("CANCELED");
                }
                bookingDetailRepository.save(detail);

            }
        }
        Wallet customerWallet = walletRepository.findByUserId(booking.getCustomer().getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for customer."));

        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            if(booking.getDepositPaid()!=null){
                customerWallet.setBalance(customerWallet.getBalance().add(totalRefund).add(booking.getDepositPaid()));
            }else{
                customerWallet.setBalance(customerWallet.getBalance().add(totalRefund));
            }
            CustomerTransaction refundTransaction = CustomerTransaction.builder()
                    .wallet(customerWallet)
                    .booking(booking)
                    .transactionType("REFUND")
                    .amount(totalRefund)
                    .description("Refund for canceled booking by Chef.")
                    .status("COMPLETED")
                    .isDeleted(false)
                    .build();
            customerTransactionRepository.save(refundTransaction);
            boolean hasCompletedDetail = allDetails.stream()
                    .anyMatch(detail -> "COMPLETED".equalsIgnoreCase(detail.getStatus()));
            if (hasCompletedDetail) {
                booking.setStatus("COMPLETED");
                booking.setTotalPrice(booking.getTotalPrice().subtract(totalRefund));
                booking.setDepositPaid(BigDecimal.ZERO);
            } else {
                booking.setStatus("CANCELED");
            }
            booking = bookingRepository.save(booking);

        }

        chefService.updateReputation(chef,-5);

        NotificationRequest customerNotification = NotificationRequest.builder()
                .userId(chef.getId())
                .title("Booking Cancel - Reputation Points Penalized")
                .body("Your booking #" + booking.getBookingType() + " was cancelled. As a result, your reputation points have been penalized for cancelling the booking.")
                .bookingId(booking.getId())
                .notiType("PENALDO_NOTIFY")
                .screen("ChefDashboard")
                .build();
        notificationService.sendPushNotification(customerNotification);
        NotificationRequest refundNotification = NotificationRequest.builder()
                .userId(booking.getCustomer().getId())
                .title("Booking Canceled and Refund Issued")
                .body("Your booking #" + booking.getBookingType() +" with Chef "+ booking.getChef().getUser().getFullName()+" has been canceled by the chef. A refund of " +
                        totalRefund + " has been issued to your wallet.")
                .bookingId(booking.getId())
                .screen("CustomerBookingManagementScreen")
                .build();
        notificationService.sendPushNotification(refundNotification);
        return modelMapper.map(booking, BookingResponseDto.class);
    }
    private PaymentCycle getPaymentCycleForBookingDetail(BookingDetail bookingDetail) {
        Booking booking = bookingDetail.getBooking();
        LocalDate sessionDate = bookingDetail.getSessionDate();
        return paymentCycleRepository.findByBookingId(booking.getId())
                .stream()
                .filter(paymentCycle -> !sessionDate.isBefore(paymentCycle.getStartDate()) && !sessionDate.isAfter(paymentCycle.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "No valid payment cycle found for the given session date."));
    }

    @Override
    @Transactional
    public BookingResponseDto cancelLongTermBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!"LONG_TERM".equalsIgnoreCase(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This is not a long-term booking");
        }

        String status = booking.getStatus();
        if (!List.of("PENDING", "PENDING_FIRST_CYCLE", "CONFIRMED", "DEPOSITED").contains(status.toUpperCase())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This booking status cannot be canceled");
        }
        List<PaymentCycle> paymentCycles = paymentCycleRepository.findByBookingId(bookingId);
        if (paymentCycles.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "No payment cycles found for this booking");
        }
        boolean hasPaidCycle = paymentCycles.stream()
                .anyMatch(cycle -> "PAID".equalsIgnoreCase(cycle.getStatus()));
        if (hasPaidCycle) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel booking because some payment cycles are already PAID. Consider canceling individual cycles.");
        }
        for (PaymentCycle cycle : paymentCycles) {
            cycle.setStatus("CANCELED");
            paymentCycleRepository.save(cycle);
        }
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingId(bookingId);
        for (BookingDetail detail : bookingDetails) {
            detail.setStatus("CANCELED");
            bookingDetailRepository.save(detail);
        }
        // Hoàn tiền nếu là CONFIRMED hoặc DEPOSITED
        if (status.equals("CONFIRMED") || status.equals("DEPOSITED")) {
            List<CustomerTransaction> depositTransactions = customerTransactionRepository
                    .findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(bookingId, "INITIAL_PAYMENT", "COMPLETED");

            if (!depositTransactions.isEmpty()) {
                boolean hasRefund = customerTransactionRepository
                        .existsByBookingIdAndTransactionType(bookingId, "REFUND");

                if (hasRefund) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking has already been refunded");
                }

                CustomerTransaction depositTransaction = depositTransactions.get(0);
                Wallet customerWallet = depositTransaction.getWallet();
                BigDecimal refundAmount = depositTransaction.getAmount();

                // Tạo giao dịch hoàn tiền
                CustomerTransaction refundTransaction = CustomerTransaction.builder()
                        .wallet(customerWallet)
                        .booking(booking)
                        .transactionType("REFUND")
                        .amount(refundAmount)
                        .description("Refund for canceled long-term booking")
                        .status("COMPLETED")
                        .isDeleted(false)
                        .build();

                customerTransactionRepository.save(refundTransaction);

                // Cộng tiền vào ví
                customerWallet.setBalance(customerWallet.getBalance().add(refundAmount));
                walletRepository.save(customerWallet);

                // Gửi thông báo cho customer
                NotificationRequest customerNotification = NotificationRequest.builder()
                        .userId(booking.getCustomer().getId())
                        .title("Refund Successful")
                        .body("Your deposit for the canceled long-term booking has been refunded.")
                        .bookingId(booking.getId())
                        .screen("CustomerWalletScreen")
                        .build();

                notificationService.sendPushNotification(customerNotification);
            }
        }

        // Nếu là CONFIRMED thì gửi thông báo cho chef
        if (status.equals("CONFIRMED")) {
            NotificationRequest chefNotification = NotificationRequest.builder()
                    .userId(booking.getChef().getUser().getId())
                    .title("Long-term Booking Canceled")
                    .body("The customer has canceled the confirmed long-term booking.")
                    .bookingId(booking.getId())
                    .screen("ChefBookingManagementScreen")
                    .build();

            notificationService.sendPushNotification(chefNotification);
        }

        // Cập nhật trạng thái booking
        booking.setStatus("CANCELED");
        bookingRepository.save(booking);

        return modelMapper.map(booking, BookingResponseDto.class);
    }

    @Override
    public BookingResponseDto cancelLongTermBooking2(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!customer.getId().equals(booking.getCustomer().getId())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "User not in booking.");
        }
        if (!"LONG_TERM".equalsIgnoreCase(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This is not a long-term booking");
        }
        String status = booking.getStatus();
        if (!List.of("PENDING", "PENDING_FIRST_CYCLE", "PAID_FIRST_CYCLE", "CONFIRMED", "DEPOSITED","CONFIRMED_PARTIALLY_PAID","CONFIRMED_PAID").contains(status.toUpperCase())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This booking status cannot be canceled");
        }
        List<BookingDetail> allDetails = bookingDetailRepository.findByBookingId(bookingId);
        if (allDetails.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "No booking details found");
        }
        if (List.of("CONFIRMED_PARTIALLY_PAID","CONFIRMED_PAID").contains(status.toUpperCase())) {
            boolean hasTodaySession = allDetails.stream()
                    .anyMatch(detail -> detail.getSessionDate().isBefore(LocalDate.now().plusDays(1)) || detail.getStatus().equalsIgnoreCase("IN_PROGRESS"));
            if (hasTodaySession) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel booking less than 1 day before session date or has session today.");
            }
        }

        List<BookingDetail> futureDetails = allDetails.stream()
                .filter(detail -> detail.getSessionDate().isAfter(LocalDate.now()))
                .toList();
        List<PaymentCycle> allCycles = paymentCycleRepository.findByBookingId(bookingId);
        BigDecimal totalRefund = BigDecimal.ZERO;
        Wallet customerWallet = walletRepository.findByUserId(customer.getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for customer."));

        switch (status.toUpperCase()) {
            case "CONFIRMED":
            case "PENDING":
            case "PENDING_FIRST_CYCLE":
                for (PaymentCycle cycle : allCycles) {
                    cycle.setStatus("CANCELED");
                    paymentCycleRepository.save(cycle);
                }
                for (BookingDetail detail : allDetails) {
                    detail.setStatus("CANCELED");
                    bookingDetailRepository.save(detail);
                }
                booking.setStatus("CANCELED");
                break;

            case "PAID_FIRST_CYCLE":
                Optional<PaymentCycle> paidCycleOpt = allCycles.stream()
                        .filter(cycle -> "PAID".equalsIgnoreCase(cycle.getStatus()))
                        .findFirst();
                if (paidCycleOpt.isPresent()) {
                    PaymentCycle paidCycle = paidCycleOpt.get();
                    for (BookingDetail detail : allDetails) {
                        LocalDate sessionDate = detail.getSessionDate();
                        if (!sessionDate.isBefore(paidCycle.getStartDate()) && !sessionDate.isAfter(paidCycle.getEndDate())) {
                            detail.setStatus("REFUNDED");
                            totalRefund = totalRefund.add(detail.getTotalPrice());
                            bookingDetailRepository.save(detail);
                        }
                        detail.setStatus("CANCELED");
                        bookingDetailRepository.save(detail);
                    }

                }
                if (booking.getDepositPaid() != null) {
                    totalRefund = totalRefund.add(booking.getDepositPaid());
                    booking.setDepositPaid(BigDecimal.ZERO);
                }
                booking.setStatus("CANCELED");
                break;

            case "DEPOSITED":
                for (PaymentCycle cycle : allCycles) {
                    cycle.setStatus("CANCELED");
                    paymentCycleRepository.save(cycle);
                }
                for (BookingDetail detail : allDetails) {
                    detail.setStatus("CANCELED");
                    bookingDetailRepository.save(detail);
                }
                booking.setStatus("CANCELED");
                if (booking.getDepositPaid() != null) {
                    totalRefund = totalRefund.add(booking.getDepositPaid());
                    booking.setDepositPaid(BigDecimal.ZERO);
                }
                booking.setStatus("CANCELED");
                break;

            case "CONFIRMED_PARTIALLY_PAID":
            case "CONFIRMED_PAID":
                for (BookingDetail detail : futureDetails) {
                    PaymentCycle paymentCycle = getPaymentCycleForBookingDetail(detail);
                    if (paymentCycle!=null) {
                        if ("PAID".equalsIgnoreCase(paymentCycle.getStatus())) {
                            if(!detail.getStatus().equalsIgnoreCase("COMPLETED")){
                                detail.setStatus("REFUNDED");
                                totalRefund = totalRefund.add(detail.getTotalPrice());
                            }
                        } else {
                            paymentCycle.setStatus("CANCELED");
                            paymentCycleRepository.save(paymentCycle);
                            detail.setStatus("CANCELED");
                        }
                        bookingDetailRepository.save(detail);
                    }
                }
                break;
        }
        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            customerWallet.setBalance(customerWallet.getBalance().add(totalRefund));
            walletRepository.save(customerWallet);
            CustomerTransaction refundTransaction = CustomerTransaction.builder()
                    .wallet(customerWallet)
                    .booking(booking)
                    .transactionType("REFUND")
                    .amount(totalRefund)
                    .description("Refund for canceled booking.")
                    .status("COMPLETED")
                    .isDeleted(false)
                    .build();
            customerTransactionRepository.save(refundTransaction);
            boolean hasCompletedDetail = allDetails.stream()
                    .anyMatch(detail -> "COMPLETED".equalsIgnoreCase(detail.getStatus()));
            if (hasCompletedDetail) {
                booking.setStatus("COMPLETED");
                booking.setTotalPrice(booking.getTotalPrice().subtract(totalRefund));
                booking.setDepositPaid(BigDecimal.ZERO);
            } else {
                booking.setStatus("CANCELED");
            }
        }
        booking = bookingRepository.save(booking);

        NotificationRequest customerNotification = NotificationRequest.builder()
                .userId(customer.getId())
                .title("Booking Canceled")
                .body("You have canceled your booking #" + booking.getBookingType() +" with Chef "+ booking.getChef().getUser().getFullName()+ ". A refund of " +
                        totalRefund + " has been issued to your wallet (if applicable).")
                .bookingId(booking.getId())
                .screen("CustomerBookingManagementScreen")
                .build();
        notificationService.sendPushNotification(customerNotification);

        if(!List.of("CONFIRMED","CONFIRMED_PARTIALLY_PAID","CONFIRMED_PAID").contains(status.toUpperCase())){
            NotificationRequest notifyChef = NotificationRequest.builder()
                    .userId(booking.getChef().getId())
                    .title("Booking Canceled by Customer")
                    .body("Customer "+customer.getFullName()+ " has canceled booking #" + booking.getBookingType() + ".")
                    .bookingId(booking.getId())
                    .screen("ChefDashboard")
                    .build();
            notificationService.sendPushNotification(notifyChef);
        }
        return modelMapper.map(booking, BookingResponseDto.class);
    }

    @Override
    public Set<LocalDate> getFullyBookedDates(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));

        LocalDate today = LocalDate.now();

        List<Object[]> results = bookingDetailRepository.countFutureBookingsByChef(chef, today);

        Set<LocalDate> fullyBookedDates = new HashSet<>();
        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];

            if (count >= 4) {
                fullyBookedDates.add(date);
            }
        }
        return fullyBookedDates;
    }


    @Scheduled(cron = "0 0/10 * * * *") // chạy mỗi 10 phút
    @Transactional
    public void markOverdueAndRefundBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = bookingRepository
                .findByStatusIn(List.of("PENDING", "PENDING_FIRST_CYCLE", "PAID", "DEPOSITED", "PAID_FIRST_CYCLE"));

        for (Booking booking : bookings) {
            String status = booking.getStatus().toUpperCase();
            boolean isPendingStatus = status.equals("PENDING") || status.equals("PENDING_FIRST_CYCLE");
            boolean isPaidStatus = status.equals("PAID") || status.equals("DEPOSITED") || status.equals("PAID_FIRST_CYCLE");
            // Kiểm tra với updatedAt > 1 tiếng cho PENDING
            if (isPendingStatus) {
                if (booking.getUpdatedAt().isBefore(now.minusHours(1))) {
                    booking.setStatus("OVERDUE");
                    bookingRepository.save(booking);
                    // Hủy các buổi booking detail
                    for (BookingDetail detail : booking.getBookingDetails()) {
                        detail.setStatus("CANCELED");
                    }
                    bookingDetailRepository.saveAll(booking.getBookingDetails());
                    // Gửi thông báo
                    notificationService.sendPushNotification(NotificationRequest.builder()
                            .userId(booking.getCustomer().getId())
                            .title("Booking Expired")
                            .body("Your booking has expired after 1 hour of inactivity.")
                            .bookingId(booking.getId())
                            .screen("CustomerBookingScreen")
                            .build());
                }
            }
            // Kiểm tra ngày sửa < hôm nay với PAID/DEPOSITED → refund
            else if (isPaidStatus && Duration.between(booking.getUpdatedAt(), now).toHours() > 6) {
                // Kiểm tra đã hoàn tiền chưa
                boolean hasRefund = customerTransactionRepository
                        .existsByBookingIdAndTransactionType(booking.getId(), "REFUND");
                if (hasRefund) continue;
                String transactionType = switch (status) {
                    case "PAID" -> "PAYMENT";
                    case "DEPOSITED", "PAID_FIRST_CYCLE" -> "INITIAL_PAYMENT";
                    default -> null;
                };
                List<CustomerTransaction> transactions = customerTransactionRepository
                        .findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(
                                booking.getId(), transactionType, "COMPLETED");
                if (transactions.isEmpty()) continue;
                CustomerTransaction original = transactions.get(0);
                Wallet wallet = original.getWallet();
                BigDecimal refundAmount = original.getAmount();

                // Cập nhật ví
                wallet.setBalance(wallet.getBalance().add(refundAmount));
                walletRepository.save(wallet);
                // Giao dịch hoàn tiền
                CustomerTransaction refund = CustomerTransaction.builder()
                        .wallet(wallet)
                        .booking(booking)
                        .transactionType("REFUND")
                        .amount(refundAmount)
                        .description("Refund for overdue booking")
                        .status("COMPLETED")
                        .isDeleted(false)
                        .build();
                customerTransactionRepository.save(refund);
                // Cập nhật booking
                booking.setStatus("OVERDUE");
                bookingRepository.save(booking);
                // Hủy các bookingDetail
                for (BookingDetail detail : booking.getBookingDetails()) {
                    detail.setStatus("CANCELED");
                }
                bookingDetailRepository.saveAll(booking.getBookingDetails());
                // Gửi thông báo
                notificationService.sendPushNotification(NotificationRequest.builder()
                        .userId(booking.getCustomer().getId())
                        .title("Booking Overdue & Refunded")
                        .body("Your booking on " + booking.getCreatedAt() + " has expired and refunded " + refundAmount)
                        .bookingId(booking.getId())
                        .screen("CustomerWalletScreen")
                        .build());
                chefService.updateReputation(booking.getChef(),-1);
            }
        }
    }


}
