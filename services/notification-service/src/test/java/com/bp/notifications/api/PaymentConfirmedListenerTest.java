package com.bp.notifications.api;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentStatus;
import com.bp.notifications.kafka.PaymentConfirmedListener;
import com.bp.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmedListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentConfirmedListener listener;

    @Test
    void shouldDelegateEventToService() {
        // given
        var event = new PaymentConfirmedEvent(
                1L,
                10L,
                PaymentStatus.CONFIRMED
        );

        // when
        listener.onPaymentConfirmed(event);

        // then
        verify(notificationService).processPaymentConfirmation(event);
        verifyNoMoreInteractions(notificationService);
    }
}