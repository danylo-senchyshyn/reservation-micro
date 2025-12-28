package com.bp.reservations.api;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import com.bp.common.events.PaymentStatus;
import com.bp.reservations.entity.ReservationStatus;
import com.bp.reservations.kafka.PaymentStatusListener;
import com.bp.reservations.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationPaymentListenerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private PaymentStatusListener listener;

    @Test
    void shouldHandlePaymentConfirmedEvent() {
        var event = new PaymentConfirmedEvent(
                1L,
                10L,
                PaymentStatus.CONFIRMED
        );

        listener.onPaymentConfirmed(event);

        verify(reservationService)
                .updateReservationStatus(10L, PaymentStatus.CONFIRMED);
    }

    @Test
    void shouldHandlePaymentFailedEvent() {
        var event = new PaymentFailedEvent(
                1L,
                10L,
                "Not enough funds"
        );

        listener.onPaymentFailed(event);

        verify(reservationService)
                .updateReservationStatus(10L, PaymentStatus.FAILED);
    }
}