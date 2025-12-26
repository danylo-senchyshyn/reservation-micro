package com.bp.common.events;

public record PaymentConfirmedEvent(
        String paymentId,
        String reservationId,
        String status
) {}