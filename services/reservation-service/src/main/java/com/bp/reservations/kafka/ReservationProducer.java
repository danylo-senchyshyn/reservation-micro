package com.bp.reservations.kafka;

import com.bp.common.events.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
     */
    public void sendReservationCreatedEvent(ReservationCreatedEvent event) {
        log.info("Sending reservation created event: {}", event);
        kafkaTemplate.send(topic, String.valueOf(event.reservationId()), event);
    }
}