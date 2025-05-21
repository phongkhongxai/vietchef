package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.NotificationDto;
import com.spring2025.vietchefs.models.payload.dto.UserDto;
import com.spring2025.vietchefs.models.payload.requestModel.NotificationRequest;
import com.spring2025.vietchefs.models.payload.responseModel.NotificationCountResponse;
import com.spring2025.vietchefs.models.payload.responseModel.NotificationsResponse;
import com.spring2025.vietchefs.services.UserService;
import com.spring2025.vietchefs.services.impl.NotificationService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request) {
        notificationService.sendPushNotification(request);
        return ResponseEntity.ok("Notification sent!");
    }
    @GetMapping("/my")
    public NotificationsResponse getNotiOfUser(@AuthenticationPrincipal UserDetails userDetails,
                                               @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                               @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                               @RequestParam(value = "sortBy", defaultValue = "createdAt", required = false) String sortBy,
                                               @RequestParam(value = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return notificationService.getALlNotificationsOfUser(bto.getId(), pageNo,  pageSize,  sortBy,  sortDir);
    }
    @GetMapping("/my/count")
    public ResponseEntity<NotificationCountResponse> countNotiOfUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        return ResponseEntity.ok(notificationService.countUnreadNotifications(bto.getId()));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_CHEF')")
    @PutMapping("/my/all")
    public ResponseEntity<String> updateNotiReadAll(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        notificationService.markAllAsReadByUser(bto.getId());
        return ResponseEntity.ok("OKE");
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_CHEF')")
    @PutMapping("/my")
    public ResponseEntity<String> updateNotiReadIds(@RequestParam List<Long> ids) {
        notificationService.markAsReadByIds(ids);
        return ResponseEntity.ok("OKE");
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_CHEF')")
    @PutMapping("/my-chat")
    public ResponseEntity<String> updateNotiReadIds(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto bto = userService.getProfileUserByUsernameOrEmail(userDetails.getUsername(),userDetails.getUsername());
        notificationService.markAllChatAsReadByUser(bto.getId());
        return ResponseEntity.ok("OKE");
    }
}


