package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.MenuRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuUpdateDto;
import com.spring2025.vietchefs.models.payload.responseModel.ApiResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuPagingResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MenuService {
    MenuResponseDto createMenu(MenuRequestDto menuRequestDto, MultipartFile imageFile);
    MenuPagingResponse getMenusByChef(Long chefId, int pageNo, int pageSize, String sortBy, String sortDir);
    MenuResponseDto getMenuById(Long id);
    MenuPagingResponse getAllMenus(int pageNo, int pageSize, String sortBy, String sortDir);
    MenuResponseDto updateMenu(Long menuId, MenuUpdateDto menuUpdateDto,MultipartFile imageFile);
    String deleteMenu(Long menuId);
    ApiResponse<Void> validateMenuStillValid(Long menuId, List<Long> allowedItemNames);
    List<MenuResponseDto> searchMenuByNameNearBy(double customerLat, double customerLng, double distance, String keyword, String sortBy, String sortDir);
    List<MenuResponseDto> getMenusNearBy(double customerLat, double customerLng, double distance, String sortBy, String sortDir);

}
