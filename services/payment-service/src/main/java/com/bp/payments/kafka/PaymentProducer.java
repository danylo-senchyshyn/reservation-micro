package com.bp.payments.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
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
    private String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentConfirmedEvent(PaymentConfirmedEvent event) {
        log.info("Sending payment confirmed event: {}", event);
        kafkaTemplate.send(topic, event.paymentId(), event);
    }
}