package com.spring2025.vietchefs.services;

import com.spring2025.vietchefs.models.payload.requestModel.ChefPackageRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.PackageRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.PackageResponseDto;

import java.util.List;

public interface PackageService {
    PackageResponseDto createPackage(PackageRequestDto packageRequest);
    PackageResponseDto getPackageById(Long id);
    PackageResponseDto updatePackage(Long id, PackageRequestDto packageRequest);
    void deletePackage(Long id);
    List<PackageResponseDto> getAllPackages();
    void registerChefToPackages(ChefPackageRequestDto request);
    void unregisterChefFromPackages(ChefPackageRequestDto request);
    List<PackageResponseDto> getUnregisteredPackages(Long chefId);

}
