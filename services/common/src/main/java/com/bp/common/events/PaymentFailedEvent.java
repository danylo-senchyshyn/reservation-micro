package com.bp.common.events;

public record PaymentFailedEvent(
        Long paymentId,
        Long reservationId,
        String reason
) {}