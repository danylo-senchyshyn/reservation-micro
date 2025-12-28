package com.bp.payments.entity;

/**
 * The enum Outbox event status.
 */
public enum OutboxEventStatus {
    /**
     * New outbox event status.
     */
    NEW,
    /**
     * Sent outbox event status.
     */
    SENT,
    /**
     * Failed outbox event status.
     */
    FAILED
}
