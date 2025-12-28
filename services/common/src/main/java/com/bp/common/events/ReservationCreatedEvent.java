package com.bp.common.events;

import java.time.LocalDateTime;

/**
 * The type Reservation created event.
 */
public record ReservationCreatedEvent(
        Long reservationId,
        Long userId,
        Long resourceId,
        LocalDateTime from,
        LocalDateTime to
) {}