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
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuPagingResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuResponseDto;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.DishRepository;
import com.spring2025.vietchefs.repositories.MenuItemRepository;
import com.spring2025.vietchefs.repositories.MenuRepository;
import com.spring2025.vietchefs.services.MenuService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private ModelMapper modelMapper;
    @Override
    public MenuResponseDto createMenu(MenuRequestDto menuRequestDto) {
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

        Page<Menu> menu = menuRepository.findByChef(chef,pageable);

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

        Page<Menu> menu = menuRepository.findAll(pageable);

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
    public MenuResponseDto updateMenu(Long menuId, MenuUpdateDto menuUpdateDto) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Menu not found with id: " + menuId));
        List<Dish> dishes = menuUpdateDto.getMenuItems().stream()
                .map(itemDto -> dishRepository.findById(itemDto.getDishId())
                        .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Dish not found")))
                .toList();

        menu.setName(menuUpdateDto.getName() != null ? menuUpdateDto.getName() : menu.getName());
        menu.setDescription(menuUpdateDto.getDescription() != null ? menuUpdateDto.getDescription() : menu.getDescription());
        menu.setHasDiscount(menuUpdateDto.getHasDiscount() != null ? menuUpdateDto.getHasDiscount() : menu.getHasDiscount());
        menu.setDiscountPercentage(menuUpdateDto.getDiscountPercentage() != null ? menuUpdateDto.getDiscountPercentage() : menu.getDiscountPercentage());

        if (menuUpdateDto.getTotalCookTime() == null) {
            List<Long> dishIds = dishes.stream()
                    .map(Dish::getId)
                    .collect(Collectors.toList());

            BigDecimal calculatedTotalCookTime = calculateService.calculateTotalCookTime(dishIds, 4);
            menu.setTotalCookTime(calculatedTotalCookTime);
        } else {
            menu.setTotalCookTime(menuUpdateDto.getTotalCookTime());
        }
        if (menuUpdateDto.getMenuItems() != null && !menuUpdateDto.getMenuItems().isEmpty()) {
            List<MenuItem> menuItemList = menuItemRepository.findByMenu(menu);
            menuItemRepository.deleteAll(menuItemList);

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
                    .collect(Collectors.toList());

            menuItemRepository.saveAll(newMenuItems);
        }

        // Lưu menu đã cập nhật và trả về DTO
        Menu updatedMenu = menuRepository.save(menu);
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



}
