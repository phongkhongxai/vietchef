package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

@Repository
@EnableJpaRepositories
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByCustomerIdAndIsDeletedFalse(Long customerId, Pageable pageable);
    Page<Booking> findByChefIdAndIsDeletedFalse(Long chefId, Pageable pageable);

}
