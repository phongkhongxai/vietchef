package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChefRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.RoleRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private DistanceService distanceService;
    @Autowired
    private CalculateService calculateService;

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
        return modelMapper.map(chef.get(), ChefResponseDto.class);
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

        // Đặt trạng thái thành ACTIVE
        chef.setStatus("ACTIVE");

        // Gán quyền ROLE_CHEF cho User
        Role chefRole = roleRepository.findByRoleName("ROLE_CHEF")
                                  .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Role ROLE_CHEF not found"));
        User user = chef.getUser();
        user.setRole(chefRole);
        userRepository.save(user);

        walletService.updateWalletType(user.getId(), "CHEF");

        chefRepository.save(chef);
        return modelMapper.map(chef, ChefResponseDto.class);
    }

    @Override
    public ChefsResponse getAllChefs(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Chef> chefs = chefRepository.findByStatusAndIsDeletedFalse("ACTIVE",pageable);

        // get content for page object
        List<Chef> listOfChefs = chefs.getContent();

        List<ChefResponseDto> content = listOfChefs.stream().map(bt -> modelMapper.map(bt, ChefResponseDto.class)).collect(Collectors.toList());

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
    public ChefsResponse getAllChefsNearBy( double customerLat, double customerLng, double distance,int pageNo, int pageSize, String sortBy, String sortDir) {
        // Tạo đối tượng Sort
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Chef> chefs = chefRepository.findByStatusAndIsDeletedFalse("ACTIVE", pageable);

        // Lấy danh sách các đầu bếp từ kết quả
        List<Chef> listOfChefs = chefs.getContent();

        // Lọc các đầu bếp có khoảng cách gần khách hàng trong bán kính mong muốn
        List<Chef> filteredChefs = listOfChefs.stream()
                .filter(chef -> {
                    double chefLat = chef.getLatitude(); // Giả sử có latitude và longitude trong chef entity
                    double chefLng = chef.getLongitude();
                    double distanceToCustomer = calculateService.calculateDistance(customerLat, customerLng, chefLat, chefLng);
                    return distanceToCustomer <= distance; // Lọc các đầu bếp trong bán kính mong muốn
                })
                .toList();

        // Chuyển đổi thành DTO để trả về
        List<ChefResponseDto> content = filteredChefs.stream()
                .map(chef -> modelMapper.map(chef, ChefResponseDto.class))
                .collect(Collectors.toList());

        // Tạo đối tượng response
        ChefsResponse chefsResponse = new ChefsResponse();
        chefsResponse.setContent(content);
        chefsResponse.setPageNo(chefs.getNumber());
        chefsResponse.setPageSize(chefs.getSize());
        chefsResponse.setTotalElements(filteredChefs.size());
        chefsResponse.setTotalPages((int) Math.ceil((double) filteredChefs.size() / pageSize));
        chefsResponse.setLast(filteredChefs.size() <= pageNo * pageSize);

        return chefsResponse;
    }

    @Override
    public ChefResponseDto updateChef(Long chefId, ChefRequestDto requestDto) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        chef.setBio(requestDto.getBio() != null ? requestDto.getBio() : chef.getBio());
        chef.setDescription(requestDto.getDescription() != null ? requestDto.getDescription() : chef.getDescription());
        chef.setAddress(requestDto.getAddress() != null ? requestDto.getAddress() : chef.getAddress());
        chef.setPrice(requestDto.getPrice() != null ? requestDto.getPrice() : chef.getPrice());
        chef.setMaxServingSize(requestDto.getMaxServingSize() != null ? requestDto.getMaxServingSize() : chef.getMaxServingSize());

        chefRepository.save(chef);
        return modelMapper.map(chef, ChefResponseDto.class);
    }

    @Override
    public void deleteChef(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        chef.setStatus("BLOCKED");
        chef.setIsDeleted(true);
        chefRepository.save(chef);
    }
}
