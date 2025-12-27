package com.bp.notifications.service;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import com.bp.notifications.entity.NotificationLog;
import com.bp.notifications.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void processPaymentConfirmation(PaymentConfirmedEvent event) {
        String eventType = event.getClass().getSimpleName();

        if (notificationLogRepository.existsByPaymentIdAndEventType(
                event.paymentId(), eventType)) {
            log.warn("Duplicate confirmation event for paymentId={}, skipping", event.paymentId());
            return;
        }

        try {
            notificationLogRepository.save(
                    NotificationLog.builder()
                            .paymentId(event.paymentId())
                            .eventType(eventType)
                            .sentAt(LocalDateTime.now())
                            .build()
            );
            log.info("Payment confirmation notification sent. paymentId={}", event.paymentId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition while saving notification log, skipping");
        }
    }

    @Transactional
    public void processPaymentFailure(PaymentFailedEvent event) {
        String eventType = event.getClass().getSimpleName();

        if (notificationLogRepository.existsByPaymentIdAndEventType(
                event.paymentId(), eventType)) {
            log.warn("Duplicate failure event for paymentId={}, skipping", event.paymentId());
            return;
        }

        try {
            notificationLogRepository.save(
                    NotificationLog.builder()
                            .paymentId(event.paymentId())
                            .eventType(eventType)
                            .sentAt(LocalDateTime.now())
                            .build()
            );
            log.info("Payment failure notification sent. paymentId={}", event.paymentId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition while saving notification log, skipping");
        }
    }
}