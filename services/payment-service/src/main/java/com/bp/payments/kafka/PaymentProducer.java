package com.bp.payments.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    @Value("${app.kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${app.kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentConfirmedEvent(PaymentConfirmedEvent event) {
        log.info(
                "Sending PaymentConfirmedEvent: paymentId={}, reservationId={}, status={}",
                event.paymentId(),
                event.reservationId(),
                event.status()
        );

        kafkaTemplate.send(paymentConfirmedTopic, event);
    }

    public void sendPaymentFailedEvent(PaymentFailedEvent event) {
        log.warn(
                "Sending PaymentFailedEvent: paymentId={}, reservationId={}, reason={}",
                event.paymentId(),
                event.reservationId(),
                event.reason()
        );

        kafkaTemplate.send(paymentFailedTopic, event);
    }
}