package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.WalletRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChefRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.ChefResponseDto;
import com.spring2025.vietchefs.services.WalletRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet-requests")
public class WalletRequestController {
    @Autowired
    private WalletRequestService walletRequestService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_CHEF')")
    @PostMapping
    public ResponseEntity<WalletRequestDto> createRequest(@RequestBody @Valid WalletRequestDto dto) {
        WalletRequestDto createdRequest = walletRequestService.createWithdrawalRequest(dto);
        return ResponseEntity.status(201).body(createdRequest);
    }
    @GetMapping("/{id}")
    public ResponseEntity<WalletRequestDto> getRequestById(@PathVariable Long id) {
        WalletRequestDto walletRequest = walletRequestService.getWalletRequestById(id);
        return ResponseEntity.ok(walletRequest); // Trả về yêu cầu ví theo ID
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<WalletRequestDto> approveRequest(@PathVariable Long id) {
        WalletRequestDto approvedRequest = walletRequestService.approveRequest(id);
        return ResponseEntity.ok(approvedRequest);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<WalletRequestDto> rejectRequest(@PathVariable Long id, @RequestParam String reason) {
        WalletRequestDto rejectedRequest = walletRequestService.rejectRequest(id, reason);
        return ResponseEntity.ok(rejectedRequest);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRequest(@PathVariable Long id) {
        String message = walletRequestService.deleteWalletRequestById(id);
        return ResponseEntity.ok(message);
    }
    @Operation(
            summary = "PENDING, APPROVED, REJECTED"
    )
    @GetMapping("/status")
    public ResponseEntity<List<WalletRequestDto>> getRequestsByStatus(@RequestParam String status) {
        List<WalletRequestDto> requests = walletRequestService.getRequestsByStatus(status);
        return ResponseEntity.ok(requests);
    }
    @GetMapping
    public ResponseEntity<List<WalletRequestDto>> getAllRequests() {
        List<WalletRequestDto> requests = walletRequestService.getAllRequests();
        return ResponseEntity.ok(requests);
    }
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<WalletRequestDto>> getAllRequestsOfUser(@PathVariable Long userId) {
        List<WalletRequestDto> requests = walletRequestService.getAllRequestsOfUser(userId);
        return ResponseEntity.ok(requests);
    }


}
