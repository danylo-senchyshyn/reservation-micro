package com.bp.reservations.kafka;

import com.bp.common.events.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * The type Reservation producer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationProducer {

    @Value("${app.kafka.topics.reservation-created}")
    private String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send reservation created event.
     *
     * @param event the event
     * @return future resolved when broker acknowledges the message
     */
    public CompletableFuture<SendResult<String, Object>> sendReservationCreatedEvent(ReservationCreatedEvent event) {
        log.debug("KAFKA | Sending ReservationCreatedEvent: reservationId={}", event.reservationId());
        return kafkaTemplate.send(topic, String.valueOf(event.reservationId()), event);
    }
}