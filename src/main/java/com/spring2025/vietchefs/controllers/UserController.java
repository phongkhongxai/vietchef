package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.BookingResponseDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.dto.WalletDto;
import com.spring2025.vietchefs.models.payload.requestModel.ChangePasswordRequest;
import com.spring2025.vietchefs.models.payload.requestModel.UserRequest;
import com.spring2025.vietchefs.models.payload.responseModel.UserResponse;
import com.spring2025.vietchefs.models.payload.responseModel.WalletPlusResponse;
import com.spring2025.vietchefs.services.UserService;
import com.spring2025.vietchefs.services.WalletService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private WalletService walletService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @GetMapping("/profile")
    public ResponseEntity<UserDto> viewProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return new ResponseEntity<>(bto, HttpStatus.OK);
    }
    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getProfileByUsername(@PathVariable("username") String username) {
        UserResponse bto = userService.getProfileUserByUsername(username);
        return new ResponseEntity<>(bto, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @GetMapping("/profile/my-wallet/all")
    public ResponseEntity<WalletPlusResponse> viewWalletProfileAll(@AuthenticationPrincipal UserDetails userDetails,
                                                                @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                                                @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                                                @RequestParam(value = "sortBy", defaultValue = "createdAt", required = false) String sortBy,
                                                                @RequestParam(value = "sortDir", defaultValue = "desc", required = false) String sortDir) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        WalletPlusResponse bs = walletService.getWalletByUserIdAll(bto.getId(), pageNo,  pageSize,  sortBy,  sortDir);
        return new ResponseEntity<>(bs, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @GetMapping("/profile/my-wallet")
    public ResponseEntity<WalletDto> viewWalletProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        WalletDto bs = walletService.getWalletByUserId(bto.getId());
        return new ResponseEntity<>(bs, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @PutMapping("/profile/my-wallet")
    public ResponseEntity<WalletDto> updateEmailWallet(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String email) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        WalletDto bs = walletService.updateEmailPaypalForWallet(bto.getId(), email);
        return new ResponseEntity<>(bs, HttpStatus.OK);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @PostMapping("/profile/my-wallet/set-password")
    public ResponseEntity<?> setWalletPassword(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String password) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        WalletDto wallet = walletService.setPasswordForWallet(bto.getId(), password);
        return ResponseEntity.ok(wallet);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @GetMapping("/profile/my-wallet/has-password")
    public ResponseEntity<?> checkWalletHasPassword(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        boolean hasPassword = walletService.checkWalletHasPassword(bto.getId());
        return ResponseEntity.ok(hasPassword);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @PostMapping("/profile/my-wallet/access")
    public ResponseEntity<?> accessWallet(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String password) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        boolean success = walletService.accessWallet(bto.getId(), password);
        return ResponseEntity.ok(success);
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @PostMapping("/profile/my-wallet/forgot-wallet-password")
    public ResponseEntity<String> forgotWalletPassword(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return ResponseEntity.ok(walletService.forgotWalletPassword(bto.getUsername()));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateMyProfile(@AuthenticationPrincipal UserDetails userDetails, @ModelAttribute @Valid UserRequest userDTO) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return ResponseEntity.ok(userService.updateProfile(bto.getId(), userDTO));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_CHEF', 'ROLE_ADMIN')")
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody ChangePasswordRequest request) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        userService.changePassword(bto.getId(), request);
        return ResponseEntity.ok("Password changed successfully");
    }
}
