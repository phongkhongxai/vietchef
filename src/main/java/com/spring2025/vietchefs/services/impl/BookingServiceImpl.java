package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.entity.Package;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.*;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailPriceLTRequest;
import com.spring2025.vietchefs.models.payload.requestModel.BookingDetailPriceRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingLTPriceRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.BookingPriceRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.BookingDetailService;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.PaymentCycleService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        List<BookingResponseDto> content = listOfBookings.stream().map(bt -> {
            BookingResponseDto dto = modelMapper.map(bt, BookingResponseDto.class);

            // Chỉ set bookingDetails khi bookingType là "single"
            if (!"single".equalsIgnoreCase(bt.getBookingType())) {
                dto.setBookingDetails(null);
            }
            return dto;
        }).collect(Collectors.toList());
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
        BookingResponseDto dto = modelMapper.map(booking, BookingResponseDto.class);

        if (!"single".equalsIgnoreCase(booking.get().getBookingType())) {
            dto.setBookingDetails(null);
        }

        return dto;
    }

    @Override
    @Transactional
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
            detailDto.setIsUpdated(true);
            BookingDetail detail = bookingDetailService.createBookingDetail(booking, detailDto);
            totalPrice = totalPrice.add(detail.getTotalPrice());
        }

        booking.setTotalPrice(totalPrice);
        Booking booking1 = bookingRepository.save(booking);
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

        // Tạo các BookingDetail
        for (BookingDetailRequestDto detailDto : dto.getBookingDetails()) {
            BookingDetail detail = bookingDetailService.createBookingDetail(booking, detailDto);
            bookingDetailList.add(detail);
            totalPrice = totalPrice.add(detail.getTotalPrice());
        }

        booking.setTotalPrice(totalPrice);
        booking.setBookingDetails(bookingDetailList);
        bookingRepository.save(booking);

        // Chia các kỳ thanh toán
        paymentCycleService.createPaymentCycles(booking);

        // Chuyển Booking sang DTO để trả về
        return modelMapper.map(booking, BookingResponseDto.class);
    }

    @Override
    public ReviewSingleBookingResponse calculateFinalPriceForSingleBooking(BookingPriceRequestDto dto) {
        Chef chef = chefRepository.findById(dto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));

        BigDecimal totalBookingPrice = BigDecimal.ZERO;
        ReviewSingleBookingResponse reviewSingleBookingResponse = new ReviewSingleBookingResponse();

        BookingDetailPriceRequestDto detailDto = dto.getBookingDetail();
        BigDecimal totalCookTime = BigDecimal.ZERO;

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
                if (!dishIds.isEmpty()) {
                    totalCookTime = calculateService.calculateTotalCookTime(dishIds);
                    reviewSingleBookingResponse.setCookTimeMinutes(totalCookTime.multiply(BigDecimal.valueOf(60)));

                } else {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "At least one dish must be selected.");
                }
            }

            // 🔹 Tính phí dịch vụ đầu bếp (công nấu ăn)
            BigDecimal price1 = calculateService.calculateChefServiceFee(chef.getPrice(), totalCookTime);
            reviewSingleBookingResponse.setChefCookingFee(price1);

            // 🔹 Tính phí món ăn (menu hoặc món lẻ)
            BigDecimal price2 = calculateService.calculateDishPrice(detailDto.getMenuId(), dto.getGuestCount(), detailDto.getExtraDishIds());
            reviewSingleBookingResponse.setPriceOfDishes(price2);

            // 🔹 Tính phí di chuyển
            DistanceFeeResponse price3Of = calculateService.calculateTravelFee(chef.getAddress(), detailDto.getLocation());
            BigDecimal price3 = price3Of.getTravelFee();
            TimeTravelResponse ttp = calculateService.calculateArrivalTime(detailDto.getStartTime(), totalCookTime, price3Of.getDurationHours());
            reviewSingleBookingResponse.setArrivalFee(price3);
            //  Nếu khách chọn phục vụ, tính thêm phí phục vụ
            BigDecimal servingFee = BigDecimal.ZERO;
            if (detailDto.getIsServing()) {
                servingFee = calculateService.calculateServingFee(detailDto.getStartTime(), detailDto.getEndTime(), chef.getPrice());
            }
            reviewSingleBookingResponse.setChefServingFee(servingFee);
            reviewSingleBookingResponse.setPlatformFee(price1.multiply(BigDecimal.valueOf(0.12)));

            // 🔹 Tính tổng giá của BookingDetail
            BigDecimal price4 = calculateService.calculateFinalPrice(price1, price2, price3).add(servingFee);

            totalBookingPrice = totalBookingPrice.add(price4);
            reviewSingleBookingResponse.setTotalChefFeePrice(price1.multiply(BigDecimal.valueOf(0.88)).add(price2).add(price3).add(servingFee));
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

        Package bookingPackage = packageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Package not found"));
        if (dto.getBookingDetails().size() != bookingPackage.getDurationDays()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "The number of booking details must match the package duration of " + bookingPackage.getDurationDays() + " days.");
        }
        // 🔹 Tính phí di chuyển
        DistanceFeeResponse travelFeeResponse = calculateService.calculateTravelFee(chef.getAddress(), dto.getLocation());
        BigDecimal travelFee = travelFeeResponse.getTravelFee();
        BigDecimal totalBookingPrice = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        List<BookingDetailPriceResponse> detailPriceResponses = new ArrayList<>();

        for (BookingDetailPriceLTRequest detailDto : dto.getBookingDetails()) {
            BigDecimal totalCookTime = BigDecimal.ZERO;

            // 🔹 Kiểm tra xem BookingDetail đã chọn món chưa
            if (Boolean.FALSE.equals(detailDto.getIsDishSelected())) {
                //  Nếu chưa chọn món, lấy tổng thời gian nấu của 3 món lâu nhất của đầu bếp
                totalCookTime = dishRepository.findTop3LongestCookTimeByChef(chef.getId())
                        .stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

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
                    if (!dishIds.isEmpty()) {
                        totalCookTime = calculateService.calculateTotalCookTime(dishIds);
                    } else {
                        throw new VchefApiException(HttpStatus.BAD_REQUEST, "At least one dish must be selected.");
                    }
                }
            }

            // 🔹 Tính phí dịch vụ đầu bếp (công nấu ăn)
            BigDecimal chefCookingFee = calculateService.calculateChefServiceFee(chef.getPrice(), totalCookTime);

            // 🔹 Tính phí món ăn
            BigDecimal dishPrice = calculateService.calculateDishPrice(detailDto.getMenuId(), dto.getGuestCount(), detailDto.getExtraDishIds());


            // 🔹 Tính phí phục vụ nếu có
            BigDecimal servingFee = BigDecimal.ZERO;
            if (detailDto.getIsServing()) {
                servingFee = calculateService.calculateServingFee(detailDto.getStartTime(), detailDto.getEndTime(), chef.getPrice());
            }

            // 🔹 Tính tổng giá từng buổi
            BigDecimal sessionTotalPrice = calculateService.calculateFinalPrice(chefCookingFee, dishPrice, travelFee).add(servingFee);
            totalBookingPrice = totalBookingPrice.add(sessionTotalPrice);
            // 🔹 Tính thời gian di chuyển và nấu ăn
            TimeTravelResponse ttp = calculateService.calculateArrivalTime(detailDto.getStartTime(), totalCookTime, travelFeeResponse.getDurationHours());
            BigDecimal discountAmountDetail = BigDecimal.ZERO;
            // Áp dụng giảm giá từ Package
            if (bookingPackage.getDiscount() != null) {
                discountAmountDetail = sessionTotalPrice.multiply(bookingPackage.getDiscount());
                discountAmount = discountAmount.add(discountAmountDetail);
                sessionTotalPrice = sessionTotalPrice.subtract(discountAmountDetail);
            }

            // 🔹 Tạo response cho từng BookingDetail
            BookingDetailPriceResponse detailResponse = new BookingDetailPriceResponse();
            detailResponse.setMenuId(detailDto.getMenuId());
            detailResponse.setSessionDate(detailDto.getSessionDate());
            detailResponse.setDiscountAmout(discountAmountDetail);
            detailResponse.setTotalPrice(sessionTotalPrice);
            detailResponse.setChefCookingFee(chefCookingFee);
            detailResponse.setPriceOfDishes(dishPrice);
            detailResponse.setArrivalFee(travelFee);
            detailResponse.setChefServingFee(servingFee);
            detailResponse.setTimeBeginTravel(ttp.getTimeBeginTravel());
            detailResponse.setTimeBeginCook(ttp.getTimeBeginCook());
            detailResponse.setStartTime(detailDto.getStartTime());
            detailResponse.setEndTime(detailDto.getEndTime());
            detailResponse.setLocation(dto.getLocation());
            detailResponse.setIsServing(detailDto.getIsServing());
            detailResponse.setDishes(detailDto.getDishes());
            detailResponse.setPlatformFee(chefCookingFee.multiply(BigDecimal.valueOf(0.12)));
            detailResponse.setTotalChefFeePrice(chefCookingFee.multiply(BigDecimal.valueOf(0.88)).add(dishPrice).add(travelFee).add(servingFee));
            detailResponse.setIsUpdated(detailDto.getIsDishSelected());
            detailPriceResponses.add(detailResponse);


        }

        // Tạo response tổng hợp
        ReviewLongTermBookingResponse reviewResponse = new ReviewLongTermBookingResponse();
        reviewResponse.setTotalPrice(totalBookingPrice);
        reviewResponse.setDiscountAmount(discountAmount);
        reviewResponse.setBookingDetails(detailPriceResponses);
        //reviewResponse.setPaymentCycles(paymentCycles);

        return reviewResponse;
    }

    @Override
    @Transactional
    public BookingResponseDto updateBookingStatusConfirm(Long bookingId, Long userId, boolean isConfirmed) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId));
        Chef chef = chefRepository.findByUserId(userId)
                        .orElseThrow(() ->new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found with userId"));
        if(!chef.getId().equals(booking.getChef().getId())){
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef not in booking.");
        }

        // 3. Kiểm tra trạng thái và loại Booking trước khi xác nhận/từ chối
        boolean isSinglePaid = "PAID".equals(booking.getStatus()) && "SINGLE".equals(booking.getBookingType());
        boolean isLongTermDeposited = "DEPOSITED".equals(booking.getStatus()) && "LONG_TERM".equals(booking.getBookingType());

        if (isConfirmed) {
            // Trường hợp xác nhận booking
            if (isSinglePaid || isLongTermDeposited) {
                if (isSinglePaid) {
                    List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
                    for (BookingDetail detail : bookingDetails) {
                        detail.setStatus("LOCKED");
                        bookingDetailRepository.save(detail);
                    }
                }
                if (isLongTermDeposited) {
                    List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
                    for (BookingDetail detail : bookingDetails) {
                        if(detail.getIsUpdated()){
                            detail.setStatus("LOCKED");
                            bookingDetailRepository.save(detail);
                        }
                    }
                }
                booking.setStatus("CONFIRMED");
                bookingRepository.save(booking);
                return modelMapper.map(booking, BookingResponseDto.class);
            } else {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking does not meet the conditions for confirmation.");
            }
        } else {
            // Trường hợp từ chối booking và hoàn tiền lại
            if (isSinglePaid || isLongTermDeposited) {
                List<BookingDetail> bookingDetails = bookingDetailRepository.findByBooking(booking);
                for (BookingDetail detail : bookingDetails) {
                    detail.setStatus("CANCELLED");
                    bookingDetailRepository.save(detail);
                }
                // 4. Lấy ví của khách hàng để hoàn tiền
                Wallet wallet = walletRepository.findByUserId(booking.getCustomer().getId())
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for customer."));

                BigDecimal refundAmount = isSinglePaid ? booking.getTotalPrice() : booking.getDepositPaid();

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

                // 7. Cập nhật trạng thái Booking thành REJECTED
                booking.setStatus("REJECTED");
                bookingRepository.save(booking);

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

        // Trả về Booking đã thanh toán
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
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) && !"PARTIALLY_PAID".equalsIgnoreCase(booking.getStatus())) {
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
        BigDecimal remainingAmount = amountDue; // Số tiền thực tế cần thanh toán

        if (depositPaid.compareTo(BigDecimal.ZERO) > 0) {
                booking.setDepositPaid(BigDecimal.ZERO);
                remainingAmount = amountDue.subtract(depositPaid);
        }
        // 6. Trừ tiền trong ví
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (wallet.getBalance().compareTo(remainingAmount) < 0) {
                throw new VchefApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in wallet.");
            }
            // Trừ tiền trong ví
            wallet.setBalance(wallet.getBalance().subtract(remainingAmount));
            walletRepository.save(wallet);
        }

        // 7. Cập nhật trạng thái của PaymentCycle → "PAID"
        paymentCycle.setStatus("PAID");
        paymentCycleRepository.save(paymentCycle);

        // 8. Cập nhật trạng thái của BookingDetail thuộc PaymentCycle này
        for (BookingDetail detail : bookingDetails) {
            if (!detail.getSessionDate().isBefore(paymentCycle.getStartDate()) &&
                    !detail.getSessionDate().isAfter(paymentCycle.getEndDate())) {
                detail.setStatus("LOCKED");
            }
        }
        bookingDetailRepository.saveAll(bookingDetails);


        // 9. Cập nhật trạng thái Booking dựa vào tình trạng PaymentCycle
        List<PaymentCycle> allCycles = paymentCycleRepository.findByBookingId(booking.getId());

        long paidCount = allCycles.stream().filter(pc -> "PAID".equals(pc.getStatus())).count();
        if (paidCount == 0) {
            booking.setStatus("CONFIRMED");
        } else if (paidCount < allCycles.size()) {
            booking.setStatus("PARTIALLY_PAID");
        } else {
            booking.setStatus("CONFIRMED_PAID");
        }
        bookingRepository.save(booking);

        // 10. Ghi lại giao dịch vào bảng Transaction
        CustomerTransaction transaction = CustomerTransaction.builder()
                .wallet(wallet)
                .booking(booking)
                .transactionType("PAYMENT")
                .amount(amountDue)
                .status("COMPLETED")
                .isDeleted(false)
                .description("Payment for PaymentCycle #" + paymentCycle.getId())
                .build();
        customerTransactionRepository.save(transaction);

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
    public BookingResponseDto depositBooking(Long bookingId, Long userId) {
        // 1. Lấy Booking theo ID
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found with ID: " + bookingId));

        // 2. Kiểm tra điều kiện booking phải là LONG_TERM và PENDING
        if (!"PENDING".equals(booking.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking is not in PENDING status.");
        }
        if (!"LONG_TERM".equals(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking is not Long-Term.");
        }

        // 3. Lấy ví của người dùng
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found for customer."));

        // 4. Kiểm tra số dư có đủ thanh toán tiền đặt cọc không
        BigDecimal depositAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.05)); // Lấy số tiền đặt cọc
        if (wallet.getBalance().compareTo(depositAmount) < 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in the wallet.");
        }

        // 5. Trừ tiền đặt cọc trong ví
        wallet.setBalance(wallet.getBalance().subtract(depositAmount));
        walletRepository.save(wallet);

        // 6. Cập nhật trạng thái Booking thành DEPOSITED
        booking.setStatus("DEPOSITED");
        booking.setDepositPaid(depositAmount); // Lưu số tiền đặt cọc đã thanh toán
        bookingRepository.save(booking);

        // 7. Ghi lại giao dịch vào bảng Transaction
        CustomerTransaction transaction = new CustomerTransaction();
        transaction.setWallet(wallet);
        transaction.setBooking(booking);
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(depositAmount);
        transaction.setStatus("COMPLETED");
        transaction.setDescription("Deposit for Long-Term Booking #" + booking.getId() +
                " with " + booking.getChef().getUser().getFullName() + " Chef.");
        customerTransactionRepository.save(transaction);

        // 8. Trả về Booking đã thanh toán đặt cọc
        return modelMapper.map(booking, BookingResponseDto.class);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelSingleBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!"SINGLE".equalsIgnoreCase(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This is not a single booking");
        }
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingId(bookingId);
        if (bookingDetails.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Booking detail not found");
        }
        BookingDetail bookingDetail = bookingDetails.get(0);
        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus()) &&
                bookingDetail.getSessionDate().isBefore(LocalDate.now().plusDays(2))) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Cannot cancel booking less than 2 days before session date");
        }

        // Nếu booking là PENDING thì chỉ cần hủy, không cần hoàn tiền
        if ("PENDING".equalsIgnoreCase(booking.getStatus())) {
            booking.setStatus("CANCELED");
            bookingRepository.save(booking);
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
                        .description("Refund for canceled booking")
                        .status("COMPLETED")
                        .isDeleted(false)
                        .build();

                customerTransactionRepository.save(refundTransaction);

                customerWallet.setBalance(customerWallet.getBalance().add(refundAmount));
                walletRepository.save(customerWallet);
            }
        }

        // Cập nhật trạng thái booking
        bookingDetail.setStatus("CANCELED");
        bookingDetailRepository.save(bookingDetail);
        booking.setStatus("CANCELED");
        bookingRepository.save(booking);

        // Trả về DTO phản hồi
        return modelMapper.map(booking, BookingResponseDto.class);

    }

    @Override
    @Transactional
    public BookingResponseDto cancelLongTermBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!"LONG-TERM".equalsIgnoreCase(booking.getBookingType())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "This is not a long-term booking");
        }

        List<PaymentCycle> paymentCycles = paymentCycleRepository.findByBookingId(bookingId);
        if (paymentCycles.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "No payment cycles found for this booking");
        }
        // Kiểm tra xem có kỳ nào đã thanh toán không
        boolean hasPaidCycle = paymentCycles.stream()
                .anyMatch(cycle -> "PAID".equalsIgnoreCase(cycle.getStatus()));

        if (hasPaidCycle) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel full booking because some payment cycles are already PAID. Consider canceling individual cycles.");
        }

        // Nếu booking là CONFIRMED, chỉ cho phép hủy
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) ) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel paid booking.");
        }

        // Hủy tất cả các kỳ thanh toán (nếu chưa có kỳ nào PAID)
        for (PaymentCycle cycle : paymentCycles) {
                cycle.setStatus("CANCELED");
                paymentCycleRepository.save(cycle);
        }
        // Cập nhật trạng thái của BookingDetail
        List<BookingDetail> bookingDetails = bookingDetailRepository.findByBookingId(bookingId);
        for (BookingDetail detail : bookingDetails) {
            detail.setStatus("CANCELED");
            bookingDetailRepository.save(detail);
        }

        if ("DEPOSITED".equalsIgnoreCase(booking.getStatus())) {
            List<CustomerTransaction>  depositTransactions = customerTransactionRepository
                    .findByBookingIdAndTransactionTypeAndIsDeletedFalseAndStatus(booking.getId(), "DEPOSIT","COMPLETED");

            if (!depositTransactions.isEmpty()) {
                boolean hasRefund = customerTransactionRepository.existsByBookingIdAndTransactionType(bookingId, "REFUND");
                if (hasRefund) {
                    throw new VchefApiException(HttpStatus.BAD_REQUEST, "Booking has already been refunded");
                }

                Wallet customerWallet = depositTransactions.get(0).getWallet();
                BigDecimal refundAmount = depositTransactions.get(0).getAmount();

                // Tạo giao dịch hoàn tiền (REFUND)
                CustomerTransaction refundTransaction = CustomerTransaction.builder()
                        .wallet(customerWallet)
                        .booking(booking)
                        .transactionType("REFUND")
                        .amount(refundAmount)
                        .description("Refund for canceled booking")
                        .status("COMPLETED")
                        .isDeleted(false)
                        .build();

                customerTransactionRepository.save(refundTransaction);

                customerWallet.setBalance(customerWallet.getBalance().add(refundAmount));
                walletRepository.save(customerWallet);
            }
        }

        // Cập nhật trạng thái booking
        booking.setStatus("CANCELED");
        bookingRepository.save(booking);

        return modelMapper.map(booking, BookingResponseDto.class);
    }
}
