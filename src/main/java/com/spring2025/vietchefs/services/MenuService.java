package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.MenuRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuUpdateDto;
import com.spring2025.vietchefs.models.payload.responseModel.ApiResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuPagingResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuResponseDto;

import java.util.List;

public interface MenuService {
    MenuResponseDto createMenu(MenuRequestDto menuRequestDto);
    MenuPagingResponse getMenusByChef(Long chefId, int pageNo, int pageSize, String sortBy, String sortDir);
    MenuResponseDto getMenuById(Long id);
    MenuPagingResponse getAllMenus(int pageNo, int pageSize, String sortBy, String sortDir);
    MenuResponseDto updateMenu(Long menuId, MenuUpdateDto menuUpdateDto);
    String deleteMenu(Long menuId);
    ApiResponse<Void> validateMenuStillValid(Long menuId, List<Long> allowedItemNames);
}
