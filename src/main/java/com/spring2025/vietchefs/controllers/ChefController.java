package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.DishDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChefRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefsResponse;
import com.spring2025.vietchefs.models.payload.responseModel.DishesResponse;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chefs")
public class ChefController {
    @Autowired
    private ChefService chefService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @PostMapping("/register/{userId}")
    public ResponseEntity<ChefResponseDto> registerChef(@PathVariable Long userId, @RequestBody ChefRequestDto requestDto) {
        return ResponseEntity.ok(chefService.registerChefRequest(userId, requestDto));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/approve/{chefId}")
    public ResponseEntity<ChefResponseDto> approveChef(@PathVariable Long chefId) {
        return ResponseEntity.ok(chefService.approveChef(chefId));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PutMapping("/{chefId}")
    public ResponseEntity<ChefResponseDto> updateChef(@PathVariable Long chefId, @RequestBody ChefRequestDto chefRequestDto) {
        return ResponseEntity.ok(chefService.updateChef(chefId,chefRequestDto));
    }
    @GetMapping("/{chefId}")
    public ResponseEntity<?> getChefById(@PathVariable Long chefId){
        ChefResponseDto dto = chefService.getChefById(chefId);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
    @GetMapping
    public ChefsResponse getAllChefs(
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        return chefService.getAllChefs(pageNo, pageSize, sortBy, sortDir);
    }
    @GetMapping("/nearby")
    public ChefsResponse getAllChefsNearBy(
            @RequestParam(value = "customerLat") Double customerLat,
            @RequestParam(value = "customerLng") Double customerLng,
            @RequestParam(value = "distance") Double distance,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir){
        return chefService.getAllChefsNearBy(customerLat,customerLng,distance,pageNo, pageSize, sortBy, sortDir);
    }
//    @PostMapping("/reputation/recharge")
//    public ResponseEntity<?> rechargeReputation(@RequestBody RechargeDto dto) {
//
//        // Kiểm tra thanh toán thành công
//        if (paymentService.verify(dto.getPaymentToken())) {
//            chef.setReputationPoints(70);
//            chef.setStatus("ACTIVE");
//            chefRepository.save(chef);
//            return ResponseEntity.ok("Phục hồi thành công!");
//        }
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thanh toán thất bại!");
//    }

}
