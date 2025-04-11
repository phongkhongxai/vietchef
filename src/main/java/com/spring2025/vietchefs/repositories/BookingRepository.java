package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Booking;
import com.spring2025.vietchefs.models.entity.BookingDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@EnableJpaRepositories
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByCustomerIdAndIsDeletedFalse(Long customerId, Pageable pageable);
    Page<Booking> findByChefIdAndIsDeletedFalse(Long chefId, Pageable pageable);
    Page<Booking> findByChefIdAndStatusNotInAndIsDeletedFalse(Long chefId, List<String> statusList, Pageable pageable);


    List<Booking> findByStatusInAndCreatedAtBefore(List<String> statuses, LocalDateTime before);


}
