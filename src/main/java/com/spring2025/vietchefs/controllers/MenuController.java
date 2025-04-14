package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.MenuUpdateDto;
import com.spring2025.vietchefs.models.payload.responseModel.ApiResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuPagingResponse;
import com.spring2025.vietchefs.models.payload.responseModel.MenuResponseDto;
import com.spring2025.vietchefs.services.MenuService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {
    @Autowired
    private MenuService menuService;


    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<MenuResponseDto> createMenu(@RequestBody MenuRequestDto requestDto) {
        return ResponseEntity.ok(menuService.createMenu(requestDto));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @PutMapping("/{menuId}")
    public ResponseEntity<MenuResponseDto> updateMenu(@PathVariable Long menuId, @RequestBody MenuUpdateDto dto) {
        return ResponseEntity.ok(menuService.updateMenu(menuId, dto));
    }

    @GetMapping
    public MenuPagingResponse getMenuByChef(
            @RequestParam(value = "chefId", required = false) Long chefId,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        if (chefId != null) {
            return menuService.getMenusByChef(chefId, pageNo, pageSize, sortBy, sortDir);
        }
        return menuService.getAllMenus(pageNo, pageSize, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMenuById(@PathVariable("id") Long id) {
        MenuResponseDto menuResponseDto = menuService.getMenuById(id);
        return new ResponseEntity<>(menuResponseDto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{menuId}")
    public ResponseEntity<?> deleteMenu(@PathVariable Long menuId) {
        String msg = menuService.deleteMenu(menuId);
        return new ResponseEntity<>(msg, HttpStatus.NO_CONTENT);

    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{menuId}/validate")
    public ResponseEntity<ApiResponse<Void>> validateMenu(
            @PathVariable Long menuId,
            @RequestBody List<Long> allowedDishIds) {

        ApiResponse<Void> response = menuService.validateMenuStillValid(menuId, allowedDishIds);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }


}
