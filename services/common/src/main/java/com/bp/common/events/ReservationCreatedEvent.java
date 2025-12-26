package com.bp.common.events;

public record ReservationCreatedEvent(
        String reservationId,
        String userId,
        String resourceId,
        String fromIso,
        String toIso
) {}