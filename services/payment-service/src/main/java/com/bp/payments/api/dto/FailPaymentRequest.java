package com.bp.payments.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The type Fail payment request.
 */
public record FailPaymentRequest(
        @NotBlank String reason
) {
}