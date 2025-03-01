package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.BookingDetailItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

@Repository
@EnableJpaRepositories
public interface BookingDetailItemRepository extends JpaRepository<BookingDetailItem, Long> {
}
