package com.bp.notifications.api;

import com.bp.common.events.PaymentFailedEvent;
import com.bp.notifications.kafka.PaymentFailedListener;
import com.bp.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentFailedListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentFailedListener listener;

    @Test
    void shouldDelegateEventToService() {
        // given
        var event = new PaymentFailedEvent(
                1L,
                10L,
                "Not enough funds"
        );

        // when
        listener.onPaymentFailed(event);

        // then
        verify(notificationService).processPaymentFailure(event);
        verifyNoMoreInteractions(notificationService);
    }
}