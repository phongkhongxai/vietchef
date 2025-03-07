package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.requestModel.CreateAddressRequest;
import com.spring2025.vietchefs.models.payload.requestModel.UpdateAddressRequest;
import com.spring2025.vietchefs.models.payload.responseModel.AddressResponse;
import com.spring2025.vietchefs.services.AddressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * Lấy thông tin Address theo id.
     * Endpoint: GET /api/v1/address/{id}
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable Long id) {
        AddressResponse response = addressService.getAddressById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách Address của người dùng hiện tại.
     * Endpoint: GET /api/v1/address/my-addresses
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @GetMapping("/my-addresses")
    public ResponseEntity<List<AddressResponse>> getAddressesFromUser() {
        List<AddressResponse> responses = addressService.getAddressesFromUser();
        return ResponseEntity.ok(responses);
    }

    /**
     * Tạo mới Address cho người dùng (giới hạn tối đa 5 địa chỉ).
     * Endpoint: POST /api/v1/address
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse response = addressService.createAddress(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Cập nhật thông tin Address.
     * Endpoint: PUT /api/v1/address
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping
    public ResponseEntity<AddressResponse> updateAddress(
            @Valid @RequestBody UpdateAddressRequest request) {
        AddressResponse response = addressService.updateAddress(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa mềm Address theo id.
     * Endpoint: DELETE /api/v1/address/{id}
     */
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok("Address deleted successfully");
    }
}

