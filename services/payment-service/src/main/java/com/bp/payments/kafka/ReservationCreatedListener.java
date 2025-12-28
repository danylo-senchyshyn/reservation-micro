package com.bp.payments.kafka;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The type Reservation created listener.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCreatedListener {

    private final PaymentService paymentService;

    /**
     * On reservation created.
     *
     * @param event the event
     */
    @KafkaListener(
            topics = "${app.kafka.topics.reservation-created}",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReservationCreated(ReservationCreatedEvent event) {
        log.info("🔥 RECEIVED ReservationCreatedEvent: {}", event);
        paymentService.processPayment(event);
    }
}