package com.bp.common.events;

/**
 * The type Payment failed event.
 */
public record PaymentFailedEvent(
        Long paymentId,
        Long reservationId,
        String reason
) {}