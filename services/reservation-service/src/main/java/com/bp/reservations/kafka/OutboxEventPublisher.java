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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type Outbox event publisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ReservationProducer reservationProducer;
    private final ObjectMapper objectMapper;

    // fixedDelay: next run starts only AFTER the previous one completes,
    // preventing overlapping executions under load.
    @Scheduled(fixedDelayString = "${app.outbox.scheduler-fixed-rate}")
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> newEvents = outboxEventRepository.findByStatus(OutboxEventStatus.NEW);

        if (newEvents.isEmpty()) {
            return;
        }

        log.info("Outbox: publishing {} new events", newEvents.size());

        List<OutboxEvent> toSave = new ArrayList<>(newEvents.size());

        for (OutboxEvent event : newEvents) {
            try {
                ReservationCreatedEvent reservationEvent =
                        objectMapper.readValue(event.getPayload(), ReservationCreatedEvent.class);

                // Await broker acknowledgment before marking SENT.
                // Prevents silent message loss when Kafka is slow or temporarily unavailable.
                reservationProducer.sendReservationCreatedEvent(reservationEvent)
                        .get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxEventStatus.SENT);
                log.debug("Outbox: sent eventId={} reservationId={}", event.getId(), event.getAggregateId());

            } catch (JsonProcessingException e) {
                log.error("Outbox: deserialize failed eventId={}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventStatus.FAILED);
            } catch (ExecutionException | TimeoutException e) {
                log.error("Outbox: Kafka send failed eventId={}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventStatus.FAILED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Outbox: interrupted while sending eventId={}", event.getId());
                event.setStatus(OutboxEventStatus.FAILED);
            }

            toSave.add(event);
        }

        // Batch update — one round-trip instead of N individual UPDATEs
        outboxEventRepository.saveAll(toSave);
    }
}
