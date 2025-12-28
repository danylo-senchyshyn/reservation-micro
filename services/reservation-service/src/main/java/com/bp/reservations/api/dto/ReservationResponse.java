package com.bp.reservations.api.dto;

import com.bp.reservations.entity.ReservationStatus;

import java.time.LocalDateTime;

/**
 * The type Reservation response.
 */
public record ReservationResponse(
        Long id,
        Long userId,
        Long resourceId,
        LocalDateTime from,
        LocalDateTime to,
        ReservationStatus status
) {}