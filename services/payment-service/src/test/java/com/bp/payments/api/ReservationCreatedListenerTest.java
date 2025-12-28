package com.bp.payments.api;

import com.bp.common.events.ReservationCreatedEvent;
import com.bp.payments.kafka.ReservationCreatedListener;
import com.bp.payments.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ReservationCreatedListenerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private ReservationCreatedListener listener;

    @Test
    void shouldProcessReservationCreatedEvent() {
        var event = new ReservationCreatedEvent(
                10L,
                1L,
                5L,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2)
        );

        listener.onReservationCreated(event);

        verify(paymentService).processPayment(event);
        verifyNoMoreInteractions(paymentService);
    }
}