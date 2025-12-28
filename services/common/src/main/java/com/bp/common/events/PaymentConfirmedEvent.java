package com.bp.common.events;

/**
 * The type Payment confirmed event.
 */
public record PaymentConfirmedEvent(
        Long paymentId,
        Long reservationId,
        PaymentStatus status
) {}