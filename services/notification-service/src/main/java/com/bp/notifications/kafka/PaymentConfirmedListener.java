package com.bp.notifications.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The type Payment confirmed listener.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmedListener {

    private final NotificationService notificationService;

    /**
     * On payment confirmed.
     *
     * @param event the event
     */
    @KafkaListener(
            topics = "${app.kafka.topics.payment-confirmed}",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        notificationService.processPaymentConfirmation(event);
    }
}