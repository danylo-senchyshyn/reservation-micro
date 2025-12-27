package com.bp.reservations.api;

import com.bp.common.events.ReservationCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static jdk.internal.org.objectweb.asm.util.CheckClassAdapter.verify;

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
                5L
        );

        listener.onReservationCreated(event);

        verify(paymentService)
                .processPayment(event);
    }
}