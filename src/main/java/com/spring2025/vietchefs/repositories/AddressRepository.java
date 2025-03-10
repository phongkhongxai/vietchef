package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Address;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserAndIsDeletedFalse(User user);
}
