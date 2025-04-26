package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.FavoriteChef;
import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.FavoriteChefDto;
import com.spring2025.vietchefs.models.payload.responseModel.FavoriteChefsResponse;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.FavoriteChefRepository;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.services.FavoriteChefService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteChefServiceImpl implements FavoriteChefService {

    private final FavoriteChefRepository favoriteChefRepository;
    private final UserRepository userRepository;
    private final ChefRepository chefRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public FavoriteChefServiceImpl(
            FavoriteChefRepository favoriteChefRepository,
            UserRepository userRepository,
            ChefRepository chefRepository,
            ModelMapper modelMapper) {
        this.favoriteChefRepository = favoriteChefRepository;
        this.userRepository = userRepository;
        this.chefRepository = chefRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public FavoriteChefDto addFavoriteChef(Long userId, Long chefId) {
        User user = userRepository.findExistUserById(userId);
        if (user == null) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        // Kiểm tra xem chef có trong trạng thái active không
        if (!"ACTIVE".equals(chef.getStatus())) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef is not active");
        }

        // Kiểm tra xem đã tồn tại trong danh sách yêu thích chưa
        if (favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(user, chef)) {
            throw new VchefApiException(HttpStatus.BAD_REQUEST, "Chef already in favorites");
        }

        // Thêm mới vào danh sách yêu thích
        FavoriteChef favoriteChef = new FavoriteChef();
        favoriteChef.setUser(user);
        favoriteChef.setChef(chef);
        favoriteChef.setIsDeleted(false);

        favoriteChef = favoriteChefRepository.save(favoriteChef);

        return mapToFavoriteChefDto(favoriteChef);
    }

    @Override
    public void removeFavoriteChef(Long userId, Long chefId) {
        User user = userRepository.findExistUserById(userId);
        if (user == null) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        // Tìm và cập nhật trạng thái isDeleted
        FavoriteChef favoriteChef = favoriteChefRepository.findByUserAndChefAndIsDeletedFalse(user, chef)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found in favorites"));

        favoriteChef.setIsDeleted(true);
        favoriteChefRepository.save(favoriteChef);
    }

    @Override
    public FavoriteChefsResponse getFavoriteChefs(Long userId, int pageNo, int pageSize, String sortBy, String sortDir) {
        User user = userRepository.findExistUserById(userId);
        if (user == null) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        
        Page<FavoriteChef> favorites = favoriteChefRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        List<FavoriteChef> listOfFavorites = favorites.getContent();
        
        List<FavoriteChefDto> content = listOfFavorites.stream()
                .map(this::mapToFavoriteChefDto)
                .collect(Collectors.toList());

        FavoriteChefsResponse favoriteChefsResponse = new FavoriteChefsResponse();
        favoriteChefsResponse.setContent(content);
        favoriteChefsResponse.setPageNo(favorites.getNumber());
        favoriteChefsResponse.setPageSize(favorites.getSize());
        favoriteChefsResponse.setTotalElements(favorites.getTotalElements());
        favoriteChefsResponse.setTotalPages(favorites.getTotalPages());
        favoriteChefsResponse.setLast(favorites.isLast());

        return favoriteChefsResponse;
    }

    @Override
    public boolean isChefFavorite(Long userId, Long chefId) {
        User user = userRepository.findExistUserById(userId);
        if (user == null) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        return favoriteChefRepository.existsByUserAndChefAndIsDeletedFalse(user, chef);
    }

    // Helper method để chuyển đổi Entity sang DTO
    private FavoriteChefDto mapToFavoriteChefDto(FavoriteChef favoriteChef) {
        FavoriteChefDto dto = new FavoriteChefDto();
        dto.setId(favoriteChef.getId());
        dto.setUserId(favoriteChef.getUser().getId());
        dto.setChefId(favoriteChef.getChef().getId());
        dto.setChefName(favoriteChef.getChef().getUser().getFullName());
        dto.setChefAvatar(favoriteChef.getChef().getUser().getAvatarUrl());
        dto.setChefSpecialization(favoriteChef.getChef().getSpecialization());
        dto.setChefAddress(favoriteChef.getChef().getAddress());
        dto.setCreatedAt(favoriteChef.getCreatedAt());
        return dto;
    }
} 