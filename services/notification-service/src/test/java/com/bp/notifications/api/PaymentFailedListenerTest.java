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

@ExtendWith(MockitoExtension.class)
class PaymentFailedListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentFailedListener listener;

    @Test
    void shouldDelegateEventToService() {
        var event = new PaymentFailedEvent(
                1L,
                10L,
                "Not enough funds"
        );

        listener.onPaymentFailed(event);

        verify(notificationService)
                .processPaymentFailure(event);
    }
}