package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.entity.Chef;
import com.spring2025.vietchefs.models.entity.Package;
import com.spring2025.vietchefs.models.exception.VchefApiException;
import com.spring2025.vietchefs.models.payload.requestModel.ChefPackageRequestDto;
import com.spring2025.vietchefs.models.payload.requestModel.PackageRequestDto;
import com.spring2025.vietchefs.models.payload.responseModel.PackageResponseDto;
import com.spring2025.vietchefs.repositories.ChefRepository;
import com.spring2025.vietchefs.repositories.PackageRepository;
import com.spring2025.vietchefs.services.PackageService;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PackageServiceImpl implements PackageService {
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ChefRepository chefRepository;
    @Override
    public PackageResponseDto createPackage(PackageRequestDto packageRequest) {
        Package newPackage = modelMapper.map(packageRequest, Package.class);
        Package savedPackage = packageRepository.save(newPackage);
        return modelMapper.map(savedPackage, PackageResponseDto.class);
    }

    @Override
    public PackageResponseDto getPackageById(Long id) {
        Package existingPackage = packageRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Package not found with id: " + id));
        return modelMapper.map(existingPackage, PackageResponseDto.class);
    }

    @Override
    public PackageResponseDto updatePackage(Long id, PackageRequestDto packageRequest) {
        Package existingPackage = packageRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Package not found with id: " + id));

        modelMapper.map(packageRequest, existingPackage);
        Package updatedPackage = packageRepository.save(existingPackage);

        return modelMapper.map(updatedPackage, PackageResponseDto.class);
    }

    @Override
    public void deletePackage(Long id) {
        Package existingPackage = packageRepository.findById(id)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Package not found with id: " + id));
        existingPackage.setIsDeleted(true);
        packageRepository.save(existingPackage);
    }

    @Override
    public List<PackageResponseDto> getAllPackages() {
        List<Package> packages = packageRepository.findByIsDeletedFalse();
        return packages.stream()
                .map(packageEntity -> modelMapper.map(packageEntity, PackageResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public void registerChefToPackages(ChefPackageRequestDto request) {
        Chef chef = chefRepository.findById(request.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + request.getChefId()));

        Set<Package> registeredPackages = chef.getPackages();

        List<Package> packages = packageRepository.findAllById(request.getPackageIds());
        if (packages.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Packages not found.");
        }

        registeredPackages.addAll(packages);
        chef.setPackages(registeredPackages);
        chefRepository.save(chef);
    }

    @Override
    public void unregisterChefFromPackages(ChefPackageRequestDto request) {
        Chef chef = chefRepository.findById(request.getChefId())
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + request.getChefId()));
        Set<Package> updatedPackages = chef.getPackages();
        List<Package> packages = packageRepository.findAllById(request.getPackageIds());
        if (packages.isEmpty()) {
            throw new VchefApiException(HttpStatus.NOT_FOUND, "Packages not found.");
        }
        updatedPackages.removeIf(pkg -> request.getPackageIds().contains(pkg.getId()));

        chef.setPackages(updatedPackages);
        chefRepository.save(chef);
    }

    @Override
    public List<PackageResponseDto> getUnregisteredPackages(Long chefId) {
        Chef chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new VchefApiException(HttpStatus.NOT_FOUND, "Chef not found with id: " + chefId));

        Set<Long> registeredPackageIds = chef.getPackages().stream().map(Package::getId).collect(Collectors.toSet());

        List<Package> unregisteredPackages = packageRepository.findAll().stream()
                .filter(pkg -> !registeredPackageIds.contains(pkg.getId()))
                .toList();

        return unregisteredPackages.stream()
                .map(pkg -> modelMapper.map(pkg, PackageResponseDto.class))
                .collect(Collectors.toList());
    }
}
