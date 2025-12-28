package com.bp.payments.api.dto;

import com.bp.payments.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The type Payment response.
 */
public record PaymentResponse(
        Long id,
        Long reservationId,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime createdAt
) {
}