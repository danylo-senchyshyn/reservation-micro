package com.bp.reservations.api.dto;

public record ReservationResponse(
        String id,
        String userId,
        String resourceId,
        String fromIso,
        String toIso,
        String status
) {}