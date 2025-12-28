package com.bp.notifications.api;

import com.bp.common.events.PaymentConfirmedEvent;
import com.bp.common.events.PaymentFailedEvent;
import com.bp.common.events.PaymentStatus;
import com.bp.notifications.entity.NotificationLog;
import com.bp.notifications.repository.NotificationLogRepository;
import com.bp.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository repository;

    @InjectMocks
    private NotificationService service;

    // ---------- CONFIRMED ----------

    @Test
    void shouldSendNotificationOnce_forPaymentConfirmed() {
        when(repository.existsByPaymentIdAndEventType(
                1L,
                "PaymentConfirmedEvent"
        )).thenReturn(false);

        service.processPaymentConfirmation(
                new PaymentConfirmedEvent(
                        1L,
                        10L,
                        PaymentStatus.CONFIRMED
                )
        );

        verify(repository).save(any(NotificationLog.class));
    }

    @Test
    void shouldBeIdempotent_forPaymentConfirmed() {
        when(repository.existsByPaymentIdAndEventType(
                1L,
                "PaymentConfirmedEvent"
        )).thenReturn(true);

        service.processPaymentConfirmation(
                new PaymentConfirmedEvent(
                        1L,
                        10L,
                        PaymentStatus.CONFIRMED
                )
        );

        verify(repository, never()).save(any());
    }

    // ---------- FAILED ----------

    @Test
    void shouldSendNotificationOnce_forPaymentFailed() {
        when(repository.existsByPaymentIdAndEventType(
                1L,
                "PaymentFailedEvent"
        )).thenReturn(false);

        service.processPaymentFailure(
                new PaymentFailedEvent(
                        1L,
                        10L,
                        "Not enough funds"
                )
        );

        verify(repository).save(any(NotificationLog.class));
    }

    @Test
    void shouldBeIdempotent_forPaymentFailed() {
        when(repository.existsByPaymentIdAndEventType(
                1L,
                "PaymentFailedEvent"
        )).thenReturn(true);

        service.processPaymentFailure(
                new PaymentFailedEvent(
                        1L,
                        10L,
                        "Not enough funds"
                )
        );

        verify(repository, never()).save(any());
    }
}