package com.bp.payments.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import com.bp.payments.entity.OutboxEvent;
import com.bp.payments.entity.OutboxEventStatus;
import com.bp.payments.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final PaymentProducer paymentProducer;
    private final ObjectMapper objectMapper;

    // Cache Class.forName results to avoid repeated class-loader lookups under load
    private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

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
                Class<?> eventClass = classCache.computeIfAbsent(
                        event.getEventType(), name -> {
                            try {
                                return Class.forName(name);
                            } catch (ClassNotFoundException e) {
                                throw new IllegalArgumentException("Unknown event class: " + name, e);
                            }
                        });

                Object kafkaEvent = objectMapper.readValue(event.getPayload(), eventClass);

                // Await broker acknowledgment before marking SENT.
                // Prevents silent message loss when Kafka is slow or temporarily unavailable.
                if (kafkaEvent instanceof PaymentConfirmedEvent confirmed) {
                    paymentProducer.sendPaymentConfirmedEvent(confirmed)
                            .get(5, TimeUnit.SECONDS);
                } else if (kafkaEvent instanceof PaymentFailedEvent failed) {
                    paymentProducer.sendPaymentFailedEvent(failed)
                            .get(5, TimeUnit.SECONDS);
                } else {
                    log.error("Outbox: unsupported event type={} eventId={}", event.getEventType(), event.getId());
                    event.setStatus(OutboxEventStatus.FAILED);
                    toSave.add(event);
                    continue;
                }

                event.setStatus(OutboxEventStatus.SENT);
                log.debug("Outbox: sent eventId={} paymentId={}", event.getId(), event.getAggregateId());

            } catch (JsonProcessingException | IllegalArgumentException e) {
                log.error("Outbox: deserialize/class failed eventId={}: {}", event.getId(), e.getMessage());
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
