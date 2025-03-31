package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageRepository extends JpaRepository<Package, Long> {
    List<Package> findByIsDeletedFalse();
}
