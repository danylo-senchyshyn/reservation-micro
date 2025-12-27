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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentProducer paymentProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRateString = "${app.outbox.scheduler-fixed-rate}") // Configurable rate, e.g., 5000ms
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
                // Deserialize the payload to the correct event type
                Object kafkaEvent;
                if (event.getEventType().equals("PaymentConfirmedEvent")) {
                    kafkaEvent = objectMapper.readValue(event.getPayload(), PaymentConfirmedEvent.class);
                    paymentProducer.sendPaymentConfirmedEvent((PaymentConfirmedEvent) kafkaEvent);
                } else if (event.getEventType().equals("PaymentFailedEvent")) {
                    kafkaEvent = objectMapper.readValue(event.getPayload(), PaymentFailedEvent.class);
                    paymentProducer.sendPaymentFailedEvent((PaymentFailedEvent) kafkaEvent);
                } else {
                    log.error("Unknown event type {} for outbox event id {}", event.getEventType(), event.getId());
                    event.setStatus(OutboxEventStatus.FAILED); // Mark as failed due to unknown type
                    outboxEventRepository.save(event);
                    continue;
                }

                event.setStatus(OutboxEventStatus.SENT);
                outboxEventRepository.save(event);
                log.info("Published outbox event for paymentId={} (eventId={})", event.getAggregateId(), event.getId());
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize outbox event payload for eventId={}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventStatus.FAILED); // Mark as failed due to serialization error
                outboxEventRepository.save(event);
            } catch (Exception e) { // Catch any other exceptions during Kafka sending
                log.error("Failed to publish outbox event for eventId={}: {}", event.getId(), e.getMessage());
                // Depending on policy, might retry or mark as FAILED
                // For now, simply log and let it potentially be retried by the next schedule if not marked FAILED.
            }
        }
    }
}
