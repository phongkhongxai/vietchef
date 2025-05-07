package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIsReadFalseAndNotiTypeNot(Long userId, String notiType);
    List<Notification> findByUserIdAndIsReadFalseAndNotiType(Long userId, String notiType);

    Page<Notification> findByUserIdAndNotiTypeNotOrderByCreatedAtDesc(Long userId, String notiType, Pageable pageable);
    List<Notification> findByUserIdAndNotiType(Long userId, String notiType);



}
