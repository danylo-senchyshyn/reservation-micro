package com.bp.payments.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * The type Payment producer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    @Value("${app.kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${app.kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send payment confirmed event.
     *
     * @param event the event
     * @return future resolved when broker acknowledges the message
     */
    public CompletableFuture<SendResult<String, Object>> sendPaymentConfirmedEvent(PaymentConfirmedEvent event) {
        log.debug(
                "KAFKA | Sending PaymentConfirmedEvent: paymentId={}, reservationId={}",
                event.paymentId(),
                event.reservationId()
        );
        return kafkaTemplate.send(paymentConfirmedTopic, String.valueOf(event.reservationId()), event);
    }

    /**
     * Send payment failed event.
     *
     * @param event the event
     * @return future resolved when broker acknowledges the message
     */
    public CompletableFuture<SendResult<String, Object>> sendPaymentFailedEvent(PaymentFailedEvent event) {
        log.debug(
                "KAFKA | Sending PaymentFailedEvent: paymentId={}, reservationId={}",
                event.paymentId(),
                event.reservationId()
        );
        return kafkaTemplate.send(paymentFailedTopic, String.valueOf(event.reservationId()), event);
    }
}