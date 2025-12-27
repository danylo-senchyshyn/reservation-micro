package com.bp.notifications.kafka;

import com.bp.common.events.PaymentFailedEvent;
import com.bp.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-failed}",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        notificationService.processPaymentFailure(event);
    }
}