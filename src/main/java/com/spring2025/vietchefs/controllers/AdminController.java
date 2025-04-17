package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.ChefDto;
import com.spring2025.vietchefs.models.payload.dto.SignupDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.responseModel.UsersResponse;
import com.spring2025.vietchefs.services.BookingService;
import com.spring2025.vietchefs.services.ChefService;
import com.spring2025.vietchefs.services.PaymentCycleService;
import com.spring2025.vietchefs.services.UserService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @Autowired
    private ChefService chefService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private PaymentCycleService paymentCycleService;

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/chefs")
    public ResponseEntity<?> createChefUserEX(@Valid @RequestBody SignupDto signupDto) {
        UserDto bt = userService.saveChefUser(signupDto);
        ChefDto chefDto = new ChefDto();
        chefDto.setUser(bt);
        chefDto.setBio("Hello world");
        chefDto.setPrice(BigDecimal.valueOf(20));
        chefDto.setDescription("Bonjour");
        chefDto.setAddress("S302 Vinhomes Grand Park Quan 9 Ho Chi Minh");
        chefDto.setStatus("active");
        ChefDto chefDt = chefService.createChef(chefDto);
        return new ResponseEntity<>(chefDt, HttpStatus.CREATED);
    }

//    @SecurityRequirement(name = "Bearer Authentication")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
//    @PostMapping("/bookings/check")
//    public ResponseEntity<?> paymentCyclesCheck() {
//        bookingService.markOverdueAndRefundBookings();
//        return new ResponseEntity<>("Hehe", HttpStatus.OK);
//    }

    @GetMapping
    public UsersResponse getAllUsers(@RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                     @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                     @RequestParam(value = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
                                     @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return userService.getAllUser(pageNo, pageSize, sortBy, sortDir);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        String msg = userService.deleteUser(id);
        return new ResponseEntity<>(msg, HttpStatus.NO_CONTENT);
    }
}
