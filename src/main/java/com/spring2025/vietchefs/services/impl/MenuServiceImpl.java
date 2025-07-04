package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Dish;
import com.spring2025.vietchefs.models.entity.Menu;
import com.spring2025.vietchefs.models.entity.MenuItem;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuItemRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuUpdateDto;
import com.spring2025.vietchefs.models.payload.responseModel.*;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.DishRepository;
import com.spring2025.vietchefs.repositories.MenuItemRepository;
import com.spring2025.vietchefs.repositories.MenuRepository;
import com.spring2025.vietchefs.services.MenuService;
import com.spring2025.vietchefs.services.ReviewService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private MenuItemRepository menuItemRepository;
    @Autowired
    private ChefRepository chefRepository;
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private CalculateService calculateService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public MenuResponseDto createMenu(MenuRequestDto menuRequestDto, MultipartFile imageFile) {
        Chef chef = chefRepository.findById(menuRequestDto.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found"));

        List<Dish> dishes = menuRequestDto.getMenuItems().stream()
                .map(itemDto -> dishRepository.findById(itemDto.getDishId())
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found")))
                .toList();

        Menu menu = new Menu();
        menu.setChef(chef);
        menu.setName(menuRequestDto.getName());
        menu.setDescription(menuRequestDto.getDescription());
        menu.setHasDiscount(menuRequestDto.getHasDiscount());
        menu.setDiscountPercentage(menuRequestDto.getDiscountPercentage());
        menu.setImageUrl("defaultMenu");

        if (menuRequestDto.getTotalCookTime() == null) {
            // Chuyển danh sách Dish thành danh sách ID món ăn (Long)
            List<Long> dishIds = dishes.stream()
                    .map(Dish::getId)
                    .collect(Collectors.toList());

            // Tính tổng thời gian nấu cho các món
            BigDecimal calculatedTotalCookTime = calculateService.calculateTotalCookTime(dishIds, 4);
            menu.setTotalCookTime(calculatedTotalCookTime);
        } else {
            menu.setTotalCookTime(menuRequestDto.getTotalCookTime());
        }

        // Tạo danh sách MenuItem
        List<MenuItem> menuItems = new ArrayList<>();
        for (int i = 0; i < menuRequestDto.getMenuItems().size(); i++) {
            MenuItem newItem = new MenuItem();
            newItem.setMenu(menu);
            newItem.setDish(dishes.get(i));
            menuItems.add(newItem);
        }
        menu.setMenuItems(menuItems);

        menu = menuRepository.save(menu);
        if (imageFile != null) {
            try {
                String imageUrl = imageService.uploadImage(imageFile,menu.getId(), "MENU");
                menu.setImageUrl(imageUrl);
            } catch (IOException e) {
                throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image.");
            }
        }

        return modelMapper.map(menu, MenuResponseDto.class);
    }


    @Override
    public MenuPagingResponse getMenusByChef(Long chefId,int pageNo, int pageSize, String sortBy, String sortDir) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND,"Chef not found with id: "+ chefId));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Menu> menu = menuRepository.findByChefAndIsDeletedFalse(chef,pageable);

        // get content for page object
        List<Menu> listOfMenu = menu.getContent();

        List<MenuResponseDto> content = listOfMenu.stream().map(meu -> {
            MenuResponseDto dto = modelMapper.map(meu, MenuResponseDto.class);
            dto.setBeforePrice(calculateMenuPrice(meu, false));
            dto.setAfterPrice(calculateMenuPrice(meu, true));
            return dto;
        }).collect(Collectors.toList());
        MenuPagingResponse templatesResponse = new MenuPagingResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(menu.getNumber());
        templatesResponse.setPageSize(menu.getSize());
        templatesResponse.setTotalElements(menu.getTotalElements());
        templatesResponse.setTotalPages(menu.getTotalPages());
        templatesResponse.setLast(menu.isLast());
        return templatesResponse;
    }
    private BigDecimal calculateMenuPrice(Menu menu, boolean applyDiscount) {
        BigDecimal totalPrice = menu.getMenuItems().stream()
                .map(item -> item.getDish().getBasePrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (applyDiscount && menu.getHasDiscount() && menu.getDiscountPercentage() != null) {
            BigDecimal discount = BigDecimal.valueOf(menu.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
            totalPrice = totalPrice.multiply(BigDecimal.ONE.subtract(discount));
        }
        return totalPrice;
    }

    @Override
    public MenuResponseDto getMenuById(Long id) {
        Optional<Menu> menu = menuRepository.findById(id);
        if (menu.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found with id: "+ id);
        }
        MenuResponseDto dto = modelMapper.map(menu.get(), MenuResponseDto.class);
        dto.setBeforePrice(calculateMenuPrice(menu.get(), false));
        dto.setAfterPrice(calculateMenuPrice(menu.get(), true));
        
        return dto;
    }

    @Override
    public MenuPagingResponse getAllMenus(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // create Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Menu> menu = menuRepository.findAllNotDeleted(pageable);

        // get content for page object
        List<Menu> listOfMenu = menu.getContent();

        List<MenuResponseDto> content = listOfMenu.stream().map(meu -> {
            MenuResponseDto dto = modelMapper.map(meu, MenuResponseDto.class);
            dto.setBeforePrice(calculateMenuPrice(meu, false));
            dto.setAfterPrice(calculateMenuPrice(meu, true));

            return dto;
        }).collect(Collectors.toList());
        MenuPagingResponse templatesResponse = new MenuPagingResponse();
        templatesResponse.setContent(content);
        templatesResponse.setPageNo(menu.getNumber());
        templatesResponse.setPageSize(menu.getSize());
        templatesResponse.setTotalElements(menu.getTotalElements());
        templatesResponse.setTotalPages(menu.getTotalPages());
        templatesResponse.setLast(menu.isLast());
        return templatesResponse;
    }

    @Override
    public MenuResponseDto updateMenu(Long menuId, MenuUpdateDto menuUpdateDto, MultipartFile imageFile) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found with id: " + menuId));
        menu.setName(menuUpdateDto.getName() != null ? menuUpdateDto.getName() : menu.getName());
        menu.setDescription(menuUpdateDto.getDescription() != null ? menuUpdateDto.getDescription() : menu.getDescription());
        menu.setHasDiscount(menuUpdateDto.getHasDiscount() != null ? menuUpdateDto.getHasDiscount() : menu.getHasDiscount());
        menu.setDiscountPercentage(menuUpdateDto.getDiscountPercentage() != null ? menuUpdateDto.getDiscountPercentage() : menu.getDiscountPercentage());

        // Nếu menuItems khác null => xử lý cập nhật
        if (menuUpdateDto.getMenuItems() != null && !menuUpdateDto.getMenuItems().isEmpty()) {
            // Lấy danh sách Dish tương ứng
            List<Dish> dishes = menuUpdateDto.getMenuItems().stream()
                    .map(itemDto -> dishRepository.findById(itemDto.getDishId())
                            .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found")))
                    .toList();

            // Xoá các menu item cũ
            List<MenuItem> menuItemList = menuItemRepository.findByMenu(menu);
            menuItemRepository.deleteAll(menuItemList);

            // Tạo các menu item mới
            List<MenuItem> newMenuItems = menuUpdateDto.getMenuItems().stream()
                    .map(itemDto -> {
                        Dish dish = dishes.stream()
                                .filter(d -> d.getId().equals(itemDto.getDishId()))
                                .findFirst()
                                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found for menu item"));
                        MenuItem newItem = new MenuItem();
                        newItem.setMenu(menu);
                        newItem.setDish(dish);
                        return newItem;
                    })
                    .toList();

            menuItemRepository.saveAll(newMenuItems);

            // Cập nhật thời gian nấu nếu cần
            if (menuUpdateDto.getTotalCookTime() == null) {
                List<Long> dishIds = dishes.stream().map(Dish::getId).toList();
                BigDecimal calculatedTotalCookTime = calculateService.calculateTotalCookTimeFromMenu(menu.getId(), dishIds, 4);
                menu.setTotalCookTime(calculatedTotalCookTime);
            } else {
                menu.setTotalCookTime(menuUpdateDto.getTotalCookTime());
            }
        } else {
            // Nếu không truyền menuItems, chỉ cập nhật totalCookTime nếu có
            if (menuUpdateDto.getTotalCookTime() != null) {
                menu.setTotalCookTime(menuUpdateDto.getTotalCookTime());
            }
            // Nếu totalCookTime == null thì giữ nguyên totalCookTime hiện tại
        }


        // Lưu menu đã cập nhật và trả về DTO
        Menu updatedMenu = menuRepository.save(menu);
        if (imageFile != null) {
            try{
                String imageUrl = imageService.uploadImage(imageFile, menuId, "MENU");
                updatedMenu.setImageUrl(imageUrl);
            } catch (IOException e) {
                throw new VchefApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image.");
            }
        }
        return modelMapper.map(updatedMenu, MenuResponseDto.class);
    }

    @Override
    public String deleteMenu(Long menuId) {
        Optional<Menu> menuOptional = menuRepository.findById(menuId);
        if (menuOptional.isEmpty()){
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found with id: "+ menuId);
        }
        Menu menu = menuOptional.get();
        menu.setIsDeleted(true);
        menuRepository.save(menu);
        return "Deleted menu successfully";
    }

    @Override
    public ApiResponse<Void> validateMenuStillValid(Long menuId, List<Long> allowedItemNames) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found with ID: " + menuId));

        if (menu.getIsDeleted()) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Menu đã bị xóa.")
                    .build();
        }

        List<Long> currentDishIds = menu.getMenuItems().stream()
                .map(menuItem -> menuItem.getDish().getId())
                .toList();

        boolean allContained = currentDishIds.stream().allMatch(allowedItemNames::contains);

        if (!allContained) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Menu đã bị thay đổi món ăn so với ban đầu.")
                    .build();
        }

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Menu hợp lệ.")
                .build();
    }

    @Override
    public List<MenuResponseDto> searchMenuByNameNearBy(double customerLat, double customerLng, double distance, String keyword, String sortBy, String sortDir) {
        List<Menu> listOfMenu = menuRepository.searchMenusByKeywordAndDistance(keyword, customerLat,customerLng,distance);
        List<MenuResponseDto>  menuResponseDtos = listOfMenu.stream().map(meu -> {
            MenuResponseDto dto = modelMapper.map(meu, MenuResponseDto.class);
            dto.setBeforePrice(calculateMenuPrice(meu, false));
            dto.setAfterPrice(calculateMenuPrice(meu, true));
            Chef chef = meu.getChef();
            if (chef != null && chef.getLatitude() != null && chef.getLongitude() != null) {
                double dist = calculateService.calculateDistance(customerLat, customerLng, chef.getLatitude(), chef.getLongitude());
                if (dto.getChef() != null) {
                    dto.getChef().setDistance(dist);
                    dto.getChef().setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));
                }
            }
            return dto;
        }).toList();
        return sortMenuList(menuResponseDtos, sortBy, sortDir);
    }

    @Override
    public List<MenuResponseDto> getMenusNearBy(double customerLat, double customerLng, double distance, String sortBy, String sortDir) {
        List<Menu> listOfMenu = menuRepository.findMenusNearCustomer(customerLat,customerLng,distance);
        List<MenuResponseDto>  menuResponseDtos = listOfMenu.stream().map(meu -> {
            MenuResponseDto dto = modelMapper.map(meu, MenuResponseDto.class);
            dto.setBeforePrice(calculateMenuPrice(meu, false));
            dto.setAfterPrice(calculateMenuPrice(meu, true));
            Chef chef = meu.getChef();
            if (chef != null && chef.getLatitude() != null && chef.getLongitude() != null) {
                double dist = calculateService.calculateDistance(customerLat, customerLng, chef.getLatitude(), chef.getLongitude());
                if (dto.getChef() != null) {
                    dto.getChef().setDistance(dist);
                    dto.getChef().setAverageRating(reviewService.getAverageRatingForChef(chef.getId()));
                }
            }
            return dto;
        }).toList();
        return sortMenuList(menuResponseDtos, sortBy, sortDir);
    }
    private List<MenuResponseDto> sortMenuList(List<MenuResponseDto> menus, String sortBy, String sortDir) {
        Comparator<MenuResponseDto> comparator = switch (sortBy) {
            case "afterPrice" ->
                    Comparator.comparing(MenuResponseDto::getAfterPrice, Comparator.nullsLast(BigDecimal::compareTo));
            case "beforePrice" ->
                    Comparator.comparing(MenuResponseDto::getBeforePrice, Comparator.nullsLast(BigDecimal::compareTo));
            case "name" ->
                    Comparator.comparing(MenuResponseDto::getName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "distance" ->
                    Comparator.comparing(dto -> dto.getChef() != null ? dto.getChef().getDistance() : Double.MAX_VALUE);
            case "rating" ->
                    Comparator.comparing(dto -> dto.getChef() != null ? dto.getChef().getAverageRating() : BigDecimal.ZERO);
            default -> Comparator.comparing(MenuResponseDto::getName);
        };

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return menus.stream().sorted(comparator).toList();
    }



}
