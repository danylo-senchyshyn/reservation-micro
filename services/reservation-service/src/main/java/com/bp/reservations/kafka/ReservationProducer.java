package com.bp.reservations.kafka;

import com.bp.common.events.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationProducer {

    @Value("${app.kafka.topic}")
    private String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendReservationCreatedEvent(ReservationCreatedEvent event) {
        log.info("Sending reservation created event: {}", event);
        kafkaTemplate.send(topic, event.reservationId(), event);
    }
}
