package com.bp.common.events;

public record PaymentConfirmedEvent(
        Long paymentId,
        Long reservationId,
        PaymentStatus status
) {}