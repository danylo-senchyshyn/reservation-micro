package com.bp.notifications.repository;

import com.bp.notifications.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByPaymentId(Long paymentId);
    boolean existsByPaymentIdAndEventType(Long paymentId, String eventType);
}
