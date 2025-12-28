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
                Class<?> eventClass = Class.forName(event.getEventType());
                Object kafkaEvent = objectMapper.readValue(event.getPayload(), eventClass);

                if (kafkaEvent instanceof PaymentConfirmedEvent) {
                    paymentProducer.sendPaymentConfirmedEvent((PaymentConfirmedEvent) kafkaEvent);
                } else if (kafkaEvent instanceof PaymentFailedEvent) {
                    paymentProducer.sendPaymentFailedEvent((PaymentFailedEvent) kafkaEvent);
                } else {
                    log.error("Unsupported event type {} for outbox event id {}", event.getEventType(), event.getId());
                    event.setStatus(OutboxEventStatus.FAILED); // Mark as failed due to unsupported type
                    outboxEventRepository.save(event);
                    continue;
                }

                event.setStatus(OutboxEventStatus.SENT);
                outboxEventRepository.save(event);
                log.info("Published outbox event for paymentId={} (eventId={})", event.getAggregateId(), event.getId());
            } catch (JsonProcessingException | ClassNotFoundException e) {
                log.error("Failed to deserialize or find class for outbox event payload for eventId={}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventStatus.FAILED); // Mark as failed due to serialization/class error
                outboxEventRepository.save(event);
            } catch (Exception e) { // Catch any other exceptions during Kafka sending
                log.error("Failed to publish outbox event for eventId={}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxEventStatus.FAILED); // Mark as failed due to an exception during publishing
                outboxEventRepository.save(event);
            }
        }
    }
}
