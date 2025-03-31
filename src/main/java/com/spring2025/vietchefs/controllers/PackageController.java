package com.spring2025.vietchefs.controllers;

import com.spring2025.vietchefs.models.payload.requestModel.ChefPackageRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.PackageRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.PackageResponseDto;
import com.spring2025.vietchefs.services.PackageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/packages")
public class PackageController {
    @Autowired
    private PackageService packageService;

    @GetMapping
    public ResponseEntity<List<PackageResponseDto>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageResponseDto> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.getPackageById(id));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<PackageResponseDto> createPackage(@RequestBody PackageRequestDto packageRequest) {
        return ResponseEntity.ok(packageService.createPackage(packageRequest));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PackageResponseDto> updatePackage(@PathVariable Long id, @RequestBody PackageRequestDto packageRequest) {
        return ResponseEntity.ok(packageService.updatePackage(id, packageRequest));
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ResponseEntity.ok("Package deleted successfully!");
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PostMapping("/subscribe")
    public ResponseEntity<String> registerChefToPackages(@RequestBody ChefPackageRequestDto request) {
        packageService.registerChefToPackages(request);
        return ResponseEntity.ok("Chef registered to packages successfully!");
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unregisterChefFromPackages(@RequestBody ChefPackageRequestDto request) {
        packageService.unregisterChefFromPackages(request);
        return ResponseEntity.ok("Chef unregistered from packages successfully!");
    }
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ROLE_CHEF')")
    @GetMapping("/unregistered/{chefId}")
    public ResponseEntity<List<PackageResponseDto>> getUnregisteredPackages(@PathVariable Long chefId) {
        return ResponseEntity.ok(packageService.getUnregisteredPackages(chefId));
    }
}
