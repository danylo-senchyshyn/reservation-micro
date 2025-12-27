package com.bp.common.events;

import java.time.LocalDateTime;

public record ReservationCreatedEvent(
        Long reservationId,
        Long userId,
        Long resourceId,
        LocalDateTime from,
        LocalDateTime to
) {}