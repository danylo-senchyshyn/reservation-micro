package com.bp.notifications.repository;

import com.bp.notifications.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The interface Notification log repository.
 */
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    /**
     * Exists by payment id boolean.
     *
     * @param paymentId the payment id
     * @return the boolean
     */
    boolean existsByPaymentId(Long paymentId);

    /**
     * Exists by payment id and event type boolean.
     *
     * @param paymentId the payment id
     * @param eventType the event type
     * @return the boolean
     */
    boolean existsByPaymentIdAndEventType(Long paymentId, String eventType);
}
