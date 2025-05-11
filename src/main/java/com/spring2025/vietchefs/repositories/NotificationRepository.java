package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false AND (n.notiType != 'CHAT_NOTIFY' OR n.notiType IS NULL)")
    List<Notification> findUnreadNonChatNotifications(@Param("userId") Long userId);

    List<Notification> findByUserIdAndIsReadFalseAndNotiType(Long userId, String notiType);

    Page<Notification> findByUserIdAndNotiTypeNotOrderByCreatedAtDesc(Long userId, String notiType, Pageable pageable);
    List<Notification> findByUserIdAndNotiType(Long userId, String notiType);
    int countByUserIdAndIsReadFalseAndNotiType(Long userId, String notiType);
    int countByUserIdAndIsReadFalse(Long userId);




}
