package com.bp.reservations.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateReservationRequest(
        @NotBlank String userId,
        @NotBlank String resourceId,
        @NotBlank String fromIso,
        @NotBlank String toIso
) {}