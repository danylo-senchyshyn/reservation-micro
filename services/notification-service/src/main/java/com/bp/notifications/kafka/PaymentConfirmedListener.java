package com.bp.notifications.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmedListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-confirmed}",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        notificationService.processPaymentConfirmation(event);
    }
}