package com.ecom.notifications.repos;

import com.ecom.notifications.models.Notification;
import com.ecom.notifications.models.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
    boolean existsByEventId(String eventId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByStatusAndAttemptsLessThan(NotificationStatus status, int maxAttempts);
}
