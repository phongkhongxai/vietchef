package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.BookingPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingPriceRepository extends JpaRepository<BookingPrice, Long> {
}
