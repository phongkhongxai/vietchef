package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.*;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChefRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefsResponse;
import com.spring2025.vietchefs.repositories.*;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.ReviewService;
import com.spring2025.vietchefs.services.WalletService;
import com.spring2025.vietchefs.utils.AppConstants;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChefServiceImpl implements ChefService {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private WalletService walletService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private ChefTransactionRepository chefTransactionRepository;
    @Autowired
    private DistanceService distanceService;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private ReviewService reviewService;

    @Override
    public ChefDto createChef(ChefDto chefDto) {
        User user = userRepository.findById(chefDto.getUser().getId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"User not found"));

        // Kiểm tra role của User, chỉ cho phép nếu role là "chef"
        if (!"ROLE_CHEF".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new VchefApiException(HttpStatus.UNAUTHORIZED,"User does not have chef role.");
        }

        // Kiểm tra xem User đã có hồ sơ Chef chưa
        Optional<Chef> existingChef = chefRepository.findByUser(user);
        if(existingChef.isPresent()){
            throw new VchefApiException(HttpStatus.BAD_REQUEST,"Chef profile already exists for this user.");
        }

        Chef chef = new Chef();
        chef.setUser(user);
        chef.setBio(chefDto.getBio());
        chef.setDescription(chefDto.getDescription());
        chef.setAddress(chefDto.getAddress());
        chef.setCountry(chefDto.getCountry());
        chef.setPrice(chefDto.getPrice());
        chef.setStatus(chefDto.getStatus() != null ? chefDto.getStatus() : "active");
        double[] latLng = distanceService.getLatLngFromAddress(chefDto.getAddress());
        chef.setLatitude(latLng[0]);
        chef.setLongitude(latLng[1]);
        chef = chefRepository.save(chef);

        return modelMapper.map(chefRepository.save(chef), ChefDto.class);
    }

    @Override
    public void updateReputation(Chef chef, int delta) {
        int updated = chef.getReputationPoints() + delta;
        updated = Math.max(0, Math.min(100, updated));
        chef.setReputationPoints(updated);
        if (updated < 60) {
            chef.setStatus("LOCKED");
        }
        chefRepository.save(chef);
    }

    @Override
    public ChefResponseDto getChefById(Long id) {
        Optional<Chef> chef = chefRepository.findById(id);
        if(chef.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: "+ id);

        }
        ChefResponseDto result = modelMapper.map(chef.get(), ChefResponseDto.class);
        result.setAverageRating(reviewService.getAverageRatingForChef(id)); 
        return result;
    }

    @Override
    public ChefResponseDto registerChefRequest(Long userId, ChefRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Kiểm tra nếu User đã có hồ sơ Chef
        if (chefRepository.findByUser(user).isPresent()) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef profile already exists for this user.");
        }
        // Đặt trạng thái chờ duyệt (PENDING)
        Chef chef = new Chef();
        chef.setUser(user);
        chef.setYearsOfExperience(requestDto.getYearsOfExperience());
        chef.setBio(requestDto.getBio());
        chef.setDescription(requestDto.getDescription());
        chef.setAddress(requestDto.getAddress());
        chef.setCountry(requestDto.getCountry());
        chef.setPrice(requestDto.getPrice() != null ? requestDto.getPrice() : BigDecimal.valueOf(10));
        chef.setMaxServingSize(requestDto.getMaxServingSize() != null ? requestDto.getMaxServingSize() : 10);
        double[] latLng = distanceService.getLatLngFromAddress(requestDto.getAddress());
        chef.setLatitude(latLng[0]);
        chef.setLongitude(latLng[1]);
        chef.setStatus("PENDING");
        chef.setIsDeleted(false);

        Chef ch = chefRepository.save(chef);
        return modelMapper.map(ch, ChefResponseDto.class);
    }

    @Override
    public ChefResponseDto approveChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        if (!chef.getStatus().equals("PENDING")) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef is not in PENDING status.");
        }
        chef.setStatus("ACTIVE");
        Role chefRole = roleRepository.findByRoleName("ROLE_CHEF")
                                  .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Role ROLE_CHEF not found"));
        User user = chef.getUser();
        user.setRole(chefRole);
        userRepository.save(user);

        walletService.updateWalletType(user.getId(), "CHEF");

        chefRepository.save(chef);
        emailVerificationService.sendChefApprovalEmail(chef.getUser());
        return modelMapper.map(chef, ChefResponseDto.class);
    }

    @Override
    public ChefResponseDto rejectChef(Long chefId, String reason) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        if (!chef.getStatus().equals("PENDING")) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef is not in PENDING status.");
        }
        chef.setStatus("REJECTED");
        chefRepository.save(chef);
        emailVerificationService.sendChefRejectionEmail(chef.getUser(), reason);

        return modelMapper.map(chef, ChefResponseDto.class);
    }

    @Override
    public ChefsResponse getAllChefsPending(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Chef> chefs = chefRepository.findByStatusAndIsDeletedFalse("PENDING", pageable);

        // get content for page object
        List<Chef> listOfChefs = chefs.getContent();

        List<ChefResponseDto> content = listOfChefs.stream()
                .map(chef -> {
                    ChefResponseDto dto = modelMapper.map(chef, ChefResponseDto.class);
                    dto.setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));
                    return dto;
                })
                .collect(Collectors.toList());

        ChefsResponse templatesResponse = new ChefsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(chefs.getNumber());
        templatesResponse.setPageSize(chefs.getSize());
        templatesResponse.setTotalElements(chefs.getTotalElements());
        templatesResponse.setTotalPages(chefs.getTotalPages());
        templatesResponse.setLast(chefs.isLast());
        return templatesResponse;
    }

    @Override
    public ChefsResponse getAllChefs(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort;
        // Xử lý trường hợp sắp xếp theo đánh giá
        if (sortBy.equals("rating") || AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            sort = Sort.by("id").ascending(); // Default sort, we'll handle rating sort later
        } else {
            sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
        }

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Chef> chefs = chefRepository.findByStatusAndIsDeletedFalse("ACTIVE", pageable);

        // get content for page object
        List<Chef> listOfChefs = chefs.getContent();

        List<ChefResponseDto> content = listOfChefs.stream()
                .map(chef -> {
                    ChefResponseDto dto = modelMapper.map(chef, ChefResponseDto.class);
                    // Lấy đánh giá trung bình cho chef
                    dto.setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Sắp xếp theo rating nếu được yêu cầu
        if (sortBy.equals("rating")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if (AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            content.sort(Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        ChefsResponse templatesResponse = new ChefsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(chefs.getNumber());
        templatesResponse.setPageSize(chefs.getSize());
        templatesResponse.setTotalElements(chefs.getTotalElements());
        templatesResponse.setTotalPages(chefs.getTotalPages());
        templatesResponse.setLast(chefs.isLast());
        return templatesResponse;
    }

    @Override
    public ChefsResponse getAllChefsNearBy(double customerLat, double customerLng, double distance, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort;
        // Xử lý trường hợp sắp xếp theo đánh giá
        if (sortBy.equals("rating") || sortBy.equals("distance")|| AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            sort = Sort.by("id").ascending();
        } else {
            sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
        }
        
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Chef> chefs = chefRepository.findByStatusAndIsDeletedFalseAndDistance("ACTIVE", customerLat, customerLng, distance, pageable);
        // Lấy danh sách các đầu bếp từ kết quả
        List<Chef> listOfChefs = chefs.getContent();
        List<ChefResponseDto> content = listOfChefs.stream()
                .map(chef -> {
                    ChefResponseDto dto = modelMapper.map(chef, ChefResponseDto.class);
                    // Tính khoảng cách từ chef đến customer
                    double chefLat = chef.getLatitude();
                    double chefLng = chef.getLongitude();
                    double distanceToCustomer = calculateService.calculateDistance(customerLat, customerLng, chefLat, chefLng);
                    dto.setDistance(distanceToCustomer);
                    dto.setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));

                    return dto;
                })
                .collect(Collectors.toList());

        if (sortBy.equals("rating")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder())));
        }else if (sortBy.equals("distance")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(ChefResponseDto::getDistance, Comparator.nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(ChefResponseDto::getDistance, Comparator.nullsLast(Comparator.reverseOrder())));
        }
        else if (AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            content.sort(Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        ChefsResponse templatesResponse = new ChefsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(chefs.getNumber());
        templatesResponse.setPageSize(chefs.getSize());
        templatesResponse.setTotalElements(chefs.getTotalElements());
        templatesResponse.setTotalPages(chefs.getTotalPages());
        templatesResponse.setLast(chefs.isLast());
        return templatesResponse;
    }

    @Override
    public ChefsResponse getAllChefsNearBySearch(String keySearch, double customerLat, double customerLng, double distance, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort;
        // Xử lý trường hợp sắp xếp theo đánh giá
        if (sortBy.equals("rating") || sortBy.equals("distance")|| AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            sort = Sort.by("id").ascending();
        } else {
            sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
        }
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Chef> chefs = chefRepository.searchChefByKeywordStatusAndDistance(keySearch,"ACTIVE",customerLat, customerLng, distance, pageable);
        List<Chef> listOfChefs = chefs.getContent();
        List<ChefResponseDto> content = listOfChefs.stream()
                .map(chef -> {
                    ChefResponseDto dto = modelMapper.map(chef, ChefResponseDto.class);
                    double chefLat = chef.getLatitude();
                    double chefLng = chef.getLongitude();
                    double distanceToCustomer = calculateService.calculateDistance(customerLat, customerLng, chefLat, chefLng);
                    dto.setDistance(distanceToCustomer);
                    dto.setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));

                    return dto;
                })
                .collect(Collectors.toList());
        if (sortBy.equals("rating")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder())));
        }else if (sortBy.equals("distance")) {
            content.sort(sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Comparator.comparing(ChefResponseDto::getDistance, Comparator.nullsLast(Comparator.naturalOrder())) :
                    Comparator.comparing(ChefResponseDto::getDistance, Comparator.nullsLast(Comparator.reverseOrder())));
        }
        else if (AppConstants.DEFAULT_SORT_RATING_DESC.equals(sortDir)) {
            content.sort(Comparator.comparing(ChefResponseDto::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder())));
        }
        // Tạo đối tượng response
        ChefsResponse templatesResponse = new ChefsResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(chefs.getNumber());
        templatesResponse.setPageSize(chefs.getSize());
        templatesResponse.setTotalElements(chefs.getTotalElements());
        templatesResponse.setTotalPages(chefs.getTotalPages());
        templatesResponse.setLast(chefs.isLast());
        return templatesResponse;
    }

    @Override
    public ChefResponseDto updateChef(Long chefId, ChefRequestDto requestDto) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        if (requestDto.getBio() != null) chef.setBio(requestDto.getBio());
        if (requestDto.getDescription() != null) chef.setDescription(requestDto.getDescription());
        if (requestDto.getAddress() != null) {
            chef.setAddress(requestDto.getAddress());
            double[] latLng = distanceService.getLatLngFromAddress(requestDto.getAddress());
            chef.setLatitude(latLng[0]);
            chef.setLongitude(latLng[1]);
        }
        if (requestDto.getPrice() != null) chef.setPrice(requestDto.getPrice());
        if (requestDto.getMaxServingSize() != null) chef.setMaxServingSize(requestDto.getMaxServingSize());
        if (requestDto.getSpecialization() != null) chef.setSpecialization(requestDto.getSpecialization());
        if (requestDto.getYearsOfExperience() != null) chef.setYearsOfExperience(requestDto.getYearsOfExperience());
        if (requestDto.getCertification() != null) chef.setCertification(requestDto.getCertification());

        chefRepository.save(chef);
        chefRepository.save(chef);
        return modelMapper.map(chef, ChefResponseDto.class);
    }

    @Override
    public ChefResponseDto updateChefBySelf(Long userId, ChefRequestDto requestDto) {
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with user: " + userId));
        if (chef.getStatus().equalsIgnoreCase("REJECTED")){
            chef.setStatus("PENDING");
        }
        if (requestDto.getBio() != null) chef.setBio(requestDto.getBio());
        if (requestDto.getDescription() != null) chef.setDescription(requestDto.getDescription());
        if (requestDto.getAddress() != null) {
            chef.setAddress(requestDto.getAddress());
            double[] latLng = distanceService.getLatLngFromAddress(requestDto.getAddress());
            chef.setLatitude(latLng[0]);
            chef.setLongitude(latLng[1]);
        }
        if (requestDto.getPrice() != null) chef.setPrice(requestDto.getPrice());
        if (requestDto.getMaxServingSize() != null) chef.setMaxServingSize(requestDto.getMaxServingSize());
        if (requestDto.getSpecialization() != null) chef.setSpecialization(requestDto.getSpecialization());
        if (requestDto.getYearsOfExperience() != null) chef.setYearsOfExperience(requestDto.getYearsOfExperience());
        if (requestDto.getCertification() != null) chef.setCertification(requestDto.getCertification());

        chefRepository.save(chef);
        chefRepository.save(chef);
        return modelMapper.map(chef, ChefResponseDto.class);
    }

    @Override
    public void deleteChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        chef.setStatus("BANNED");
        chef.setIsDeleted(true);
        chefRepository.save(chef);
    }

    @Override
    @Transactional
    public String unlockChefByPayment(Long userId) {
        Chef chef = chefRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found."));

        if (!"LOCKED".equalsIgnoreCase(chef.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef status not block.");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Wallet not found."));
        BigDecimal unlockFee = new BigDecimal("25.00");
        if (chef.getPenaltyFee() != null && chef.getPenaltyFee().compareTo(BigDecimal.ZERO) > 0) {
            unlockFee = chef.getPenaltyFee();
            chef.setPenaltyFee(BigDecimal.ZERO);
            chefRepository.save(chef);
        }
        if (wallet.getBalance().compareTo(unlockFee) < 0) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance to unlock.");
        }
        wallet.setBalance(wallet.getBalance().subtract(unlockFee));
        walletRepository.save(wallet);
        chef.setReputationPoints(75);
        chef.setStatus("ACTIVE");
        chefRepository.save(chef);
        ChefTransaction transaction = ChefTransaction.builder()
                .wallet(wallet)
                .amount(unlockFee)
                .transactionType("PAYMENT")
                .description("Chef account unlock payment.")
                .status("COMPLETED")
                .build();
        chefTransactionRepository.save(transaction);
        return "Unlock payment successful. Chef account is now active.";
    }
}
