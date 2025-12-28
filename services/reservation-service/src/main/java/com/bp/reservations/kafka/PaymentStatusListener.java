package com.bp.reservations.kafka;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import com.bp.reservations.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The type Payment status listener.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusListener {

    private final ReservationService reservationService;

    /**
     * On payment confirmed.
     *
     * @param event the event
     */
    @KafkaListener(
            topics = "${app.kafka.topics.payment-confirmed}",
            groupId = "reservation-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info(
                "PaymentConfirmedEvent received for reservationId: {}, paymentId: {}",
                event.reservationId(),
                event.paymentId()
        );
        reservationService.updateReservationStatus(event.reservationId(), event.status());
    }

    /**
     * On payment failed.
     *
     * @param event the event
     */
    @KafkaListener(
            topics = "${app.kafka.topics.payment-failed}",
            groupId = "reservation-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info(
                "PaymentFailedEvent received for reservationId: {}, paymentId: {}, reason: {}",
                event.reservationId(),
                event.paymentId(),
                event.reason()
        );
        // Assuming common.events.PaymentStatus will be PAYMENT_FAILED
        reservationService.updateReservationStatus(event.reservationId(), com.bp.common.events.PaymentStatus.FAILED);
    }
}