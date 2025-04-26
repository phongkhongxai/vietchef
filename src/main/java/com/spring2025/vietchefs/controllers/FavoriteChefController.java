package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.dto.FavoriteChefDto;
import com.spring2025.vietchefs.models.payload.responseModel.FavoriteChefsResponse;
import com.spring2025.vietchefs.services.FavoriteChefService;
import com.spring2025.vietchefs.utils.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/favorite-chefs")
public class FavoriteChefController {

    private final FavoriteChefService favoriteChefService;

    @Autowired
    public FavoriteChefController(FavoriteChefService favoriteChefService) {
        this.favoriteChefService = favoriteChefService;
    }

    @Operation(summary = "Add a chef to favorites")
    @PostMapping("/{userId}/chefs/{chefId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<FavoriteChefDto> addFavoriteChef(
            @PathVariable Long userId,
            @PathVariable Long chefId) {
        return new ResponseEntity<>(favoriteChefService.addFavoriteChef(userId, chefId), HttpStatus.CREATED);
    }

    @Operation(summary = "Remove a chef from favorites")
    @DeleteMapping("/{userId}/chefs/{chefId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<String> removeFavoriteChef(
            @PathVariable Long userId,
            @PathVariable Long chefId) {
        favoriteChefService.removeFavoriteChef(userId, chefId);
        return new ResponseEntity<>("Chef removed from favorites successfully", HttpStatus.OK);
    }

    @Operation(summary = "Get list of favorite chefs for a user")
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<FavoriteChefsResponse> getFavoriteChefs(
            @PathVariable Long userId,
            @RequestParam(value = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc", required = false) String sortDir) {
        return new ResponseEntity<>(favoriteChefService.getFavoriteChefs(userId, pageNo, pageSize, sortBy, sortDir), HttpStatus.OK);
    }

    @Operation(summary = "Check if a chef is in user's favorites")
    @GetMapping("/{userId}/chefs/{chefId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isChefFavorite(
            @PathVariable Long userId,
            @PathVariable Long chefId) {
        return new ResponseEntity<>(favoriteChefService.isChefFavorite(userId, chefId), HttpStatus.OK);
    }
} 