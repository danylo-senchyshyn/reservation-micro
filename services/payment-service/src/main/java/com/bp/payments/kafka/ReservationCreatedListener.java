package com.bp.payments.kafka;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationCreatedListener {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${app.kafka.topics.reservation-created}",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReservationCreated(ReservationCreatedEvent event) {
        paymentService.processPayment(event);
    }
}