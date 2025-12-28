package com.bp.reservations.entity;

/**
 * The enum Reservation status.
 */
public enum ReservationStatus {
    /**
     * Created reservation status.
     */
    CREATED,
    /**
     * Paid reservation status.
     */
    PAID,
    /**
     * Payment failed reservation status.
     */
    PAYMENT_FAILED,
    /**
     * Cancelled reservation status.
     */
    CANCELLED
}
