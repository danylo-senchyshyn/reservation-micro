package com.bp.payments.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * The type Create payment request.
 */
public record CreatePaymentRequest(
        @NotNull Long reservationId,
        @NotNull @Positive BigDecimal amount
) {
}