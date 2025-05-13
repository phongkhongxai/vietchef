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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {
    @Autowired
    private MenuService menuService;


    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuResponseDto> createMenu(@ModelAttribute MenuRequestDto requestDto,@RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(menuService.createMenu(requestDto,file));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF') or hasRole('ROLE_ADMIN')")
    @PutMapping(value ="/{menuId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuResponseDto> updateMenu(@PathVariable Long menuId, @ModelAttribute MenuUpdateDto dto,@RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(menuService.updateMenu(menuId, dto,file));
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
    @GetMapping("/nearby")
    public ResponseEntity<List<MenuResponseDto>> getMenuNearby(
            @Parameter(description = "Vĩ độ của khách hàng") @RequestParam Double customerLat,
            @Parameter(description = "Kinh độ của khách hàng") @RequestParam Double customerLng,
            @Parameter(description = "Bán kính tìm kiếm (đơn vị km)") @RequestParam Double distance,
            @Parameter(description = "Tiêu chí sắp xếp (name, beforePrice, afterPrice, distance, rating)") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Thứ tự sắp xếp: asc (tăng dần), desc (giảm dần)") @RequestParam(defaultValue = "asc") String sortDir){
        List<MenuResponseDto> result = menuService.getMenusNearBy(customerLat, customerLng, distance, sortBy, sortDir);
        return ResponseEntity.ok(result);
    }
    @Operation(
            summary = "Tìm kiếm menu gần vị trí khách hàng theo từ khóa",
            description = "Trả về danh sách menu nằm trong bán kính chỉ định, tên chứa từ khóa. Hỗ trợ sắp xếp theo tên, giá, khoảng cách, đánh giá."
    )
    @GetMapping("/nearby/search")
    public ResponseEntity<List<MenuResponseDto>> searchNearbyMenus(
            @Parameter(description = "Từ khóa tìm kiếm trong tên menu") @RequestParam String keyword,
            @Parameter(description = "Vĩ độ của khách hàng") @RequestParam Double customerLat,
            @Parameter(description = "Kinh độ của khách hàng") @RequestParam Double customerLng,
            @Parameter(description = "Bán kính tìm kiếm (đơn vị km)") @RequestParam Double distance,
            @Parameter(description = "Tiêu chí sắp xếp (name, beforePrice, afterPrice, distance, rating)") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Thứ tự sắp xếp: asc (tăng dần), desc (giảm dần)") @RequestParam(defaultValue = "asc") String sortDir) {

        List<MenuResponseDto> result = menuService.searchMenuByNameNearBy(customerLat, customerLng, distance, keyword, sortBy, sortDir);
        return ResponseEntity.ok(result);
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
