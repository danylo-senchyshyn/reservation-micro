package com.bp.notifications.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmedListener {

    @KafkaListener(topics = "${app.kafka.topics.payment-confirmed}", groupId = "notification-service-group")
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info(
                "Payment confirmed: reservationId={}, paymentId={}, status={}",
                event.reservationId(),
                event.paymentId(),
                event.status()
        );
    }
}