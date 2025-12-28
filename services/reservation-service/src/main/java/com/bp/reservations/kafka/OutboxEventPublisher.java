package com.bp.reservations.kafka;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.reservations.entity.OutboxEvent;
import com.bp.reservations.entity.OutboxEventStatus;
import com.bp.reservations.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ReservationProducer reservationProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRateString = "${app.outbox.scheduler-fixed-rate}")
    @Transactional
    public void publishOutboxEvents() {
        log.debug("Checking for new outbox events to publish...");
        List<OutboxEvent> newEvents = outboxEventRepository.findByStatus(OutboxEventStatus.NEW);

        if (newEvents.isEmpty()) {
            log.debug("No new outbox events found.");
            return;
        }

        log.info("Found {} new outbox events to publish.", newEvents.size());

        for (OutboxEvent event : newEvents) {
            try {
                ReservationCreatedEvent reservationEvent = objectMapper.readValue(event.getPayload(), ReservationCreatedEvent.class);
                reservationProducer.sendReservationCreatedEvent(reservationEvent);

                event.setStatus(OutboxEventStatus.SENT);
                outboxEventRepository.save(event);
                log.info("Published outbox event for reservationId={} (eventId={})", event.getAggregateId(), event.getId());
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize outbox event payload for eventId={}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventStatus.FAILED);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event for eventId={}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventStatus.FAILED); // Mark as failed due to an exception during publishing
                outboxEventRepository.save(event);
            }
        }
    }
}
